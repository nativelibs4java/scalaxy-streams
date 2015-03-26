package scalaxy.streams

// private[scalaxy] 
trait StreamTransforms
  extends Streams
  with StreamSources
  with StreamSinks
  with StreamOps
  with Strategies
  with Reporters
  with Blacklists
{
  import global._

  /**
   * Transforms a stream if it can, or returns None if it can't.
   *
   * Recurses in to stream's subTrees with recur.
   */
  def transformStream(tree: Tree,
                      strategy: OptimizationStrategy,
                      fresh: String => String,
                      currentOwner: Symbol,
                      recur: Tree => Tree,
                      typecheck: Tree => Tree): Option[Tree]
  = tree match {
    case tree @ SomeStream(stream) if !hasKnownLimitationOrBug(stream) =>
      if (isBlacklisted(tree.pos, currentOwner)) {
        if (flags.verbose) {
          info(
              tree.pos,
              Optimizations.messageHeader + s"Skipped stream ${stream.describe()}",
              force = flags.verbose)
        }
        None
      } else if (isWorthOptimizing(stream, strategy)) {
        // println(s"stream = $stream")

        info(
          tree.pos,
          Optimizations.optimizedStreamMessage(stream.describe(), strategy),
          force = flags.verbose)

        try {
          val result: Tree = stream
            .emitStream(
              n => newTermName(fresh(n)),
              recur,
              currentOwner = currentOwner,
              typed = typecheck)
            .compose(typecheck)

          if (flags.debug) {
            info(
              tree.pos,
              Optimizations.messageHeader + s"Result for ${stream.describe()} (owner: ${currentOwner.fullName}):\n$result",
              force = flags.verbose)
          }
          Some(result)

        } catch {
          case ex: Throwable =>
            logException(tree.pos, ex)
            None
        }
      } else {
        if (flags.veryVerbose && !stream.isDummy && !flags.quietWarnings) {
          info(
            tree.pos,
            Optimizations.messageHeader + s"Stream ${stream.describe()} is not worth optimizing with strategy $strategy",
            force = flags.verbose)
        }
        None
      }

    case _ =>
      None
  }
}
