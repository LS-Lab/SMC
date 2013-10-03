# Include paths
include IMPORT

JFLAGS=-g:none
JC=javac

SFLAGS=-deprecation
SC=fsc

SCALA_FILES=$(SRC)/Satisfaction.scala $(SRC)/Parser.scala
JAVA_FILES=$(SRC)/*.java $(SRC)/learn/*.java $(SRC)/modelchecking/*.java $(SRC)/smcmdp/*.java $(SRC)/smcmdp/policy/*.java $(SRC)/smcmdp/policy/update/*.java $(SRC)/smcmdp/reward/*.java $(SRC)/smcmdp/reward/update/*.java 


all: bindir parser
	$(JC) $(JFLAGS) -d $(BIN) \
	   -cp $(BIN):$(PRISM_PATH)/classes/:$(SSJ_PATH)/ssj.jar:$(SCALA_PATH)/lib/scala-library.jar:$(OPTIMIZATION_PATH)/optimization.jar:$(COLT_PATH)/colt.jar \
	   $(JAVA_FILES)

parser: bindir
	$(SC) $(SFLAGS) -d $(BIN) -classpath $(PRISM_PATH)/classes/ $(SCALA_FILES) $(JAVA_FILES)

bindir: 
	mkdir -p $(BIN)


.PHONY : clean parser bindir 

clean: 
	rm -rf $(BIN)