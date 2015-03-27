package scalaxy.streams

private[streams] trait ScalaJsSymbols
{
  val global: scala.reflect.api.Universe
  import global._

  private[streams] lazy val JsArraySymOpt =
    scalaJsStaticClass("scala.scalajs.js.Array")

  private[streams] lazy val JsWrappedArraySymOpt =
    scalaJsStaticClass("scala.scalajs.js.WrappedArray")

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
      Option(rootMirror.staticClass(name))
    } catch {
      case ex: Throwable =>
        if (flags.debug)
          println(s"Failed to get Scala.js class $name: $ex")
        None
    }
  }
  def scalaJsStaticModule(name: String): Option[Symbol] = {
    try {
      Option(rootMirror.staticModule(name))
    } catch {
      case ex: Throwable =>
        if (flags.debug)
          println(s"Failed to get Scala.js module $name: $ex")
        None
    }
  }
}
