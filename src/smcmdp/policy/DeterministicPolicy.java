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

import parser.*;

/**
 * This class stores a partially deterministic policy. It is deterministic
 * except for those states that were uninitialised before and that come up
 * during simulation.
 * 
 * A policy for a state is given by the index of the deterministic choice,
 * or alternatively by a uniform list of doubles.
 */
public class DeterministicPolicy extends Policy {
  private Map<State, Integer> detP;
  
  public DeterministicPolicy(int numStates){
    super();
    detP = new HashMap<State, Integer>(numStates);
  }
  
  public DeterministicPolicy(){
    super();
    detP = new HashMap<State, Integer>(10000);
  }
  
  public void addDeterministicChoice(State state, int choiceIndex) {
    detP.put(state, choiceIndex);
  }
  
  /** 
   * @param state a state
   * @param choiceIndex a choice
   * @return Probability of choosing choice given by choiceIndex from state s.
   */
  public Double getProbability(State state, int choiceIndex){
    if(detP.containsKey(state))
      return (detP.get(state) == choiceIndex) ? 1.0 : 0;
    
    return super.getProbability(state, choiceIndex);
  }
  
  /**
   * @param state
   * @return Whether the policy is defined for the given state.
   */
  public boolean defined(State state){
    return detP.containsKey(state) || super.defined(state);
  }
 
  public int getIndexByProbabilitySum(State state, double x) {
    if(detP.containsKey(state))
      return detP.get(state);
    
    return super.getIndexByProbabilitySum(state, x);
  }
  
  public int numStates() { return p.size() + detP.size(); }
}
