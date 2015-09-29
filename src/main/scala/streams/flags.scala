package scalaxy.streams

private[streams] object LogLevel extends Enumeration {
  type LogLevel = Value
  val Quiet, Normal, Verbose, VeryVerbose, Debug = Value
}

object flags
{
  private[this] def isEnv(propName: String, envName: String, enabled: Boolean = true): Boolean = {
    var env = System.getenv("SCALAXY_STREAMS_" + envName)
    var prop = System.getProperty("scalaxy.streams." + propName)

    env == (if (enabled) "1" else "0") ||
    prop == enabled.toString()
  }

  import LogLevel._

  private[streams] var logLevel: LogLevel = {
    if (isEnv("debug", "DEBUG")) Debug
    else if (isEnv("veryVerbose", "VERY_VERBOSE")) VeryVerbose
    else if (isEnv("verbose", "VERBOSE")) Verbose
    else if (isEnv("quiet", "QUIET")) Quiet
    else Normal
  }

  private[streams] def verbose: Boolean = logLevel >= Verbose
  private[streams] def veryVerbose: Boolean = logLevel >= VeryVerbose
  private[streams] def debug: Boolean = logLevel >= Debug
  private[streams] def quiet: Boolean = logLevel == Quiet

  private[streams] var experimental: Boolean =
    isEnv("experimental", "EXPERIMENTAL")

  private[streams] var disabled: Boolean =
    isEnv("optimize", "OPTIMIZE", false)

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
