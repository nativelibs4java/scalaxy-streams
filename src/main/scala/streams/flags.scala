package scalaxy.streams

object flags
{
  private[streams] var experimental: Boolean =
    System.getenv("SCALAXY_STREAMS_EXPERIMENTAL") == "1" ||
    System.getProperty("scalaxy.streams.experimental") == "true"

  private[streams] var debug: Boolean =
    System.getenv("SCALAXY_STREAMS_DEBUG") == "1" ||
    System.getProperty("scalaxy.streams.debug") == "true"

  private[streams] var veryVerbose: Boolean =
    debug ||
    System.getenv("SCALAXY_STREAMS_VERY_VERBOSE") == "1" ||
    System.getProperty("scalaxy.streams.veryVerbose") == "true"

  // TODO: optimize this (trait).
  private[streams] var verbose: Boolean =
    veryVerbose ||
    System.getenv("SCALAXY_STREAMS_VERBOSE") != "0" &&
    System.getProperty("scalaxy.streams.verbose") != "false"

  private[streams] var disabled: Boolean =
    System.getenv("SCALAXY_STREAMS_OPTIMIZE") == "0" ||
    System.getProperty("scalaxy.streams.optimize") == "false"

  private[streams] var strategy: Option[OptimizationStrategy] =
    Option(System.getenv("SCALAXY_STREAMS_STRATEGY"))
      .orElse(Option(System.getProperty("scalaxy.streams.strategy")))
      .flatMap(scalaxy.streams.strategy.forName)

  /** For testing */
  private[streams] var quietWarnings = false

  private[streams] def withQuietWarnings[A](a: => A): A = {
    val old = quietWarnings
    try {
      quietWarnings = true
      a
    } finally {
      quietWarnings = old
    }
  }
}
