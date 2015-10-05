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
            "doubleArrayOps" |
            // These now have a _ prefix in 2.12.0-M2:
            "_intArrayOps" |
            "_longArrayOps" |
            "_byteArrayOps" |
            "_shortArrayOps" |
            "_charArrayOps" |
            "_booleanArrayOps" |
            "_floatArrayOps" |
            "_doubleArrayOps")) |
          TypeApply(
            Select(Predef(), N(
              "refArrayOps" |
              "_refArrayOps" |
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
