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

package smcmdp.reward;

import java.util.*;
import java.util.concurrent.*;
//import java.util.concurrent.locks.*;

import parser.*;
import smcmdp.*;
import smcmdp.policy.*;

/**
 * Class containing all the rewards for one block of traces.
 * For each state that has been visited, and each possible choice
 * from those states we assign two numbers: the number of traces
 * that passed through and satisfied the property and the number
 * of traces that passed through but did not satisfy it.
 */
public class Rewards {
  private ConcurrentHashMap<State, StateReward> r;
  
  public Rewards() { this(1000); }
  
  public Rewards(int n) {
    r = new ConcurrentHashMap<State, StateReward>(n);
  }

  /**
   * @param state A state
   * @return Whether the state already has rewards or not
   */
  public boolean initialised(State state) {
    return r.containsKey(state);
  }

  /**
   * Initialises the data-structures to maintain information about this state
   * @param state A state
   * @param numChoices The number of possible choices from this state
   * @return Whether initialisation was successful, i.e. it was not initialised before
   */
  public boolean initialise(State state, int numChoices) {
    return r.putIfAbsent(state, new StateReward(state, numChoices)) == null;
  }
  
  /**
   * Given a trace and whether it was satisfied, accumulate rewards
   * along all states of the path positively or negatively depending
   * on whether the property was satisfied, up to the state at which
   * the property was finally satisfied (and no further).
   * @param path A sequence of Steps, containing states
   * @param sat Whether the formula was satisfied by this trace, and
   * when the formula was satisfied.
   */
  public void updateRewards(List<Step> path, SatResult sat){
    Conf.REWARD_UPDATE.update(this, path, sat);
  }
  
  public void reset() {
    r.clear();
  }

  public Map<State, StateReward> getRewards() { return r; }
  public StateReward getLocalReward(State state) { return r.get(state); }
  
  public String toString() { return r.toString(); }
}