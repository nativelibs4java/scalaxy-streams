package scalaxy.streams

private[streams] trait MapOps
    extends ClosureStreamOps
    with CanBuildFromSinks
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeMapOp extends StreamOpExtractor {
    override def unapply(tree: Tree) = tree match {
      case q"$target.map[${_}, ${_}](${Closure(closure)})($canBuildFrom)" =>
        ExtractedStreamOp(target, MapOp(closure, canBuildFrom = Some(canBuildFrom)))

      // Option.map and Iterator.map don't take a CanBuildFrom.
      case q"$target.map[${_}](${Closure(closure)})" =>
        ExtractedStreamOp(target, MapOp(closure, canBuildFrom = None))

      case _ =>
        NoExtractedStreamOp
    }
  }

  case class MapOp(closure: Function, canBuildFrom: Option[Tree])
      extends ClosureStreamOp
  {
    override def describe = Some("map")

    override val sinkOption = canBuildFrom.map(CanBuildFromSink(_))

    override def canAlterSize = false

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      val (replacedStatements, outputVars) =
        transformationClosure.replaceClosureBody(input, outputNeeds)

      val sub = emitSub(input.copy(vars = outputVars, elementClassTag = None), nextOps)
      sub.copy(body = replacedStatements ++ sub.body)
    }
  }
}
