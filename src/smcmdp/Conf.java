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

package smcmdp;

import smcmdp.policy.update.*;
import smcmdp.reward.update.*;

public class Conf {
  
  // Policy Learning parameters
  public static double HISTORY = 0.5;            // History weight in learning
  public static double EPSILON = 0.1;            // Epsilon for epsilon improvement policy
  public static double LEARN_T = 30;             // Hypothesis testing factor threshold
  public static double IOTA    = 0.05;           // If a scheduler is better than theta + iota, improvement stops
  
  public static PolicyUpdate POLICY_UPDATE = new CountPU();   // Policy update method
  
  
  // Reward updates
  public static int DECAY_START = 5;             // Starting reinforcement for decay rewards
  public static int PATH_SAT_RU = 1;             // Reinforcement along successful path
  public static int LAST_SAT_RU = 1;             // Reinforce at the end of successful path
  public static int PATH_NSAT_RU = 1;            // Reinforcement along unsuccessful path
  public static int LAST_NSAT_RU = 5;            // Reinforcement at the end of unsuccessful path
  public static RewardUpdate REWARD_UPDATE = new UniformRU(); // Reward update method
  
  // Beta distribution parameters
  public static double ALPHA = 0.5;              // Alpha parameter for Beta distribution
  public static double BETA = 0.5;               // Beta parameter for Beta distribution
  
  // Hypothesis testing parameters
  public static double THETA = 0.5;              // Hypothesis testing threshold
  public static double T = 1000;                 // Hypothesis testing factor threshold
  public static int MIN_TRACES = 500;            // A lower bound on the number of traces
  public static int MAX_TRACES = 100000;         // A bound on the number of traces
  
  // Interval estimation parameters
  public static double DELTA = 0.01;             // Half-size of interval for interval estimation
  public static double COEFFICIENT_EST = 0.95;   // Confidence for interval estimation
  
  public static boolean DEBUG_FLAG = false;      // Output debug information
  public static boolean MACHINE_FLAG = false;    // Machine readable output
  public static boolean PROFILING_FLAG = false;  // Machine readable profiling information
  public static boolean PROGRESS_FLAG = false;   // Output progress information
  public static boolean BAYES_LEARN_FLAG = false;// Bayes factor to stop learning
  public static boolean INT_EST_FLAG = false;    // Interval estimation instead of hypothesis testing
  public static boolean DETERMINISE_FLAG = true; // Determinise policy before SMC
  
  // Use a special checking rule that deals with formulae of the form
  // SAFE_1 U (CHECKPOINT & (SAFE_2 U FINAL))
  public static boolean CHECKPOINT_LAST_ACTION = false;
  
  public static int NUM_THREADS = 1;
  public static int NUM_TRACES = 2000;
  public static int NUM_BLOCKS = 30;
  
  public static int NUM_JOBS_PER_REQUEST = 50;
  public static int MODELCHECK_BLOCK_SIZE = 100;
  
  public static boolean parseOption(String opt) {
    boolean result = true;
    if(opt.equals("-DEBUG")) {
      DEBUG_FLAG = true;
      System.out.println("Enabling debug mode...");
    } else if(opt.equals("-MACHINE")) {
      MACHINE_FLAG = true;
      if(Conf.DEBUG_FLAG) System.out.println("Enabling machine readable profiling...");
    } else if(opt.equals("-PROFILE")) {
      PROFILING_FLAG = true;
      if(Conf.DEBUG_FLAG) System.out.println("Enabling machine readable output...");
    } else if(opt.equals("-PROGRESS")) {
      PROGRESS_FLAG = true;
      if(Conf.DEBUG_FLAG) System.out.println("Enabling progress output...");
    } else if(opt.equals("-BayesLearning")) {
      BAYES_LEARN_FLAG = true;
      if(Conf.DEBUG_FLAG) System.out.println("Enabling Bayes factor learning heuristic...");
    } else if(opt.equals("-NoDeterminisation")) {
      DETERMINISE_FLAG = false;
      if(Conf.DEBUG_FLAG) System.out.println("Disabling determinisation before SMC...");
    } else if(opt.equals("-checkpoint")) {
      CHECKPOINT_LAST_ACTION = true;
      if(Conf.DEBUG_FLAG) System.out.println("Enabling last action identification for checkpoint formulae...");
    } else if(opt.equals("-IntervalEstimation")) {
      INT_EST_FLAG = true;
      if(Conf.DEBUG_FLAG) System.out.println("Doing interval estimation instead of hypothesis testing...");
    } else if(opt.charAt(0) != '-'){
      System.err.println("[ERROR] Unrecognised option: " + opt);
      result = false;
    } else if(opt.indexOf('=') != -1){
      String[] split = opt.split("=");
      if(split[0].equals("-H")) {                                    // HISTORY
        HISTORY = Double.parseDouble(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting history weight to " + HISTORY + "...");
      } else if(split[0].equals("-epsilon")) {                       // EPSILON
        EPSILON = Double.parseDouble(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting epsilon-exploration to " + EPSILON + "...");
      } else if(split[0].equals("-learnT")) {                        // EPSILON
        LEARN_T = Double.parseDouble(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting learn threshold to " + LEARN_T + "...");
      } else if(split[0].equals("-iota")) {                        // EPSILON
        IOTA = Double.parseDouble(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting learn threshold to " + IOTA + "...");
      } else if(split[0].equals("-alpha")) {                         // ALPHA
        ALPHA = Double.parseDouble(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting alpha parameter of BetaDist to " + ALPHA + "...");
      } else if(split[0].equals("-beta")) {                          // BETA
        BETA = Double.parseDouble(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting beta parameter for BetaDist to " + BETA + "...");
      } else if(split[0].equals("-theta")) {                         // THETA
        THETA = Double.parseDouble(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting hypothesis testing probability to " + THETA + "...");
      } else if(split[0].equals("-T")) {                             // T
        T = Double.parseDouble(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting Bayes factor threshold to " + T + "...");
      } else if(split[0].equals("-mintraces")) {                     // MIN_TRACES
        MIN_TRACES = Integer.parseInt(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting hypothesis testing sampling lower-bound to " + MIN_TRACES + "...");
      } else if(split[0].equals("-maxtraces")) {                     // MAX_TRACES
        MAX_TRACES = Integer.parseInt(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting hypothesis testing sampling bound to " + MAX_TRACES + "...");
      } else if(split[0].equals("-delta")) {                         // DELTA
        DELTA = Double.parseDouble(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting interval half-size to " + DELTA + "...");
      } else if(split[0].equals("-coeff")) {                         // COEFFICIENT_EST
        COEFFICIENT_EST = Double.parseDouble(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting coefficient for Interval Estimation to " + COEFFICIENT_EST + "...");
      } else if(split[0].equals("-update")) {                        // POLICY UPDATE METHODS
        if(split[1].equals("count")){
          POLICY_UPDATE = new CountPU();
          if(Conf.DEBUG_FLAG) System.out.println("Setting policy update to count method...");
        } else if(split[1].equals("compcount")){
          POLICY_UPDATE = new CompleteCountPU();
          if(Conf.DEBUG_FLAG) System.out.println("Setting policy update to complete count method...");
        } else if(split[1].equals("success")){
          POLICY_UPDATE = new SuccessPU();
          if(Conf.DEBUG_FLAG) System.out.println("Setting policy update to success probability method...");
        } else if(split[1].equals("epsilon")){
          POLICY_UPDATE = new EpsilonPU();
          if(Conf.DEBUG_FLAG) System.out.println("Setting policy update to epsilon exploration method...");
        } else if(split[1].equals("epcount")){
          POLICY_UPDATE = new EpsilonCountPU();
          if(Conf.DEBUG_FLAG) System.out.println("Setting policy update to epsilon count method...");
        } else {
          System.err.println("[ERROR] Unrecognised policy update method: " + split[1]);
          result = false;
        }
      } else if(split[0].equals("-decaystart")) {
        DECAY_START = Integer.parseInt(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting starting reinforcement before decay to " + DECAY_START + "... [needs -reward=decay]");
      } else if(split[0].equals("-pathsat")) {
        PATH_SAT_RU = Integer.parseInt(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting reinforcement along successful path to " + PATH_SAT_RU + "... [needs -reward=lastaction]");
      } else if(split[0].equals("-lastsat")) {
        LAST_SAT_RU = Integer.parseInt(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting reinforcement at the end of successful path to " + LAST_SAT_RU + "... [needs -reward=lastaction]");
      } else if(split[0].equals("-pathnsat")) {
        PATH_NSAT_RU = Integer.parseInt(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting reinforcement along unsuccessful path to " + PATH_SAT_RU + "... [needs -reward=lastaction]");
      } else if(split[0].equals("-lastnsat")) {
        LAST_NSAT_RU = Integer.parseInt(split[1]);
        if(Conf.DEBUG_FLAG) System.out.println("Setting reinforcement at the end of unsuccessful path to " + LAST_NSAT_RU + "... [needs -reward=lastaction]");
      } else if(split[0].equals("-reward")) {
        if(split[1].equals("uniform")){
          REWARD_UPDATE = new UniformRU();
          if(Conf.DEBUG_FLAG) System.out.println("Setting uniform path rewards...");
        } else if(split[1].equals("lastaction")){
          REWARD_UPDATE = new LastActionRU(PATH_SAT_RU, LAST_SAT_RU, PATH_NSAT_RU, LAST_NSAT_RU);
          if(Conf.DEBUG_FLAG) System.out.println("Setting path reward to Last Action("+
              PATH_SAT_RU + "," + LAST_SAT_RU + "," + PATH_NSAT_RU + "," + LAST_NSAT_RU+")...");
        } else if(split[1].equals("decay")){
          REWARD_UPDATE = new DecayRU(DECAY_START);
          if(Conf.DEBUG_FLAG) System.out.println("Setting path reward method to Decay("+DECAY_START+")...");
        } else {
          System.err.println("[ERROR] Unrecognised reward method: " + split[1]);
          result = false;
        }         
      }  else {
        System.err.println("[ERROR] Unrecognised option: " + opt);
        result = false;
      }
    } else {
      System.err.println("[ERROR] Unrecognised option: " + opt);
      result = false;
    }
    return result;
  }
  
  public static String newline = System.getProperty("line.separator");
}
