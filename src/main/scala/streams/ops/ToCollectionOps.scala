package scalaxy.streams

private[streams] trait ToCollectionOps
    extends StreamComponents
    with ArrayBuilderSinks
    with IteratorSinks
    with ListBufferSinks
    with VectorBuilderSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeToCollectionOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = tree match {
      case q"$target.toIterator" =>
        ExtractedStreamOp(target, ToIteratorOp)

      case q"$target.toList" =>
        ExtractedStreamOp(target, ToListOp)

      case q"$target.toVector" =>
        ExtractedStreamOp(target, ToVectorOp)

      case q"$target.toArray[${_}](${_})" =>
        ExtractedStreamOp(target, ToArrayOp)

      case _ =>
        NoExtractedStreamOp
    }
  }

  class ToCollectionOp(name: String, sink: StreamSink) extends PassThroughStreamOp {
    override def describe = Some(name)
    override def sinkOption = Some(sink)
    override def canAlterSize = false
  }

  case object ToListOp extends ToCollectionOp("toList", ListBufferSink)

  case object ToArrayOp extends ToCollectionOp("toArray", ArrayBuilderSink)

  case object ToVectorOp extends ToCollectionOp("toVector", VectorBuilderSink)

  case object ToIteratorOp extends ToCollectionOp("toIterator", IteratorSink)
}
