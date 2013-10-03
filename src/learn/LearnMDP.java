/***********************************************************************************************
 * Copyright (C) 2011, 2012  D. Henriques; J. G. Martins; P. Zuliani; A. Platzer; E. M. Clarke.  All rights reserved.
 * By using this software the USER indicates that he or she has read, understood and will comply
 * with the following:
 *
 * 1. The USER is hereby granted non-exclusive permission to use, copy and/or
 * modify this software for internal, non-commercial, research purposes only. Any
 * distribution, including commercial sale or license, of this software, copies of
 * the software, its associated documentation and/or modifications of either is
 * strictly prohibited without the prior consent of the authors. Title to copyright
 * to this software and its associated documentation shall at all times remain with
 * the authors. Appropriated copyright notice shall be placed on all software
 * copies, and a complete copy of this notice shall be included in all copies of
 * the associated documentation. No right is granted to use in advertising,
 * publicity or otherwise any trademark, service mark, or the name of the authors.
 *
 * 2. This software and any associated documentation is provided "as is".
 *
 * THE AUTHORS MAKE NO REPRESENTATIONS OR WARRANTIES, EXPRESSED OR IMPLIED,
 * INCLUDING THOSE OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE, OR THAT
 * USE OF THE SOFTWARE, MODIFICATIONS, OR ASSOCIATED DOCUMENTATION WILL NOT
 * INFRINGE ANY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER INTELLECTUAL PROPERTY
 * RIGHTS OF A THIRD PARTY.
 *
 * The authors shall not be liable under any circumstances for any direct,
 * indirect, special, incidental, or consequential damages with respect to any
 * claim by USER or any third party on account of or arising from the use, or
 * inability to use, this software or its associated documentation, even if the
 * authors have been advised of the possibility of those damages.
 * ***********************************************************************************************/

package learn;
//CHANGES: state hashCode
//CHANGES: private -> protected/public

import smcmdp.*;
import smcmdp.reward.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.*;

import modelchecking.*;
import modelchecking.Result;

import smcmdp.policy.*;
import parser.ast.*;
import prism.*;
import umontreal.iro.lecuyer.probdist.*;

public class LearnMDP {
  
  protected Prism prism; // One Prism to rule them all.
  
  protected ModulesFile modulesFile;
  protected Formula formula;
  
  // Thread information
  protected int numJobs;
  protected int numWorkersWorking;
  protected List<TraceGeneratorThread> workers;
  
  //Counts for each block
  protected AtomicInteger sat;
  protected AtomicInteger fal;
  // Results for model-checking
  protected BlockingQueue<SatResult> results;
  
  protected boolean deterministic;
  protected Policy policy;
  protected DeterministicPolicy deterministicPolicy;
  
  protected Rewards rewards;
  
  // Flag used to kill threads
  protected boolean done;
  
  protected ReentrantLock jobLock;
  protected Condition jobsAvailable;
  protected Condition jobsDone;
  
  protected Random seedGenerator;
  
  public LearnMDP(Prism prism, ModulesFile modulesFile, Formula formula) throws Exception {
    this.prism = prism; this.modulesFile = modulesFile; this.formula = formula;
    
    this.workers = new LinkedList<TraceGeneratorThread>();
    
    this.jobLock = new ReentrantLock();
    this.jobsAvailable = this.jobLock.newCondition();
    this.jobsDone      = this.jobLock.newCondition();
    
    this.deterministic = false;
    this.done = false;
    
    this.seedGenerator = new Random(new Random().nextLong()); // huzzah
    
    sat = new AtomicInteger(0);
    fal = new AtomicInteger(0);
  }
  
  public void resetCounts() { sat.set(0); fal.set(0); }
  
  public synchronized int nextRNGSeed() { return seedGenerator.nextInt(); }
    
  public void startThreads(int numThreads, boolean reward) {
    numWorkersWorking = 0;
    for(int i = 0; i < numThreads; i++){
      TraceGeneratorThread t = new TraceGeneratorThread(this);
      workers.add(t);
      new Thread(t).start();
    }
  }
  
  public void setThreadMode(boolean reward) {
    if(reward) results = null;
    else       results = new LinkedBlockingDeque<SatResult>();
      
    for(TraceGeneratorThread t: workers)
      t.setResultQueue(results);
  }
  
  public void stopThreads(){
    jobLock.lock();
    done = true;
    jobsAvailable.signalAll();
    workers.clear();
    jobLock.unlock();
  }
  
  public long learn(int numTraces, int numBlocks) throws Exception {
    this.policy = new Policy();
    this.rewards = new Rewards();
    
    long updateTime = 0;
    
    // Bayes factor for learning
    BetaDist betaDist = new BetaDist(Conf.ALPHA, Conf.BETA);
    double LEARN_THETA = Math.min(Conf.THETA + Conf.IOTA, 1);
    double PI1 = betaDist.cdf(LEARN_THETA);
    double prior = PI1 / (1-PI1);
    
    if(Conf.DEBUG_FLAG) System.out.println(" Block[#]{# satisfying traces, # falsifying traces}");
    for(int i = 0; i < numBlocks; i++){               // Run nBlocks blocks...
      jobLock.lock();
      int tracesRun = 0;
      while(tracesRun < numTraces){ // Synchronise periodically to avoid bias
        int miniBlock = Math.min(numTraces - tracesRun, Conf.NUM_THREADS*Conf.NUM_JOBS_PER_REQUEST);
        tracesRun = tracesRun + miniBlock;
        //if(Conf.DEBUG_FLAG) System.out.print("("+miniBlock+", "+tracesRun+") ");
        addJobs(miniBlock);
        do {
          jobsDone.await();
        } while(numWorkersWorking > 0);
        
        // If we are not using Bayes factor heuristic for learning
        // skip that part of the loop entirely
        if(!Conf.BAYES_LEARN_FLAG)
          continue;
        betaDist = new BetaDist(sat.get() + Conf.ALPHA, fal.get() + Conf.BETA);
        double bayesFactor = prior * (1.0 / betaDist.cdf(LEARN_THETA) - 1);
        
        // Only check for positive evidence of refuting P<=\theta (\phi)
        // It is normal for initial policies to succeed very rarely, and being fake evidence for the above assertion
        if(bayesFactor > Conf.LEARN_T) {
          if(Conf.DEBUG_FLAG) System.out.print(" Block["+i+"]{"+ sat +", " + fal +"} ");
          if(Conf.DEBUG_FLAG) System.out.println("Stopped learning due to Bayes factor learning heuristic...");
          jobLock.unlock();
          return updateTime;
        }
      }
      
      if(Conf.DEBUG_FLAG) System.out.print(" Block["+i+"]{"+ sat +", " + fal +"} ");
      // Reset satisfaction/falsification counts
      resetCounts();
      assert(numJobs == 0);
      long startUpdate = System.currentTimeMillis();
      if(Conf.DEBUG_FLAG) System.out.print("1");
      
      // Update policy with rewards
      policy.update(rewards);
      
      if(Conf.DEBUG_FLAG) System.out.print("2");
      updateTime += System.currentTimeMillis() - startUpdate;
      // Reset rewards
      this.rewards.reset();
      
      if(Conf.DEBUG_FLAG) System.out.println("3");
      
      jobLock.unlock();
      if(Conf.PROGRESS_FLAG && !Conf.DEBUG_FLAG)                System.out.print("+");
      if(Conf.PROGRESS_FLAG && !Conf.DEBUG_FLAG && i % 40 == 0) System.out.println();
    }
    
    if(Conf.PROGRESS_FLAG || Conf.DEBUG_FLAG) System.out.println();
    return updateTime;
  }
  
  public EstimationResult IntervalEstimation(double alpha, double beta,
      double delta, double coefficient) throws Exception {
    EstimationResult r = new EstimationResult();
    
    int nTraces = 0;
    int nSatisfied = 0;
    double postProb = 0;
    do {
      jobLock.lock();
      if(numWorkersWorking == 0 && results.size() == 0){
        assert(numJobs == 0);
        addJobs(Conf.MODELCHECK_BLOCK_SIZE);
      }
      jobLock.unlock();
      
      SatResult sat = results.take(); // Check whether it is satisfied
      nTraces++;
      
      if(sat.getSat())
        nSatisfied++;
      
      r.p = (nSatisfied + alpha) / (nTraces + alpha + beta);
      r.t0 = r.p - delta;
      r.t1 = r.p + delta;
      
      if (r.t1 > 1) {
        r.t0 = 1 - 2 * delta;
        r.t1 = 1;
      } else if (r.t0 < 0) {
        r.t0 = 0;
        r.t1 = 2 * delta;
      }
      
      BetaDist bd = new BetaDist(nSatisfied + alpha, nTraces - nSatisfied + beta);
      
      postProb = bd.cdf(r.t1) - bd.cdf(r.t0);
      
    } while (postProb < coefficient);
    jobLock.lock();
    numJobs = 0;
    jobLock.unlock();
    
    r.n = nTraces;
    r.nSat = nSatisfied;
    
    return r;
  }
  
  public TestingResult HypothesisTesting(double theta, double T,
      double alpha, double beta) throws Exception {
    TestingResult r = new TestingResult();
    
    int nTraces = 0;
    int nSatisfied = 0;
    
    BetaDist betaDist = new BetaDist(alpha, beta);
    double prior1 = betaDist.cdf(theta);
    double prior = prior1 / (1-prior1);
    
    while(true) {
      jobLock.lock();
      if(numWorkersWorking == 0 && results.size() == 0){
        assert(numJobs == 0);
        addJobs(Conf.MODELCHECK_BLOCK_SIZE);
      }
      jobLock.unlock();
      
      SatResult sat = results.take(); // Check whether it is satisfied
      nTraces++;
      
      if(sat.getSat())
        nSatisfied++;
      
      if(nTraces < Conf.MIN_TRACES) // Do not make a decision until we have enough traces
        continue;
      
      betaDist = new BetaDist(nSatisfied + alpha, nTraces - nSatisfied + beta);
      
      r.bayesFactor = prior * (1.0 / betaDist.cdf(theta) - 1);
      
      if(r.bayesFactor > T || (nTraces == Conf.MAX_TRACES && r.bayesFactor > 1)) {
        r.nullHypothesis = false;
        break;
      } else if (r.bayesFactor < 1.0 / T ||  (nTraces == Conf.MAX_TRACES && r.bayesFactor < 1)) {
        r.nullHypothesis = true;
        break;
      }
    }
    jobLock.lock();
    numJobs = 0;
    jobLock.unlock();
    
    r.n = nTraces;
    r.nSat = nSatisfied;
    
    return r;
  }
  
  public void lock() { jobLock.lock(); }
  
  public void unlock() { jobLock.unlock(); } 
  
  public int requestJobs(int limit) throws Exception {
    jobLock.lock();
    jobsDone.signal();
    while (numJobs == 0 && !done)
      jobsAvailable.await();

    if (done) {
      jobsAvailable.signalAll();
      jobLock.unlock();
      return -1;
    }

    int result = Math.min(numJobs, limit);
    numJobs = numJobs - result;
    numWorkersWorking++;
    jobLock.unlock();
    return result;
  }
  
  /**
   * Call only with lock already acquired!
   * @param numJobs
   */
  public void addJobs(int numJobs) {
    jobLock.lock();
    assert(this.numJobs == 0);
    this.numJobs = numJobs;
    jobsAvailable.signalAll();
    jobLock.unlock();
  }
  
  public void calculateDeterministicPolicy() { deterministicPolicy = policy.determinise(); }
  public void setDeterministic(boolean deterministic) { this.deterministic = deterministic; }
  
  public boolean isDone() { return done; }

  public Prism getPrism() { return prism; }
  public ModulesFile getModulesFile() { return modulesFile; }
  public Formula getFormula() { return formula; }
  
  public Rewards getRewards() { return rewards; }
  public Policy getPolicy() { return (deterministic) ? deterministicPolicy : policy; }
  
  
  /**
   * Prints out how much time each thread spent on each
   * of its tasks: generating traces, checking traces
   * against properties, and rewarding traces.
   */
  public void printThreadProfiling() {
    System.out.println("Thread profiling information");
    System.out.println(" - Thread[#]{generating, checking, rewarding}");
    int i = 0;
    for(TraceGeneratorThread t : workers)
      System.out.println(" - Thread[" + (i++) +"]{" + m2s(t.getTimeSimulating()) + "s, " + m2s(t.getTimeChecking()) + "s, " + m2s(t.getTimeRewarding()) + "s}");
  }
  
  /**
   * Averages thread times spent doing each task
   * @param pr The structure into which profiling information is inserted.
   */
  private void threadProfiling(Profiling pr) {
    pr.tAVGCheck = 0; pr.tAVGReward = 0; pr.tAVGSimulate = 0;
    for(TraceGeneratorThread t : workers) {
      pr.tAVGCheck += t.getTimeChecking();
      pr.tAVGSimulate += t.getTimeSimulating();
      pr.tAVGReward += t.getTimeRewarding();
    }
    pr.tAVGCheck    /= workers.size();
    pr.tAVGSimulate /= workers.size();
    pr.tAVGReward   /= workers.size();
  }
  
  /**
   * @param path File with the formula in it.
   * @return A string with the actual formula.
   * @throws Exception As usual.... BAD STUFF MAY HAPPEN.
   */
  public static String readFile(File path) throws Exception {
    Scanner s = new Scanner(new FileInputStream(path));
    String result = "";
    while(s.hasNextLine())
      result = result + (s.nextLine().trim());
    return result;
  }
  
  public static void main(String[] args) throws Exception {
    if(args.length < 5) {
      System.out.println("[ERROR] Not enough arguments." + Conf.newline);
      printHelp();
      System.exit(1);
    }
    
    File modulesF = new File(args[0]);
    if(!modulesF.exists()){
      System.err.println("[ERROR] Modules file \""+ args[0] +"\" does not exist.");
      System.exit(1);
    }
    
    File formulaFile = new File(args[1]);
    if(!formulaFile.exists()){
      System.err.println("[ERROR] Formula file \""+ args[1] +"\" does not exist.");
      System.exit(1);
    }
    
    Conf.NUM_THREADS = Integer.parseInt(args[2]);
    Conf.NUM_TRACES  = Integer.parseInt(args[3]);
    Conf.NUM_BLOCKS  = Integer.parseInt(args[4]);
    
    if(Conf.NUM_TRACES < 1 || Conf.NUM_BLOCKS < 1 || Conf.NUM_THREADS < 1) {
      System.err.println("The number of threads, number traces per block and number of blocks must be a positive integer.");
      System.exit(1);
    }
    
    boolean successfulParse = true;
    for(int n = 5; n < args.length; n++)
      successfulParse = successfulParse && Conf.parseOption(args[n]);
    
    if(!successfulParse){
      System.out.println();
      printHelp();
      return;
    }
    
    // Ignore Prism output
    PrismLog ll = new PrismPrintStreamLog(new PrintStream(new OutputStream(){ public void write(int b) {} }));

    // Create and initialise Prism.
    Prism p = new Prism(ll, ll);
    p.initialise();

    ModulesFile modulesFile = p.parseModelFile(modulesF);                      // PRISM reads modules file
    
    Formula formula = Parser$.MODULE$.parseFormula(readFile(formulaFile)); // Read and parse formula
    if(formula == null){
      System.err.println("Could not parse formula.");
      System.exit(1);
    }
    
    long startTime = System.currentTimeMillis();
    long tmpTime;
    
    LearnMDP lmdp = new LearnMDP(p, modulesFile, formula);
    
    Profiling pr = new Profiling();
    
    // Starting Threads
    if(Conf.PROGRESS_FLAG || Conf.DEBUG_FLAG) System.out.print("Starting threads... ");
    tmpTime = System.currentTimeMillis();
    lmdp.startThreads(Conf.NUM_THREADS, true);
    pr.tThreadStart = System.currentTimeMillis() - tmpTime;
    if(Conf.PROGRESS_FLAG || Conf.DEBUG_FLAG) System.out.println(m2s(pr.tThreadStart) + "s");
    
    // Learning algorithm
    if(Conf.PROGRESS_FLAG || Conf.DEBUG_FLAG) System.out.println("Learning...");
    tmpTime = System.currentTimeMillis();
    pr.tPolicyUpdate = lmdp.learn(Conf.NUM_TRACES, Conf.NUM_BLOCKS); // THIS IS OUR ALGORITHM! RIGHT HERE! LOOK AT ME!
    pr.tLearn = System.currentTimeMillis() - tmpTime;
    if(Conf.PROGRESS_FLAG || Conf.DEBUG_FLAG)
      System.out.println("Learning: " + m2s(pr.tLearn) + "s, Policy update: " + m2s(pr.tPolicyUpdate) + "s");
    
    // Policy determinisation
    if(Conf.DETERMINISE_FLAG) {
      if(Conf.PROGRESS_FLAG || Conf.DEBUG_FLAG) System.out.print("Calculating deterministic policy... ");
      tmpTime = System.currentTimeMillis();
      lmdp.calculateDeterministicPolicy();
      lmdp.setDeterministic(true);
      pr.tDeterminise = System.currentTimeMillis() - tmpTime;
      if(Conf.PROGRESS_FLAG || Conf.DEBUG_FLAG) System.out.println(m2s(pr.tDeterminise) + "s");
    }
    
    // Statistical Model Checking
    if(Conf.PROGRESS_FLAG || Conf.DEBUG_FLAG) System.out.print("Starting SMC... ");
    tmpTime = System.currentTimeMillis();
    
    lmdp.setThreadMode(false); // Set threads to SMC mode
    Result r;
    if(Conf.INT_EST_FLAG) r = lmdp.IntervalEstimation(Conf.ALPHA, Conf.BETA, Conf.DELTA, Conf.COEFFICIENT_EST);
    else                  r = lmdp.HypothesisTesting(Conf.THETA, Conf.T, Conf.ALPHA, Conf.BETA);
    pr.tSMC = System.currentTimeMillis() - tmpTime;
    lmdp.threadProfiling(pr);
    if(Conf.PROGRESS_FLAG || Conf.DEBUG_FLAG) {
      System.out.println(m2s(pr.tSMC) + "s");
      lmdp.printThreadProfiling();
      System.out.println("Stopping threads...");
    }
    lmdp.stopThreads();
    
    pr.sPolicy = lmdp.getPolicy().numStates();
    pr.tAll = System.currentTimeMillis() - startTime;
    
    // Print results
    if(Conf.MACHINE_FLAG) {
      String toPrint = r.CSV();
      if(Conf.PROFILING_FLAG) toPrint += ", " + pr.CSV();
      System.out.println(toPrint);
    } else {
      System.out.println(r);
      if(Conf.PROGRESS_FLAG || Conf.DEBUG_FLAG) System.out.println(" - Policy has " + pr.sPolicy + " states");
      if(!Conf.MACHINE_FLAG) System.out.println(" - Total run time: " + m2s(pr.tAll) + "s");  
    }
  }

  public static void printHelp() {
    System.out.println("Usage: sh run.sh modules_file formula_file num_threads number_traces_per_block number_of_blocks [OPTIONS]");
    System.out.println();
    System.out.println("OPTIONS are any of the following:");
    System.out.println(" ---- OUTPUT");
    System.out.println(" -DEBUG                  output debugging information");
    System.out.println(" -MACHINE                output machine-readable CSV results");
    System.out.println(" -PROFILE                output machine-readable profiling information");
    System.out.println(" -PROGRESS               output algorithm progress information");
    System.out.println();
    
    System.out.println(" ---- LEARNING");
    System.out.println(" -BayesLearning          use Bayes factor heuristic to stop learning");
    System.out.println(" -NoDeterminisation      do not determinise policy prior to SMC");
    System.out.println(" -checkpoint             use last action identification for checkpoint formulae");
    System.out.println(" -H=val                  set policy update history weight (default: 0.5)");
    System.out.println(" -iota=val               set an increment of the learning theta (default: 0.05)");
    System.out.println(" -learnT=val             set confidence of Bayes factor heuristic in learning (default: 30)");
    System.out.println(" -decay=val              set initial reinforcement before decay [decay] (default: 5)");
    System.out.println(" -pathsat=val            set reinforcement along successful path [lastaction] (default: 1)");
    System.out.println(" -lastsat=val            set reinforcement at the end of successful path [lastaction] (default: 1)");
    System.out.println(" -pathnsat=val           set reinforcement along unsuccessful path [lastaction] (default: 1)");
    System.out.println(" -lastnsat=val           set reinforcement at the end of unsuccessful path [lastaction] (default: 5)");
    System.out.println(" -reward=val             set reward method to one of uniform|decay|lastaction (default: uniform)");
    System.out.println("                         Note: to use decay or lastaction, set their parameters first");
    System.out.println("                               LastAction(1, 10000, 1, 1) should work well for reachability");
    System.out.println("                               LastAction(1, 10000, 1, 10000) should work well for staying within safe area");
    System.out.println(" -epsilon=val            set epsilon-learning parameter [epsilon] (default: 0.1)");
    System.out.println(" -update=val             set policy update method to one of count|compcount|epsilon|success|epcount (default: count)");
    System.out.println();
    
    System.out.println(" ---- STATISTICAL MODEL CHECKING {HT: hypothesis testing, IE: interval estimation}");
    System.out.println(" -IntervalEstimation     perform interval estimation instead of hypothesis testing");
    System.out.println(" -alpha=val              set Beta distribution alpha parameter [HT] (default: 0.5)");
    System.out.println(" -beta=val               set Beta distribution beta parameter [HT] (default: 0.5)");
    System.out.println(" -theta=val              set SMC problem probability bound [HT] (default: 0.5)");
    System.out.println(" -T=val                  set hypothesis testing threshold [HT] (default: 1000)");
    System.out.println(" -mintraces=val          set lower-bound on sampling [HT] (default: 500)");
    System.out.println(" -maxtraces=val          set bound on sampling [HT] (default: 100000)");
    System.out.println(" -delta=val              set interval estimation interval half-size [IE] (default: 0.1)");
    System.out.println(" -coeff=val              set interval estimation confidence [IE] (default: 0.95)");
    System.out.println();
  }
  private static double m2s(double millis) {
    return millis /1000.0; 
  }
  
  static class Profiling {
    public long tThreadStart;  // Time spent starting threads
    public long tLearn;        // Time spent inside learning algorithm
    public long tPolicyUpdate; // Time spent in policy updates
    public long tDeterminise;  // Time spent determinising policy
    public long tSMC;          // Time spent in SMC
    public long tAVGCheck;     // Average time spent checking trace/formula satisfaction
    public long tAVGSimulate;  // Average time spent generating traces
    public long tAVGReward;    // Average time spent rewarding paths
    public long tAll;          // Total time spent (excluding PRISM initialisation)
    public long sPolicy;       // Number of states in the learned policy
    public String CSV() {
      return m2s(tThreadStart) + ", " + m2s(tLearn) + ", " + m2s(tPolicyUpdate) + ", " + m2s(tDeterminise) +
             ", " + m2s(tSMC) + ", " + m2s(tAVGCheck) + ", " + m2s(tAVGSimulate) + ", " + m2s(tAVGReward) +
             ", " + m2s(tAll) + ", " + sPolicy;
    }
  }
}
