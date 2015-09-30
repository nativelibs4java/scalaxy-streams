package scalaxy.streams

private[streams] trait JsArrayOpsOps
    extends StreamComponents
    with ScalaJsSymbols
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeJsArrayOp {
    def isJsArrayOpType(tpe: Type): Boolean =
      tpe != null && JsArrayOpsSymOpt.contains(tpe.typeSymbol)

    def unapply(tree: Tree): Tree = {
      val tpe = tree.tpe
      if (isJsArrayOpType(tree.tpe)) {
        tree match {
          case Apply(TypeApply(Select(JsAny(), N("jsArrayOps")), List(_)), List(array)) =>
            array

          case Apply(Select(New(_), termNames.CONSTRUCTOR), List(array)) =>
            array

          case _ =>
            EmptyTree
        }
      } else {
        EmptyTree
      }
    }
  }

  object SomeJsArrayOpsOp extends StreamOpExtractor {
    override def unapply(tree: Tree): ExtractedStreamOp =
      SomeJsArrayOp.unapply(tree) match {
        case EmptyTree =>
          NoExtractedStreamOp

        case array =>
          ExtractedStreamOp(array, JsArrayOpsOp)
      }
  }

  case object JsArrayOpsOp extends PassThroughStreamOp {
    // No need to wrap js.Arrays in sink as ops as of Arrays?
    override val sinkOption = None
  }

}
