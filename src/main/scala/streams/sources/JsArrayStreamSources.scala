package scalaxy.streams

private[streams] trait JsArrayStreamSources
    extends ArrayStreamSources
    with JsArrayBuilderSinks
    with StreamInterruptors
    with ScalaJsSymbols
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeJsArrayStreamSource {
    private[this] def isJsArrayType(tree: Tree) =
      JsArraySymOpt.exists(arr => findType(tree).exists(_.typeSymbol == arr))

    def unapply(tree: Tree): Option[ArrayStreamSource] =
      Option(tree)
        .filter(isJsArrayType)
        .map(array => ArrayStreamSource(
          array,
          describe = Some("js.Array"),
          sinkOption = Some(JsArrayBuilderSink)
        ))
  }
}
