package scalaxy.streams

private[streams] trait ScalaJsSymbols extends OptionalSymbols
{
  val global: scala.reflect.api.Universe
  import global._

  private[streams] lazy val JsArraySymOpt =
    optionalStaticClass("scala.scalajs.js.Array")

  private[streams] lazy val JsWrappedArraySymOpt =
    optionalStaticClass("scala.scalajs.js.WrappedArray")

  private[streams] lazy val JsArrayOpsSymOpt =
    optionalStaticClass("scala.scalajs.js.ArrayOps")

  private[this] lazy val JsAnyModuleOpt =
    optionalStaticModule("scala.scalajs.js.Any")

  object JsAny {
    def unapply(tree: Tree): Boolean =
      JsAnyModuleOpt.exists(_ == tree.symbol)
  }
}
