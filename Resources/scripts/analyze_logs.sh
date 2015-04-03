#!/bin/bash
#
# Just pipe compilation logs (with SCALAXY_STREAMS_VERBOSE=1) into this.
#
# SCALAXY_STREAMS_VERBOSE=1 sbt clean compile | ./Resources/scripts/analyze_logs.sh
#
set -e

grep "\[Scalaxy\] Optimized stream " | \
  sed 's/.*Optimized stream //' | sed 's/ (strategy:.*//' | \
  sort | uniq -c | sort -n
