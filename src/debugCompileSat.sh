fsc -g:vars -deprecation -cp .:../bin Satisfaction.scala Parser.scala *.java learn/*.java modelchecking/*.java  smcmdp/*.java smcmdp/policy/*.java smcmdp/policy/update/*.java smcmdp/reward/*.java smcmdp/reward/update/*.java 
mv smcmdp/*.class ../bin/smcmdp/
javac -g -d ../bin -cp ../bin/:/home/anvilfolk/tools/scala-2.9.1.final/lib/scala-library.jar:/home/anvilfolk/tools/lib/ssj.jar *.java learn/*.java modelchecking/*.java  smcmdp/*.java smcmdp/policy/*.java smcmdp/policy/update/*.java smcmdp/reward/*.java smcmdp/reward/update/*.java 
