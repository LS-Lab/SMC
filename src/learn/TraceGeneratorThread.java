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

import java.util.*;
import java.util.concurrent.*;

import smcmdp.*;
import smcmdp.policy.*;
import parser.*;
import prism.*;
import simulator.*;
import parser.State;

public class TraceGeneratorThread extends SimulatorEngine implements Runnable {
  private LearnMDP server;
  
  protected List<Step> steps;
  
  protected BlockingQueue<SatResult> results;
  
  protected long timeSimulating; // Time spent generating traces
  protected long timeChecking;   // Time spent checking traces against formula
  protected long timeRewarding;  // Time spent rewarding states along traces
  
  public TraceGeneratorThread(LearnMDP server) {
    this(server, null);
  }
  
  public TraceGeneratorThread(LearnMDP server, BlockingQueue<SatResult> results) {
    super(server.getPrism());
    this.server = server;
    this.results = results;
    this.steps = new LinkedList<Step>();
    this.rng = new SynchronizedRNG(server);
    timeSimulating = 0;
    timeChecking = 0;
    timeRewarding = 0;
  }
  
  public void run() {
    try {
      int traceSize = Satisfaction$.MODULE$.traceSize(server.getFormula()) + 1;
      
      State initialState = server.getModulesFile().getDefaultInitialState();
      
      this.createNewPath(server.getModulesFile());       // Initialising path in the engine
      
      while(true){
        int nJobs = server.requestJobs(Conf.NUM_JOBS_PER_REQUEST);
        if (nJobs == -1) return; // My work here is done *tips hat and walks away*
        
        for(int j = 0; j < nJobs - 1; j++){
          SatResult sat = runJob(traceSize, initialState);
          reward(sat);
        }
        
        // For the last iteration, we have to atomically add the final result and
        // decrease the number of working threads (deadlock otherwise)
        SatResult sat = runJob(traceSize, initialState);
        server.lock();
        reward(sat);
        server.numWorkersWorking--;
        server.unlock();
      }
    } catch(Exception e) { e.printStackTrace(); }
  }
  
  protected void reward(SatResult sat) throws Exception {
    long timer = System.currentTimeMillis();
    if(results == null) rewardPath(sat);  // Compute rewards for this path
    else                results.put(sat); // Or simply "output" the result
    if(sat.getSat()) server.sat.incrementAndGet();
    else             server.fal.incrementAndGet();
    timeRewarding += System.currentTimeMillis() - timer;
  }

  protected SatResult runJob(int traceSize, State initialState) throws Exception {
    // Generating trace
    long timer = System.currentTimeMillis();
    PathFull path = this.getPathFull();
    
    this.initialisePath(new State(initialState));
    
    this.automaticTransitions(traceSize, false);   // Run each individual trace for however many the formula requires
    timeSimulating += System.currentTimeMillis() - timer;
    
    // Checking formula
    timer = System.currentTimeMillis();
    Trace tr = new Trace(path, this.getVarList()); // Get a trace we can actually satisfy

    SatResult result = Satisfaction$.MODULE$.interpret(tr, server.getFormula()); // Check whether it is satisfied
    timeChecking += System.currentTimeMillis() - timer;
    return result;
  }
  
  public void setResultQueue(BlockingQueue<SatResult> results){
    this.results = results;
  }
  
  public Policy getPolicy() {
    return server.getPolicy();
  }
  
  public void rewardPath(SatResult sat) {
    server.getRewards().updateRewards(steps, sat);
  }

  public void initialisePath(State initialState) throws PrismException {
    super.initialisePath(initialState);
    steps = new LinkedList<Step>();
  }
  
  /**
   * Select, according to policy, a transition from the current transition list and execute it.
   * For continuous-time models, the time to be spent in the state before leaving is also picked randomly.
   * If there is currently a deadlock, no transition is taken and the function returns false.
   * Otherwise, the function returns true indicating that a transition was successfully taken. 
   */
  public boolean automaticTransition() throws PrismException {
    return automaticTransition(getPolicy());
  }
  
  public boolean automaticTransition(Policy p) throws PrismException {
    Choice choice;
    int numChoices, i, j;
    double d;

    // Check for deadlock; if so, stop and return false
    numChoices = transitionList.getNumChoices();
    if (numChoices == 0)
      return false;
    //throw new PrismException("Deadlock found at state " + path.getCurrentState().toString(modulesFile));
    
    // Initialise policy incrementally and uniformly (if this state was not visited before)
    if(numChoices > 1 && !p.defined(this.currentState))
      p.initialise(new State(this.currentState), numChoices);
    
    switch (modelType) {
    case DTMC:
    case MDP:
      // Pick a random choice
      if(numChoices == 1)
        i = 0;
      else {
        d = rng.randomUnifDouble();
        if(!p.defined(this.currentState))
          System.out.println("LOL"+numChoices);
        i = p.getIndexByProbabilitySum(this.currentState, d);
      }
      choice = transitionList.getChoice(i);
      // Pick a random transition from this choice
      d = rng.randomUnifDouble();
      j = choice.getIndexByProbabilitySum(d);
      // Execute
      executeTransition(i, j, -1);
      break;
    case CTMC:
      System.err.println("*** ERROR *** Cannot simulate CTMCs. Sorry!");
      return false;
    }

    return true;
  }
  
  protected void executeTransition(int i, int offset, int index) throws PrismException {
    steps.add(new Step(path.getCurrentState(), i, offset, transitionList.getNumChoices())); // path.getCurrentState() should not change with transition
    super.executeTransition(i, offset, index);
  }
  
  public VarList getVarList() { return varList; }

  public long getTimeSimulating() { return timeSimulating; }
  public long getTimeChecking()   { return timeChecking;   }
  public long getTimeRewarding()  { return timeRewarding;  }
}
