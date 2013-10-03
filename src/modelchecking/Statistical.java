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

package modelchecking;

import java.util.*;

import smcmdp.*;
import umontreal.iro.lecuyer.probdist.BetaDist;

public class Statistical {

  public static EstimationResult IntervalEstimation(Queue<SatResult> results,
            double alpha, double beta,
            double delta, double coefficient) throws Exception {
    EstimationResult r = new EstimationResult();
    
    int nTraces = 0;
    int nSatisfied = 0;
    double postProb = 0;
    do {
      SatResult sat = results.poll(); // Check whether it is satisfied
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
    r.n = nTraces;
    r.nSat = nSatisfied;
    //System.out.println();
    return r;
  }
}