#!/bin/sh
# Source the imports
. ./IMPORT

# Make sure to change memory given here!
JFLAGS='-Xmx10000m -Xms10000m -verbose:gc -Xloggc:gc.log -XX:+UseParallelGC -XX:+UseParallelOldGC'
CLASSPATH=$BIN:$PRISM_PATH/classes/:$SCALA_PATH/lib/scala-library.jar:$COLT_PATH/colt.jar:$SSJ_PATH/ssj.jar:$OPTIMIZATION_PATH/optimization.jar

export DYLD_LIBRARY_PATH=$PRISM_PATH/lib/
export LD_LIBRARY_PATH=$PRISM_PATH/lib/
java $JFLAGS -Djava.library.path=$PRISM_PATH/lib -cp $CLASSPATH learn.LearnMDP $@
