#!/usr/bin/env sh
set -eu
: "${JAVA_HOME:=/usr/lib/jvm/java-17-openjdk}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
exec gradle clean assembleRelease
