package scalaxy.streams

private[streams] trait StreamComponents
    extends StreamResults
    with SideEffects
{
  val global: scala.reflect.api.Universe
  import global._

  type OpsAndOutputNeeds = List[(StreamOp, OutputNeeds)]

  trait StreamComponent
  {
    def describe: Option[String]

    def sinkOption: Option[StreamSink]

    /**
     * Subtrees that are kept as-is in the rewrites.
     * For instance, a foldLeft's seed, or an mkString's separator.
     */
    def preservedSubTrees: List[Tree] = subTrees

    /**
     * Any sub-tree of the component (closures, normal preserved arguments...).
     */
    def subTrees: List[Tree]

    def lambdaCount: Int = 0

    def closureSideEffectss: List[List[SideEffect]] = Nil

    def emit(input: StreamInput,
             outputNeeds: OutputNeeds,
             nextOps: OpsAndOutputNeeds): StreamOutput

    protected def emitSub(
      input: StreamInput, 
      nextOps: OpsAndOutputNeeds,
      coercionSuccessVarRef: Option[Tree] = None): StreamOutput =
    {
      nextOps match {
        case (firstOp, outputNeeds) :: otherOpsAndOutputNeeds =>
          val sub =
            firstOp.emit(input, outputNeeds, otherOpsAndOutputNeeds)
          coercionSuccessVarRef match {
            case Some(varRef) =>
              sub.copy(body = List(q"""
                if ($varRef) {
                  ..${sub.body};
                }
              """))

            case _ =>
              sub
          }

        case Nil =>
          sys.error("Cannot call base emit at the end of an ops stream.")
      }
    }
  }

  trait StreamSource extends StreamComponent

  trait StreamOp extends StreamComponent
  {
    def canInterruptLoop: Boolean = false
    def canAlterSize: Boolean
    def isPassThrough = false
    def isSink = false

    def transmitOutputNeedsBackwards(paths: Set[TuploidPath]): Set[TuploidPath]
  }

  trait StreamSink extends StreamOp
  {
    /** Sometimes, a sink may not be emit-able because it's not implemented, but can appear in intermediate ops (as long as it's followed by an implemented sink. */
    def isImplemented = true

    /** If true, this sink is skipped unless it's at the end of the stream, i.e. after all ops. */
    def isFinalOnly: Boolean = false
    /** Sinks are "neutral" and chainable / elidable by default, except for scalar sinks. */
    def canBeElided = true
    def isJustAWrapper: Boolean = false
    override def canAlterSize = false
    override def sinkOption = Some(this)

    def outputNeeds: Set[TuploidPath] = Set(RootTuploidPath)

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = {
      val needs = outputNeeds
      require(paths.isEmpty || paths == needs)

      needs
    }

    def requireSinkInput(input: StreamInput,
                         outputNeeds: OutputNeeds,
                         nextOps: OpsAndOutputNeeds) {
      require(nextOps.isEmpty,
        "Cannot chain ops through a sink (got nextOps = " + nextOps + ")")
      require(outputNeeds == this.outputNeeds,
        "Expected outputNeeds " + this.outputNeeds + " for sink, got " + outputNeeds)
    }
  }

  trait PassThroughStreamOp extends StreamOp {

    override def isPassThrough = true

    override def describe: Option[String] = None

    override def canAlterSize = false

    override def subTrees = Nil

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) = paths

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      var (nextOp, nextOutputNeeds) :: subsequentOps = nextOps
      nextOp.emit(input, nextOutputNeeds, subsequentOps)
    }
  }

  // Allow loose coupling between sources, ops and sinks traits:
  val SomeStreamSource: Extractor[Tree, StreamSource]
  val SomeStreamOps: Extractor[Tree, (Tree, List[StreamOp])]
  val SomeStreamSink: Extractor[Tree, (Tree, StreamSink)]

  case class ExtractedStreamOp(target: Tree, op: StreamOp) {
    def isEmpty: Boolean = target == null && op == null
    def get: ExtractedStreamOp = this
    def _1: Tree = target
    def _2: StreamOp = op
  }

  lazy val NoExtractedStreamOp = ExtractedStreamOp(null, null)

  trait StreamOpExtractor {
    def unapply(tree: Tree): ExtractedStreamOp
  }

  private[streams] def printOps(ops: List[StreamOp]) {
    println(s"ops = " + ops.map(_.getClass.getSimpleName).mkString("\n\t"))
    // println(s"ops = " + ops.mkString("\n\t"))
  }
}
