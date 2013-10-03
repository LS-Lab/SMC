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

public class MultiMutex {

  //Random rand = new Random();	
	
  static String createmutexfile(int N) {
    String S = "mdp\n\n";
    for (int i = 1; i < N + 1; i++)
      S += createmodule(i, N);

    return S;
  }

  static String createmodule(int i, int N) {
	double e = Math.random()/100;
	double d = Math.random()/100;
    String S = "module M" + i + "\n\n";
    S += "x" + i + " : [0..2] init 0;\n\n";

    S += "[] x" + i + "=0 -> "+ (0.8+e) +": (x" + i + "'=0)+"+ (0.2-e) +":(x" + i + "'=1);\n";

    S += "[] x" + i + "=1 ";
    for (int j = 1; j < N + 1; j++) {
      S += "& x" + j + "!=2 ";
    }
    S += "-> (x" + i + "'=2);\n";

    S += "[] x" + i + "=2 -> "+ (0.5+d) +": (x" + i + "'=2)+"+ (0.5-d) +":(x" + i + "'=0);\n\n";

    S += "endmodule\n\n";

    return S;
  }
  
  static String createpctlfile(int N, int bound) {
    String S = "Pmax=?[F<="+bound+" (false";

    for (int i = 1; i < N + 1; i++){
      for(int j=i+1; j<N+1; j++){
        S+=" | (x"+i+"=2 & x"+j+"=2)";
      }
    }
    
    S+=")]";

    return S;
  }
  
  private static String createpropertyfile(int n, int bound) {
    String prop = "true SU("+bound+") ";
    
    for(int i = 1; i < n; i++){
      prop += "I(x"+i+" = 2) + ";
    }
    
    prop += "I(x"+n+" = 2) > 1";
    
    return prop;
  }
  
  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: java MultiMutex <m|p|pp> <number of threads>");
      System.exit(1);
    }
    
    String todo = args[0];
    int n = Integer.parseInt(args[1]);
    
    System.out.println(n);
    System.out.println(todo);
    
    if(todo.equals("m"))
      System.out.println(createmutexfile(n));
    else if(todo.equals("p"))
      System.out.println(createpropertyfile(n, Integer.parseInt(args[2])));
    else if(todo.equals("pp"))
      System.out.println(createpctlfile(n, Integer.parseInt(args[2])));
    else
      System.out.println("WHAT DO YOU WANT FROM ME!?");
    
    
  }
}