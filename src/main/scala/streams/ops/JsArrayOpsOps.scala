package scalaxy.streams

private[streams] trait JsArrayOpsOps
    extends StreamComponents
    with ScalaJsSymbols
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeJsArrayOp {
    def isJsArrayOpType(tpe: Type): Boolean =
      Option(tpe).map(_.typeSymbol).exists(JsArrayOpsSymOpt.contains(_))

    def unapply(tree: Tree): Option[Tree] =
      Option(tree).filter(_ => isJsArrayOpType(tree.tpe)) collect {
      case Apply(TypeApply(Select(JsAny(), N("jsArrayOps")), List(_)), List(array)) =>
        array

      case Apply(Select(New(_), termNames.CONSTRUCTOR), List(array)) =>
        array
    }
  }

  object SomeJsArrayOpsOp extends StreamOpExtractor {
    override def unapply(tree: Tree): Option[(Tree, StreamOp)] =
      SomeJsArrayOp.unapply(tree).map(array => (array, JsArrayOpsOp))
  }

  case object JsArrayOpsOp extends PassThroughStreamOp {
    // No need to wrap js.Arrays in sink as ops as of Arrays?
    override val sinkOption = None
  }

}
