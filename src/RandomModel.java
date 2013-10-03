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

import java.util.*;

public class RandomModel {

  public static Random rand = new Random(); 

  //i suponho que isto fosse originalmente servir para gerar mais modulos
  // M num de vars
  // N dominio das vars
  // L num de transicoes
  static String createmodule(int i, int M, int N, int L) {
  String S = "mdp\n\n";
  S += "module M1\n\n";
  for (int j = 1; j < M + 1; j++) {
    S += "x" + j + " : [0.."+ N +"] init 0;\n";
  }
  S += "\n";
  for (int j = 1; j < M + 1; j++) {
    S += "[] (x" + j + ">0) & (x"+ j +"<"+ N +") -> 0.5 : (x"+ j +"' = x"+ j + "+1) + 0.5 : (x"+ j +"' = x"+ j + "-1);\n";
    S += "[] (x" + j + "=0) -> (x"+ j +"' = x"+ j +"+1);\n";
    S += "[] (x" + j + "=" + N +") -> (x"+ j +"' = x"+ j +"-1);\n";
  }
  
  
  for (int j = 1; j < L + 1; j++) {
    int guardvar = rand.nextInt(M) +1;
    int guardnum = rand.nextInt(N) +1;
    int destvar1 = rand.nextInt(M) +1;
    int destnum1 = rand.nextInt(N) +1;
    int destvar2 = rand.nextInt(M) +1;
    int destnum2 = rand.nextInt(N) +1;
    double p = Math.random();
    S += "[] (x" + guardvar + " > "+ guardnum +") -> "+ p + " :(x"+ destvar1 +"'="+ destnum1 +") + "+ (1-p) +" :(x"+ destvar2 +"'="+ destnum2 +");\n";
    destvar1 = rand.nextInt(M) +1;
    destnum1 = rand.nextInt(N) +1;
    destvar2 = rand.nextInt(M) +1;
    destnum2 = rand.nextInt(N) +1;
    S += "[] (x" + guardvar + " <= "+ guardnum +") -> "+ p + " :(x"+ destvar1 +"'="+ destnum1 +") + "+ (1-p) +" :(x"+ destvar2 +"'="+ destnum2 +");\n";
  }

    S += "\nendmodule\n\n";
    return S;
  }
  
  static double[] distribution(int R){
    double[] rands = new double[R];
    double[] probs = new double[R];
    for(int a = 0; a < R - 1; ++a) rands[a] = rand.nextDouble();
    rands[R-1] = 1;
    Arrays.sort(rands);
    probs[0] = rands[0];
    for(int a = 1; a < R; ++a) probs[a] = rands[a] - rands[a-1];
    return probs;
  }
  
  
  static String createmodule(int i, int M, int N, int L, int R) {
    String S = "mdp\n\n";
    S += "module M1\n\n";
    for (int j = 1; j < M + 1; j++) {
      S += "x" + j + " : [0.."+ N +"] init 0;\n";
    }
    S += "\n";
    for (int j = 1; j < M + 1; j++) {
      S += "[] (x" + j + ">0) & (x"+ j +"<"+ N +") -> 0.5 : (x"+ j +"' = x"+ j + "+1) + 0.5 : (x"+ j +"' = x"+ j + "-1);\n";
      S += "[] (x" + j + "=0) -> (x"+ j +"' = x"+ j +"+1);\n";
      S += "[] (x" + j + "=" + N +") -> (x"+ j +"' = x"+ j +"-1);\n";
    }
    
    
    for (int j = 1; j < L + 1; j++) {
      double[] probs = distribution(R);
      
      int guardvar = rand.nextInt(M) +1;
      int guardnum = rand.nextInt(N) +1;
      S += "[] (x" + guardvar + " > "+ guardnum +") -> ";
      
      for(int a = 0; a < R; ++a) {
        int destvar1 = rand.nextInt(M) +1;
        int destnum1 = rand.nextInt(N) +1;
        S += probs[a] + " : (x"+ destvar1 +"'="+ destnum1 +")";
        if(a < R-1) S += " + ";
        else        S += ";\n";
      }
      
      probs = distribution(R);
      S += "[] (x" + guardvar + " <= "+ guardnum +") -> ";
      
      for(int a = 0; a < R; ++a) {
        int destvar1 = rand.nextInt(M) +1;
        int destnum1 = rand.nextInt(N) +1;
        S += probs[a] + " : (x"+ destvar1 +"'="+ destnum1 +")";
        if(a < R-1) S += " + ";
        else        S += ";\n";
      }
      
    }

    S += "\nendmodule\n\n";
    return S;
  }
  
  static double[] distribution(int R, double epsilon){
    double[] rands = new double[R];
    double[] probs = new double[R];
    for(int a = 1; a < R - 1; ++a) rands[a] = epsilon + (rand.nextDouble() * ( 1 - epsilon));
    rands[0] = epsilon;
    rands[R-1] = 1;
    Arrays.sort(rands);
    probs[0] = rands[0];
    for(int a = 1; a < R; ++a) probs[a] = rands[a] - rands[a-1];
    
    //double lol = 0;
    //for(int a = 0; a < R; ++a) lol += probs[a];
    //System.out.println("HAH " + lol);
    return probs;
  }
  
  static String createmodule(int i, int M, int N, int L, int R, double epsilon) {
    String S = "mdp\n\n";
    S += "module M1\n\n";
    for (int j = 1; j < M + 1; j++) {
      S += "x" + j + " : [0.."+ N +"] init 0;\n";
    }
    S += "\n";
    for (int j = 1; j < M + 1; j++) {
      S += "[] (x" + j + ">0) & (x"+ j +"<"+ N +") -> 0.5 : (x"+ j +"' = x"+ j + "+1) + 0.5 : (x"+ j +"' = x"+ j + "-1);\n";
      S += "[] (x" + j + "=0) -> (x"+ j +"' = x"+ j +"+1);\n";
      S += "[] (x" + j + "=" + N +") -> (x"+ j +"' = x"+ j +"-1);\n";
    }
    
    
    for (int j = 1; j < L + 1; j++) {
      double[] probs = distribution(R, epsilon);
      
      int guardvar = rand.nextInt(M) +1;
      int guardnum = rand.nextInt(N) +1;
      S += "[] (x" + guardvar + " > "+ guardnum +") -> ";
      
      for(int a = 0; a < R; ++a) {
        int destvar1 = rand.nextInt(M) +1;
        int destnum1 = rand.nextInt(N) +1;
        S += probs[a] + " : (x"+ destvar1 +"'="+ destnum1 +")";
        if(a < R-1) S += " + ";
        else        S += ";\n";
      }
      
      probs = distribution(R, epsilon);
      S += "[] (x" + guardvar + " <= "+ guardnum +") -> ";
      
      for(int a = 0; a < R; ++a) {
        int destvar1 = rand.nextInt(M) +1;
        int destnum1 = rand.nextInt(N) +1;
        S += probs[a] + " : (x"+ destvar1 +"'="+ destnum1 +")";
        if(a < R-1) S += " + ";
        else        S += ";\n";
      }
      
    }

    S += "\nendmodule\n\n";
    return S;
  }
  
  public static void main(String[] args) {
    if (args.length < 4) {
      System.err.println("Need 4 parameters...");
      System.exit(1);
    }
    
    //System.out.println(createmodule(1,7,20,5));
    if(args.length < 5) {
      System.out.println(createmodule(Integer.parseInt(args[0]),
                                      Integer.parseInt(args[1]),
                                      Integer.parseInt(args[2]),
                                      Integer.parseInt(args[3])));
    } else if(args.length < 6) {
      System.out.println(createmodule(Integer.parseInt(args[0]),
                                      Integer.parseInt(args[1]),
                                      Integer.parseInt(args[2]),
                                      Integer.parseInt(args[3]),
                                      Integer.parseInt(args[4])));
    } else if(args.length < 7) {
      System.out.println(createmodule(Integer.parseInt(args[0]),
                                      Integer.parseInt(args[1]),
                                      Integer.parseInt(args[2]),
                                      Integer.parseInt(args[3]),
                                      Integer.parseInt(args[4]),
                                      Double.parseDouble(args[5])));
}
  }
}