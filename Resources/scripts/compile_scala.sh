#!/bin/bash
#
# ./Resources/scripts/compile_scala.sh
#
set -e

declare plugin_jar
if [[ "$#" -eq 0 ]]; then
  sbt package
  plugin_jar=`ls ${PWD}/target/scala-2.11/scalaxy-streams_2.11-*.jar`
else
  plugin_jar=$1
  shift 1
fi

declare use_plugin
if [[ -z "${plugin_jar}" ]]; then
  use_plugin=0
else
  [[ -f "${plugin_jar}" ]] || ( echo "${plugin_jar} not found" && exit 1 )
  use_plugin=1
  echo "Plugin: ${plugin_jar}"
fi

declare -a libs=( "$@" )
if [[ "${#libs[@]}" -eq 0 ]]; then
  libs+=( library reflect compiler )
fi

readonly SCALA_BRANCH=2.11.x
readonly SCALA_DIR="${HOME}/.scala-${SCALA_BRANCH}"

if [[ -d "${SCALA_DIR}" ]]; then
  cd "${SCALA_DIR}"
  git pull
else
  git clone --depth 1 --single-branch -b "${SCALA_BRANCH}" \
    git@github.com:scala/scala.git "${SCALA_DIR}"
  cd "${SCALA_DIR}"
fi

echo "Scala directory: ${SCALA_DIR}"

# Build locker
ant build locker.unlock

# Make sure we're rebuilding quick for each of the required libs:
for lib in "${libs[@]}"; do
  echo "Cleaning 'quick' artifacts of ${lib}"
  rm -fR build/quick/${lib}.complete build/quick/classes/${lib}/
done

# Build quick with the Scalaxy/Stream plugin:
# (note: there are a couple of lingering problematic rewrites that must be skipped)
declare -a exceptions=(
  InlineExceptionHandlers.scala
  Typers.scala
  LambdaLift.scala
)

if (( use_plugin )); then
  readonly SCALAC_ARGS="-Xplugin-require:scalaxy-streams -Xplugin:${plugin_jar}"

  echo "Compiling ${libs[*]} with the plugin"
  SCALAXY_STREAMS_SKIP=$(IFS=, echo "${exceptions[*]}") \
    SCALAXY_STREAMS_VERY_VERBOSE=1 \
    ant "-Dscalac.args=${SCALAC_ARGS}" \
        build | 2>&1 \
      tee ant_build_scalaxy_streams.log
else
  echo "Compiling ${libs[*]} without the plugin"
  ant build
fi

if [[ "${TEST}" == "1" ]]; then
  SCALAXY_STREAMS_VERY_VERBOSE=1 \
    ant "-Dscalac.args=${SCALAC_ARGS}" \
        "-Dpartest.scalac_opts=${SCALAC_ARGS}" \
        test | 2>&1 \
      tee ant_test_scalaxy_streams.log
fi
