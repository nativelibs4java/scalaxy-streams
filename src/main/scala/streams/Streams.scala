package scalaxy.streams

trait Streams
    extends StreamComponents
    with UnusableSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeStream extends Extractor[Tree, Stream] {
    def findSink(ops: List[StreamComponent]): Option[StreamSink] = {
      def isAcceptableSink(sink: StreamSink, indexFromEnd: Int): Boolean = {
        val res = (sink != InvalidSink) &&
          (!sink.isFinalOnly || indexFromEnd == 0)

        // if (flags.debug && !res)
        //   println(s"Unacceptable sink $sink for list of ops $ops")
        res
      }

      // var decorator: StreamSinkDecorator = null

      // val sink = ops.reverse.toIterator.filter({
      //   case d: StreamSinkDecorator if decorator == null =>
      //     decorator = d
      //     false
      //   case _ =>
      //     true
      // }).zipWithIndex.map({
      //   case (op, i) =>
      //     (op.sinkOption, i)
      // }).collectFirst({
      //   case (Some(sink), i) if isAcceptableSink(sink, i) =>
      //     sink
      // })

      // println(s"FOUND SINK $sink, DECORATOR $decorator\n\tops = $ops")
      // sink.flatMap(s => Option(decorator).map(_.decorateSink(s)))
      
      ops.reverse.toIterator.zipWithIndex.map({
        case (op, i) =>
          (op.sinkOption, i)
      }).collectFirst({
        case (Some(sink), i) if isAcceptableSink(sink, i) =>
          sink
      })
    }

    def unapply(tree: Tree): Option[Stream] = tree match {
      case SomeStreamSink(SomeStreamOps(SomeStreamSource(source), ops), sink) =>
        Some(new Stream(tree, source, ops, sink, hasExplicitSink = true))

      case SomeStreamOps(SomeStreamSource(source), ops) =>
        findSink(source :: ops)
          .map(sink => new Stream(tree, source, ops, sink, hasExplicitSink = ops.exists(_.isSink)))

      case SomeStreamSource(source) =>
        findSink(List(source))
          .map(sink => new Stream(tree, source, Nil, sink, hasExplicitSink = false))

      case _ =>
        None
    }
  }

  case class Stream(
      tree: Tree,
      source: StreamSource,
      ops: List[StreamOp],
      sink: StreamSink,
      hasExplicitSink: Boolean)
  {
    def isDummy: Boolean =
      (ops.isEmpty || ops.forall(_.isPassThrough)) &&
      (!hasExplicitSink || sink.isJustAWrapper)

    private[this] val sourceAndOps = source :: ops

    val components: List[StreamComponent] =
      sourceAndOps :+ sink

    def describe(describeSink: Boolean = true) =
      sourceAndOps.flatMap(_.describe).mkString(".") +
      sink.describe.filter(_ => describeSink).map(" -> " + _).getOrElse("")

    def lambdaCount: Int =
      components.map(_.lambdaCount).sum
    lazy val closureSideEffectss: List[List[SideEffect]] =
      components.flatMap(_.closureSideEffectss)

    lazy val subTrees: List[Tree] =
      components.flatMap(_.subTrees)

    lazy val preservedSubTreess: List[List[Tree]] =
      components.map(_.preservedSubTrees)

    lazy val preservedSubTreesSideEffectss: List[List[SideEffect]] =
      preservedSubTreess.map(_.flatMap(analyzeSideEffects))

    private[streams] def computeOutputNeedsBackwards(sinkNeeds: Set[TuploidPath]) =
      ops.scanRight(sinkNeeds)({
        case (op, refs) =>
          op.transmitOutputNeedsBackwards(refs)
      })

    // println("FOUND STREAM: " + describe())
    // println("FOUND STREAM: " + this)

    def emitStream(fresh: String => TermName,
                   transform: Tree => Tree,
                   typed: Tree => Tree,
                   currentOwner: Symbol,
                   sinkNeeds: Set[TuploidPath] = sink.outputNeeds,
                   loopInterruptor: Option[Tree] = None): StreamOutput =
    {
      val sourceNeeds :: outputNeeds =
        computeOutputNeedsBackwards(sinkNeeds)

      val nextOps = ops.zip(outputNeeds) :+ (sink, sinkNeeds)
      // println(s"source = $source")
      // println(s"""ops =\n\t${ops.map(_.getClass.getName).mkString("\n\t")}""")
      // println(s"stream = ${describe()}")
      // println(s"outputNeeds = ${nextOps.map(_._2)}")
      source.emit(
        input = StreamInput(
          vars = UnitTreeScalarValue,
          loopInterruptor = loopInterruptor,
          fresh = fresh,
          transform = transform,
          currentOwner = currentOwner,
          typed = typed),
        outputNeeds = sourceNeeds,
        nextOps = nextOps)
    }
  }
}
