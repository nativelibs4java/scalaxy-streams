package scalaxy.streams

private[streams] trait ScalaJsSymbols
{
  val global: scala.reflect.api.Universe
  import global._

  private[streams] lazy val JsArraySymOpt =
    scalaJsStaticClass("scala.scalajs.js.Array")

  private[streams] lazy val JsArrayOpsSymOpt =
    scalaJsStaticClass("scala.scalajs.js.ArrayOps")

  private[this] lazy val JsAnyModuleOpt =
    scalaJsStaticModule("scala.scalajs.js.Any")

  object JsAny {
    def unapply(tree: Tree): Boolean =
      JsAnyModuleOpt.exists(_ == tree.symbol)
  }

  def scalaJsStaticClass(name: String): Option[Symbol] = {
    try {
      val c = Option(rootMirror.staticClass(name))
      println(s"scalaJsStaticClass($name) = $c")
      c
    } catch {
      case ex: Throwable =>
        if (flags.debug)
          println(s"Failed to get Scala.js class $name: $ex")
        None
    }
  }
  def scalaJsStaticModule(name: String): Option[Symbol] = {
    try {
      val c = Option(rootMirror.staticModule(name))
      println(s"scalaJsStaticModule($name) = $c")
      c
    } catch {
      case ex: Throwable =>
        if (flags.debug)
          println(s"Failed to get Scala.js module $name: $ex")
        None
    }
  }
}
