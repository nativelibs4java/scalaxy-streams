package scalaxy.streams

object flags
{
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
    System.getenv("SCALAXY_STREAMS_VERBOSE") == "1" ||
    System.getProperty("scalaxy.streams.verbose") == "true"

  private[streams] var disabled: Boolean =
    System.getenv("SCALAXY_STREAMS_OPTIMIZE") == "0" ||
    System.getProperty("scalaxy.streams.optimize") == "false"

  private[streams] var strategy: Option[String] =
    Option(System.getenv("SCALAXY_STREAMS_STRATEGY"))
      .orElse(Option(System.getProperty("scalaxy.streams.strategy")))

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
