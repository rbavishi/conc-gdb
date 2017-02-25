.PHONY : all clean

BASE_DIR:=$(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

# Soot Stuff
SOOT_JAR:=${BASE_DIR}/soot-2.5.0.jar

# Java-1.7 Stuff
JAVA_1.7_PATH:=/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.75-2.5.4.2.fc20.x86_64
JAVAC:=${JAVA_1.7_PATH}/bin/javac
JRE:=${JAVA_1.7_PATH}/bin/java

# Source
SRC:=ConcGDB.java \
	 Instrumenter.java \
	 Controls.java \
	 Console.java

BIN_PATH:=${BASE_DIR}/bin
SRC_PATH:=${BASE_DIR}/src

all:
	cd ${SRC_PATH} && \
	${JAVAC} -classpath ${SOOT_JAR} -d ${BIN_PATH} ${SRC}
	
