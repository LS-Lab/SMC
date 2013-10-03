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

package smcmdp.policy;

import java.util.*;
import java.util.concurrent.*;

import parser.*;
import smcmdp.*;
import smcmdp.reward.*;

/**
 * This class stores a probabilistic policy. In other words,
 * it will assign a probability distribution over choices
 * to each state of the MDP.
 */
public class Policy {
  protected Map<State, StatePolicy> p;
  
  public Policy(){
    this.p = new ConcurrentHashMap<State, StatePolicy>(500000);
  }
  
  /**
   * @param state A state
   * @return The policy for that state, null otherwise
   */
  public StatePolicy getStatePolicy(State state){ return p.get(state); }
  
  /** 
   * @param state a state
   * @param choiceIndex a choice
   * @return Probability of choosing choice given by choiceIndex from state s.
   */
  public Double getProbability(State state, int choiceIndex){
    StatePolicy choice = getStatePolicy(state);
    if(choice == null)
      return -1.0;
    
    return choice.get(choiceIndex);
  }
  
  /**
   * @param state
   * @return Whether the policy is defined for the given state.
   */
  public boolean defined(State state){
    return p.containsKey(state);
  }
  
  /**
   * Initialises a policy for the given state with a uniform distribution.
   * @param state
   * @param numChoices
   * @return
   */
  public boolean initialise(State state, int numChoices){
    StatePolicy choice = getStatePolicy(state);
    if(choice == null) {
      choice = new StatePolicy(state, numChoices);
      
      p.put(state, choice);
      return true;
    }
    
    //System.err.println("*** WARNING *** Attempting to reinitialise policy for state " + state);
    return false;
  }
 
  // Adapted from SimulatorEngine.java
  public int getIndexByProbabilitySum(State state, double x) {
    List<Double> choice = getStatePolicy(state).getPolicy();
    int n = choice.size();
    double d = 0.0;
    int i;
    for (i = 0; x >= d && i < n; i++)
      d += choice.get(i);
    return i - 1;
  }
  
  /**
   * Updates the policy according to the given rewards (typically
   * generated from a set of traces checked against a property),
   * and the memory parameter alpha.
   * @param rewards
   * @param alpha
   */
  public void update(Rewards rewards){
    Conf.POLICY_UPDATE.update(this, rewards);
  }
  
  public void epsilonSuccessUpdate(Rewards rewards, double history, double epsilon){
    if(history < 0 || history > 1 || epsilon < 0 || epsilon > 1) System.err.println("*** WARNING *** Alpha/epsilon should be between 0 and 1.");
    
    // Go through all states for which we have rewards
    for(Map.Entry<State, StateReward> e: rewards.getRewards().entrySet()){
      State state = e.getKey();                  // The state
      StateReward localRewards = e.getValue();  // Rewards each choice of the state
      StatePolicy localPolicy = getStatePolicy(state);   // Policy for this state, i.e. probabilities for each choice
      
      assert(localPolicy.size() == localRewards.size());
      
      // Compute condensed rewards, i.e. nSat / (nSat + nNotSat)
      double max = Double.NEGATIVE_INFINITY;
      int maxIndex = -1;
      double[] successProbs = new double[localRewards.size()];
      for(int i = 0; i < localPolicy.size(); i++) {
        ChoiceReward r = localRewards.get(i);
        int sat = r.getSat();
        int nsat = r.getNSat();
        // If we never tried this action-state pair, we simply use the previous policy's value
        double successProb = (sat + nsat > 0) ? 1.0 * sat / (sat + nsat) : localPolicy.get(i);
        successProbs[i] = successProb;
        if(successProb > max) {
          max = successProb;
          maxIndex = i;
        }
      }
      
      // Count the sum for normalisation
      double normConst = 0;
      for(int i = 0; i < localRewards.size(); i++)
        normConst = normConst + successProbs[i];
      
      // Update probability with alpha*(old probability) + (1-alpha)* (% counts)
      if(normConst == 0)
        continue;
      for(int i = 0; i < localPolicy.size(); i++) {
        double newProbability = epsilon * successProbs[i] / normConst;
        if(i == maxIndex)
          newProbability = newProbability + 1 - epsilon;
        double currentProbability = localPolicy.get(i);
        double updatedProbability = (1-history)*currentProbability + history*(newProbability);
        localPolicy.set(i, updatedProbability);
      }
    }
  }
  
  
  public int numStates() { return p.size(); }
  
  public void outputPolicy() {
    System.out.println(p);
  }
  
  public DeterministicPolicy determinise() {
    DeterministicPolicy det = new DeterministicPolicy(this.numStates());
    for(State s: p.keySet()){
      // Find index of max choice
      int maxIndex = -1;
      double max = Double.MIN_VALUE;
      StatePolicy l = getStatePolicy(s);
      for(int i = 0; i < l.size(); i++){
        if(l.get(i) < max) continue;
        maxIndex = i;
        max = l.get(i);
      }
      
      // Use found max 
      det.addDeterministicChoice(s, maxIndex);
    }
    return det;
  }
  
  public String toString() { return p.toString(); }
  
  //public List<StatePolicy> getPolicyVector() { return pVector; }
}
