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

import scala.util.parsing.combinator.lexical._
import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.combinator._

package smcmdp {

/**
 * Class that reads formulae from a String and builds an AST.
 */
object Parser extends JavaTokenParsers {
  /**
   * What you want to call :)
   */
  def parseFormula(s: String) : Formula = {
    parseAll(formula, s) match {
      case Success(result : Formula, _) => result;
      case NoSuccess(msg: String, _) => println(msg); null;
    }
  }
  
  def term: Parser[Term] = 
     prod*("+" ^^^ {(x:Term, y:Term) => Fn("+", x, y)} 
         | "-" ^^^ {(x: Term, y: Term) => Fn("-", x, y)}) 
     
   def prod: Parser[Term] =
     factor*("*" ^^^ {(x: Term, y: Term) 
                             => Fn("*", x, y)}  
             | "/" ^^^ {(x: Term, y: Term) 
                                =>  Fn("/", x, y)}) | 
     "-" ~> prod ^^ { x => Fn("-", Num(0), x)} 

   def factor: Parser[Term] = 
      atomicTerm ~ "^" ~ floatingPointNumber ^^ 
             {case x ~ "^" ~ y => 
                  Fn("^", x, Num(y.toDouble))} |
      atomicTerm

   def atomicTerm : Parser[Term] = 
     "I(" ~> formula <~ ")" ^^ {(f: Formula) => Form(f)} | 
      "(" ~> term <~  ")" | 
     floatingPointNumber ^^ (x => Num(x.toDouble)) |  
     ident ^^ (x => Var(x))


   def pred : Parser[Pred] = 
     term ~ ("<=" | ">=" | "=" | "/=" | "<" | ">") ~ term ^^
       { case t1 ~ r ~ t2 =>
          R(r, List(t1,t2))}

   def formula : Parser[Formula] = 
     ((formula0 <~ "WU(") ~ """[0-9]+""".r <~ ")") ~ formula0 ^^ {case f1 ~ n ~ f2 => TempOp(WeakUntil(n.toInt), f1, f2)} |
     ((formula0 <~ "SU(") ~ """[0-9]+""".r <~ ")") ~ formula0 ^^ {case f1 ~ n ~ f2 => TempOp(StrongUntil(n.toInt), f1, f2)} |
     formula0
     
      
   def formula0 : Parser[Formula] = 
     formula1*( "<=>" ^^^ {(f1:Formula,f2:Formula) => Binop(Iff,f1,f2)})

   // Implication is right-associative.
   def formula1 : Parser[Formula] = 
      rep1sep(formula2, "==>") ^^ 
        ((lst) => lst.reduceRight((f1:Formula,f2:Formula) => Binop(Imp,f1,f2)))

   def formula2 : Parser[Formula] = 
     formula3*( "|" ^^^ {(f1:Formula,f2:Formula) => Binop(Or,f1,f2)})

   def formula3 : Parser[Formula] = 
     formula4*( "&" ^^^ {(f1:Formula,f2:Formula) => Binop(And,f1,f2)})

   def formula4 : Parser[Formula] = 
     "~" ~> formula5 ^^ {fm => Not(fm)} | 
     formula5

   def formula5 : Parser[Formula] = 
     "(" ~> formula <~  ")" | 
     pred ^^ (x => Atom(x))  |
     "true" ^^^ True |
     "false" ^^^ False
}
}