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

package smcmdp.reward.update;

import java.util.*;

import parser.*;
import smcmdp.*;
import smcmdp.policy.*;
import smcmdp.reward.*;

public class LastActionRU implements RewardUpdate {
  
  protected int satPath, satLast, nsatPath, nsatLast;
  
  public LastActionRU(int satPath, int satLast, int nsatPath, int nsatLast){
    this.satPath = satPath; this.satLast = satLast; this.nsatPath = nsatPath; this.nsatLast = nsatLast;
  }
  
  @Override
  public void update(Rewards r, List<Step> path, SatResult sat) {
    Iterator<Step> i = path.iterator();
    for(int n = 0; n < sat.getNSteps(); n++){
      Step step = i.next();
      if(step.numChoices < 2) continue;
      
      State state = step.state;
      r.initialise(state, step.numChoices);
      
      int choice = step.choiceTaken;
      StateReward stateRewards = r.getLocalReward(state);
      if(stateRewards == null) // Single choice state
        continue;
      ChoiceReward choiceStateReward = stateRewards.get(choice);
           if( sat.getSat() && n <  sat.getNSteps() - 1) choiceStateReward.addSat(satPath);
      else if( sat.getSat() && n == sat.getNSteps() - 1) choiceStateReward.addSat(satLast);
      else if(!sat.getSat() && n <  sat.getNSteps() - 1) choiceStateReward.addNSat(nsatPath);
      else if(!sat.getSat() && n == sat.getNSteps() - 1) choiceStateReward.addNSat(nsatLast);
    }
  }
}
