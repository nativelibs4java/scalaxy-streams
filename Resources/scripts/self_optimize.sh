#!/bin/bash
#
# Build and test a self-optimized version of Scalaxy/Streams.
#
# To also publish: ./Resources/scripts/self_optimize.sh publish
#
set -e

# Note: these are scala expressions
readonly GROUP_AND_NAME_EXPR='"com.nativelibs4java" %% "scalaxy-streams"'
readonly OPTIMIZED_NAME_EXPR='"scalaxy-streams-experimental-self-optimized"'
readonly FAKE_VERSION_EXPR='"0-SNAPSHOT"'

# Publish a reversioned artifact locally.
sbt "set version := ${FAKE_VERSION_EXPR}" \
    clean publish-local

# Rebrand the project to allow self-dependency.
# Recompile in aggressive mode, but don't run the tests yet
# (otherwise their strategy would be forced to aggressive too).
SCALAXY_STREAMS_STRATEGY=aggressive \
SCALAXY_STREAMS_VERY_VERBOSE=1 \
sbt "set name := ${OPTIMIZED_NAME_EXPR}" \
    "set addCompilerPlugin(${GROUP_AND_NAME_EXPR} % ${FAKE_VERSION_EXPR})" \
    clean compile | 2>&1 tee self_optimization.log

# Now run the tests and any other command passed in
sbt "set name := ${OPTIMIZED_NAME_EXPR}" \
    "set addCompilerPlugin(${GROUP_AND_NAME_EXPR} % ${FAKE_VERSION_EXPR})" \
    test "$@"
