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

import java.util.concurrent.atomic.*;

/**
 * The class for a single reward. Uses AtomicIntegers to implement
 * hardware level synchronisation when possible.
 */
public class ChoiceReward {
  private AtomicInteger sat;
  private AtomicInteger nSat;
  
  public ChoiceReward() { this(0, 0); } 
  
  public ChoiceReward(int sat, int nSat) {
    this.sat = new AtomicInteger(sat);
    this.nSat = new AtomicInteger(nSat);
  }
  
  public void incrementSat() { sat.incrementAndGet(); }
  public void incrementNSat() { nSat.incrementAndGet(); }
  
  public void addSat(int n) { sat.addAndGet(n); }
  public void addNSat(int n) { nSat.addAndGet(n); }
  
  public int getSat() { return sat.get(); }
  public int getNSat() { return nSat.get(); }
  
  public void reset() { sat.set(0); nSat.set(0); }
  
  public String toString() { return "(" + sat + ", " + nSat + ")"; }
}