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
    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.toIterator" =>
        (target, ToIteratorOp)

      case q"$target.toList" =>
        (target, ToListOp)

      case q"$target.toVector" =>
        (target, ToVectorOp)

      case q"$target.toArray[${_}]($classTag)" =>
        (target, ToArrayOp(classTag))
    }
  }

  class ToCollectionOp(name: String, sink: StreamSink) extends PassThroughStreamOp {
    override def describe = Some(name)
    override def sinkOption = Some(sink)
    override def canAlterSize = false
  }

  case object ToListOp extends ToCollectionOp("toList", ListBufferSink)

  case class ToArrayOp(classTag: Tree) extends ToCollectionOp("toArray", ArrayBuilderSink(Some(classTag)))

  case object ToVectorOp extends ToCollectionOp("toVector", VectorBuilderSink)

  case object ToIteratorOp extends ToCollectionOp("toIterator", IteratorSink)
}
