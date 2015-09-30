package scalaxy.streams

private[streams] trait ArrayOpsOps
    extends StreamComponents
    with ArrayOpsSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeArrayOp {
    def unapply(tree: Tree): Tree = tree match {
      case Apply(
          Select(Predef(), N(
            "intArrayOps" |
            "longArrayOps" |
            "byteArrayOps" |
            "shortArrayOps" |
            "charArrayOps" |
            "booleanArrayOps" |
            "floatArrayOps" |
            "doubleArrayOps")) |
          TypeApply(
            Select(Predef(), N(
              "refArrayOps" |
              "genericArrayOps")),
            List(_)),
          List(array)) =>
        array

      case _ =>
        EmptyTree
    }
  }

  object SomeArrayOpsOp extends StreamOpExtractor {
    override def unapply(tree: Tree) =
      SomeArrayOp.unapply(tree) match {
        case EmptyTree =>
          NoExtractedStreamOp

        case array =>
          ExtractedStreamOp(array, ArrayOpsOp)
      }
  }

  case object ArrayOpsOp extends PassThroughStreamOp {
    override val sinkOption = Some(ArrayOpsSink)
  }

}
