package scalaxy.streams

private[streams] trait StreamSinks
    extends StreamComponents
    with ArrayBuilderSinks
    with IteratorSinks
    with ListBufferSinks
    with SetBuilderSinks
    with VectorBuilderSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStreamSink extends Extractor[Tree, (Tree, StreamSink)] {
    def unapply(tree: Tree): Option[(Tree, StreamSink)] = Option(tree) collect {
      case q"$target.toList" =>
        (target, ListBufferSink)

      case q"$target.toIterator" =>
        (target, IteratorSink)

      case q"$target.toArray[${_}](${_})" =>
        (target, ArrayBuilderSink)

      case q"$target.toSet[${_}]" =>
        (target, SetBuilderSink)

      case q"$target.toVector" =>
        (target, VectorBuilderSink)
    }
  }
}
