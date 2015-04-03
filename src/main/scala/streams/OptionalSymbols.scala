package scalaxy.streams

private[streams] trait OptionalSymbols
{
  val global: scala.reflect.api.Universe
  import global._

  def optionalStaticClass(name: String): Option[Symbol] = {
    try {
      Option(rootMirror.staticClass(name))
    } catch {
      case ex: Throwable =>
        if (flags.debug)
          println(s"Failed to get optional class $name: $ex")
        None
    }
  }
  def optionalStaticModule(name: String): Option[Symbol] = {
    try {
      Option(rootMirror.staticModule(name))
    } catch {
      case ex: Throwable =>
        if (flags.debug)
          println(s"Failed to get optional module $name: $ex")
        None
    }
  }
}
