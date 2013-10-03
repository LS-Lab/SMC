#!/bin/bash

fsc -deprecation -cp .:../bin/ Satisfaction.scala Parser.scala *.java learn/*.java modelchecking/*.java  smcmdp/*.java smcmdp/policy/*.java smcmdp/policy/update/*.java smcmdp/reward/*.java smcmdp/reward/update/*.java 
mv smcmdp/*.class ../scalabin/smcmdp/
javac -g:none -d ../bin -cp ../bin/:../scalabin/:$HOME/tools/lib/ssj.jar:$HOME/tools/scala-2.9.1.final/lib/scala-library.jar:$HOME/tools/lib/optimization.jar:$HOME/tools/lib/colt.jar *.java learn/*.java modelchecking/*.java  smcmdp/*.java smcmdp/policy/*.java smcmdp/policy/update/*.java smcmdp/reward/*.java smcmdp/reward/update/*.java 