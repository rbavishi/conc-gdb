#!/usr/bin/bash

BASE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Soot Stuff
SOOT_JAR=${BASE_DIR}/soot-2.5.0.jar

# Java-1.7 Stuff
JAVA_1_7_PATH=/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.75-2.5.4.2.fc20.x86_64
JAVAC=${JAVA_1_7_PATH}/bin/javac
JRE=${JAVA_1_7_PATH}/bin/java

# Conc-GDB source
BIN_PATH=${BASE_DIR}/bin
SRC_PATH=${BASE_DIR}/src

function get_ext(){
	filename=$(basename "$1")
	extension="${filename##*.}"
	echo $extension
}

function get_filename_without_ext(){
	filename=$(basename "$1")
	filename="${filename%.*}"
	echo $filename
}

if [[ "$#" != "1" ]]; then
	echo "Usage : conc-gdb.sh <java-file>"
	exit 1

elif [[ ! -f "$1" ]]; then
	echo "The file \"$1\" doesn't exist"
	exit 1

elif [[ "$(get_ext $1)" != "java" ]]; then
	echo "The file \"$1\" is not a java file"
	exit 1

fi

INP_PATH=$(dirname $(realpath "$1"))
INP_FILENAME=$(get_filename_without_ext "$1")

WORKING_DIR=.conc-gdb
SOOT_DIR=.soot-conc-gdb
LOGS_DIR=logs_conc-gdb

# The game begins!

# Create the temp directory
rm -rf ${WORKING_DIR}
rm -rf ${SOOT_DIR}
mkdir ${WORKING_DIR}
mkdir ${SOOT_DIR}
mkdir -p ${LOGS_DIR}

# Compile the inp java files
$JAVAC -g -d ${WORKING_DIR} -sourcepath ${INP_PATH} ${INP_PATH}/${INP_FILENAME}.java

# Instrument them
filelist=""
for file in ${WORKING_DIR}/*
do
	classfile=$(get_filename_without_ext $file)
	filelist="$filelist $classfile" 
done

$JRE -classpath ${BIN_PATH}:${SOOT_JAR}:${WORKING_DIR} Instrumenter -keep-line-number -d ${SOOT_DIR} $filelist

# Run the debugger
$JRE -classpath ${BIN_PATH}:${SOOT_JAR}:${SOOT_DIR} ConcGDB ${INP_FILENAME} 
