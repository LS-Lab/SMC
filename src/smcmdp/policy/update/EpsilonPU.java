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

package smcmdp.policy.update;

import java.util.*;
import smcmdp.*;
import smcmdp.policy.*;
import smcmdp.reward.*;
import parser.*;

public class EpsilonPU implements PolicyUpdate {

  @Override
  public void update(Policy p, Rewards rewards) {
    //TODO double H = Conf.HISTORY;
    double epsilon = Conf.EPSILON;
    
    // Go through all states for which we have rewards
    for(Map.Entry<State, StateReward> e: rewards.getRewards().entrySet()){
      State state = e.getKey();                  // The state
      StateReward localRewards = e.getValue();  // Rewards each choice of the state
      StatePolicy localPolicy = p.getStatePolicy(state);   // Policy for this state, i.e. probabilities for each choice
      
      assert(localPolicy.size() == localRewards.size());
      
      // Compute condensed rewards, i.e. nSat / (nSat + nNotSat)
      double max = Double.NEGATIVE_INFINITY;
      int maxIndex = -1;
      int policySize = localPolicy.size();
      for(int i = 0; i < policySize; i++) {
        ChoiceReward r = localRewards.get(i);
        int sat = r.getSat();
        int nsat = r.getNSat();
        // If we never tried this action-state pair, we simply use the previous policy's value
        double successProb = (sat + nsat > 0) ? 1.0 * sat / (sat + nsat) : localPolicy.get(i);
        localPolicy.set(i, epsilon/(policySize-1));
        if(successProb > max) {
          max = successProb;
          maxIndex = i;
        }
      }
      
      
      localPolicy.set(maxIndex, 1-epsilon);
    }
  }

}
