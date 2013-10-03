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

import parser.VarList;
import parser.State;
import simulator.PathFull;
import scala.util.parsing.combinator._;

package smcmdp {

/**
 * A Term can be either:
 * - A variable Var with a string indicating its name,
 * - A function Fn, where f is a character such as +, -, *, etc,
 *   and two terms. The second one is optional for unary operators.
 * - A number Num, with a double representing its value.
 */
sealed abstract class Term
case class Var(s: String) extends Term
case class Fn(f: String, t1: Term, t2:Term) extends Term
case class Num(d: Double) extends Term
case class Form(f: Formula) extends Term

/**
 * A predicate is defined by its string, such as <=, ==, != and others,
 * along with a list of terms.
 */
abstract class Pred
case class R(r: String, ps: List[Term]) extends Pred

/** Just the regular connectives */
sealed abstract class Connective
case object And extends Connective
case object Or extends Connective
case object Imp extends Connective
case object Iff extends Connective

/** 
 * Bounded temporal operators. The others can be defined from these.
 * Weak Until does not require the second formula to hold, Strong Until does.
 */
sealed abstract class TConnective
case class WeakUntil(b: Int) extends TConnective
case class StrongUntil(b: Int) extends TConnective

/**
 * A formula is either a boolean, an predicate, the negation of a formula,
 * a binary operator, or a temporal operator.
 */
sealed abstract class Formula
case object True extends Formula
case object False extends Formula
case class Atom(p: Pred) extends Formula
case class Not(f: Formula) extends Formula
case class Binop(c: Connective, f1 : Formula, f2: Formula) extends Formula
case class TempOp(tc: TConnective, f1 : Formula, f2: Formula) extends Formula

/**
 * This is the class of traces we can actually verify. It takes a 
 * PRISM path, a starting index (usually 0 at first), and a VarList
 * used to index variables.
 */
class Trace(var tr: PathFull, var start: Int, var vars: VarList) {
  
  def this(tr: PathFull, vars: VarList) = this(tr, 0, vars);
  def this() = this(null, 0, null);
  def this(other: Trace) = this(other.tr, other.start, other.vars);
  
  def getState() : State = { tr.getState(start); }
  
  /**
   * Decodes the variable given by varName at the current state given
   * by start. It will decode integers, doubles and booleans into
   * doubles. 
   */
  def getVar(varName: String) : Double = { 
    var o : Object = getState().varValues(vars.getIndex(varName));
    
    if(o.isInstanceOf[java.lang.Double]) return o.asInstanceOf[java.lang.Double];
    if(o.isInstanceOf[java.lang.Integer]) return 1.0 * o.asInstanceOf[java.lang.Integer];
    if(o.isInstanceOf[Boolean]) {
      if(o.asInstanceOf[Boolean])
        return 1.0;
      else
        return 0.0;
    }
    println("*** ERROR *** Bad evaluation of variable! Returning 0 by default.");
    return 0;
  }
  
  /**
   * Returns whether the current state is the last in the trace.
   */
  def hasNext() : Boolean = { start < tr.size() - 1; }
  
  /**
   * Moves the trace to the next position and returns itself (for convenience).
   */
  def next() : Trace = { start = start + 1; this; }
}

class SatResult(var sat: Boolean, var nSteps: Int){
  
  def &&(that: SatResult) : SatResult = {
    this.nSteps =
      (this.sat, that.sat) match {
        case (true, true)   => math.max(this.nSteps, that.nSteps); 
        case (false, false) => math.min(this.nSteps, that.nSteps);
        case (true, false)  => that.nSteps; 
        case (false, true)  => this.nSteps;
      }
    this.sat = this.sat && that.sat;
    this;
  }
  
  def ||(that: SatResult) : SatResult = {
    this.nSteps =
      (this.sat, that.sat) match {
        case (true, true) => math.min(this.nSteps, that.nSteps); 
        case (false, false) => math.max(this.nSteps, that.nSteps);
        case (true, false) => this.nSteps;
        case (false, true) => that.nSteps;
      }
    this.sat = this.sat || that.sat;
    this;
  }
  
  def ->(that: SatResult) : SatResult = {
    (!this) || that;
  }
  
  def <=>(that: SatResult) : SatResult = {
    (this -> that) && (that -> this)
  }
  
  def unary_! : SatResult = {
    this.sat = !this.sat;
    this;
  }
  
  override def toString = "["+sat+", "+nSteps+"]";
  
  def getSat: Boolean = sat;
  def getNSteps: Int = nSteps;
}

/**
 * Singleton that handles satisfaction of traces.
 */
object Satisfaction {
  
  /**
   * Interpretation of a term given a trace.
   * Returns the double representation of the term at the current state.
   */
  def interpret(tr: Trace, t: Term) : Double = {
    t match {
      case Var(s: String) =>  tr.getVar(s);
      case Num(d: Double) => d;
      case Form(f: Formula) => if (interpret(tr, f).getSat) 1 else 0;
      case Fn("+", t1: Term, t2: Term) => interpret(tr, t1) + interpret(tr, t2);
      case Fn("-", t1: Term, t2: Term) => interpret(tr, t1) - interpret(tr, t2);
      case Fn("*", t1: Term, t2: Term) => interpret(tr, t1) * interpret(tr, t2);
      case Fn("/", t1: Term, t2: Term) => interpret(tr, t1) / interpret(tr, t2);
      case Fn("^", t1: Term, t2: Term) => scala.math.pow(interpret(tr, t1), interpret(tr, t2));
      case _ => println("Can't interpret " + t); (-1);
    }
  }
   
  /**
   * Interpretation of a predicate given a state.
   * Returns a boolean for whether or not the predicate is satisfied at the current state.
   */
  def interpret(tr: Trace, p: Pred): SatResult = {
    p match {
      case R("=", List(t1: Term, t2: Term))  => new SatResult(interpret(tr, t1) == interpret(tr, t2), tr.start);
      case R("!=", List(t1: Term, t2: Term)) => new SatResult(interpret(tr, t1) != interpret(tr, t2), tr.start);
      case R(">", List(t1: Term, t2: Term))  => new SatResult(interpret(tr, t1) > interpret(tr, t2), tr.start);
      case R("<", List(t1: Term, t2: Term))  => new SatResult(interpret(tr, t1) < interpret(tr, t2), tr.start);
      case R(">=", List(t1: Term, t2: Term)) => new SatResult(interpret(tr, t1) >= interpret(tr, t2), tr.start);
      case R("<=", List(t1: Term, t2: Term)) => new SatResult(interpret(tr, t1) <= interpret(tr, t2), tr.start);
      case _ => println("Can't interpret " + p); new SatResult(false, 0);
    }
  }
  
  /**
   * Interpretation of a binary operator given a trace, the connective and the two formulae.
   * Returns whether it is true or false at the current state.
   */
  def interpret(tr: Trace, c: Connective, f1: Formula, f2: Formula): SatResult = {
    c match {
      case And => interpret(tr, f1) && interpret(tr, f2);
      case Or  => interpret(tr, f1) || interpret(tr, f2);
      case Imp => interpret(tr, f1) -> interpret(tr, f2);
      case Iff => interpret(tr, f1) <=> interpret(tr, f2);
    }
  }
  
  /**
   * Interpretation of a temporal operator given a trace, the connective, and the two formulae.
   * The only interpretation function that makes traces with start != 0. It does not alter
   * the original trace.
   * 
   */
  def interpret(tr: Trace, tc: TConnective, f1: Formula, f2: Formula) : SatResult = {
    if(!tr.hasNext()) {                                 // If this is the last state of the Trace
      tc match {
          // Weak until is satisfied if either it satisfied f1 until the end, or f2 was satisfied at the end.
        case WeakUntil(b: Int) => interpret(tr, f1) || interpret(tr, f2); 
          // Strong until is only satisfied if f2 is eventually satisfied
        case StrongUntil(b: Int) => interpret(tr, f2);
      }
    } else {
      
      if(Conf.CHECKPOINT_LAST_ACTION){
        f2 match {
          case Binop(And, checkpoint: Formula, TempOp(until: TConnective, f2_1, f2_2)) =>
            val checkpointSat: SatResult = interpret(tr, checkpoint);
            if(checkpointSat.sat)
              return checkpointSat && interpret(tr, until, f2_1, f2_2);
          case _ => ;
        }
      }
      
      // If this is not the last state of the trace,
      // either f2 was satisfied now
      val satNow: SatResult = interpret(tr, f2);
      if(satNow.sat) return satNow;
      // Or f1 is true and will continue to be true until f2 is.
      val satCont: SatResult = interpret(tr, f1);
      if(!satCont.sat) return satCont;
      
      return satCont && interpret(new Trace(tr).next(), tc, f1, f2);
    }
  }
   
  /**
   * Interpretation of a formula given a trace.
   * Returns whether the formula is true in this trace.
   */
  def interpret(tr: Trace, f: Formula) : SatResult = {
    f match {
      case True => new SatResult(true, tr.start);
      case False => new SatResult(false, tr. start);
      case Atom(p: Pred) => interpret(tr, p);
      case Not(f: Formula) => !interpret(tr, f);
      case Binop(c: Connective, f1 : Formula, f2: Formula) => interpret(tr, c, f1, f2);
      case TempOp(tc: TConnective, f1 : Formula, f2: Formula) => interpret(tr, tc, f1, f2);
    }
  }
  
  def traceSize(p: Pred): Int = { 0 }
  def traceSize(c: Connective, f1: Formula, f2: Formula): Int = { math.max(traceSize(f1), traceSize(f2)); }
  
  def traceSize(tc: TConnective, f1: Formula, f2: Formula) : Int = {
    tc match {
      case WeakUntil(b: Int) => b + math.max(traceSize(f1), traceSize(f2));
      case StrongUntil(b: Int) => b + math.max(traceSize(f1), traceSize(f2));
    }
  }
   
  def traceSize(f: Formula) : Int = {
    f match {
      case Not(f: Formula) => traceSize(f);
      case Binop(_, f1 : Formula, f2: Formula) => math.max(traceSize(f1), traceSize(f2));
      case TempOp(tc: TConnective, f1 : Formula, f2: Formula) => traceSize(tc, f1, f2);
      case _ => 0;
    }
  }
  
}




}
