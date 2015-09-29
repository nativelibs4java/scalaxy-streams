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

  private[this] def verboseInfo(tree: Tree, msg: => String) {
    if (!flags.quiet) {
      info(tree.pos, msg, force = flags.verbose)
    }
  }

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
        verboseInfo(
          tree,
          Optimizations.messageHeader + s"Skipped stream ${stream.describe()}")

        None
      } else if (isWorthOptimizing(stream, strategy)) {
        // println(s"stream = $stream")
        verboseInfo(
          tree,
          Optimizations.optimizedStreamMessage(stream.describe(), strategy))

        try {
          val result: Tree = stream
            .emitStream(
              n => TermName(fresh(n)),
              recur,
              currentOwner = currentOwner,
              typed = typecheck)
            .compose(typecheck)

          if (flags.debug) {
            verboseInfo(
              tree,
              Optimizations.messageHeader + s"Result for ${stream.describe()} (owner: ${currentOwner.fullName}):\n$result")
          }
          Some(result)

        } catch {
          case ex: Throwable =>
            logException(tree.pos, ex)
            None
        }
      } else {
        if (flags.veryVerbose && !stream.isDummy && !flags.quietWarnings) {
          verboseInfo(
            tree,
            Optimizations.messageHeader + s"Stream ${stream.describe()} is not worth optimizing with strategy $strategy")
        }
        None
      }

    case _ =>
      None
  }
}
