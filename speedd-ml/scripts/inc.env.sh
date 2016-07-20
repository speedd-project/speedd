#!/usr/bin/env bash

PRJ_COMMONS_CLASSPATH="."

for curr_lib in `find ${lib_dir} -name "*.jar"`
do
  PRJ_COMMONS_CLASSPATH=${PRJ_COMMONS_CLASSPATH}:${curr_lib}
done

SCALA_LIBS=""

TERMINAL_WITH=$(tput cols)

ETC_DIR="$base_dir/etc"

# JVM options:
VM_ARGS=" -XX:+DoEscapeAnalysis -XX:+UseFastAccessorMethods -XX:+OptimizeStringConcat "