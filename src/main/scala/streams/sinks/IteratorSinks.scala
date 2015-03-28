package scalaxy.streams

private[streams] trait IteratorSinks
    extends StreamComponents
    with UnusableSinks
{
  val global: scala.reflect.api.Universe
  import global._

  case object IteratorSink extends StreamSink
  {
    // Implementing an iterator sink is *tricky* and will require a new emission
    // mode for sources. Tracked by issue nativelibs4java/scalaxy-streams#3.
    override def isImplemented = false

    override def lambdaCount = 0

    override def subTrees = Nil

    override def describe = Some("Iterator")

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input._

      ???
      // TODO(ochafik): Add a new emission mode for sources / invert responsibilities.
      //
      // requireSinkInput(input, outputNeeds, nextOps)
      //
      // val next = fresh("next")
      // val hasNext = fresh("hasNext")
      // val computedHasNext = fresh("computedHasNext")
      // require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")
      //
      // val Block(List(...), ...) = typed(q"""
      //   new Iterator[Int] {
      //     private[this] var $next: ${input.vars.tpe} = _
      //     private[this] var $hasNext: Boolean = _
      //     private[this] var $computedHasNext: Boolean = _
      //     override def hasNext = {
      //       if (!$computedHasNext) {
      //         $hasNext = false
      //         $computedHasNext = true
      //         if (true /* interruptor */) {
      //           $next = ${input.vars.alias}
      //           $hasNext = true
      //         }
      //       }
      //       $hasNext
      //     }
      //     override def next = {
      //       if (!hasNext)
      //         throw new java.util.NoSuchElementException("next on empty iterator")
      //       $computedHasNext = false
      //       $next
      //     }
      //   }
      // """)
      // StreamOutput(
      //   prelude = List(...),
      //   body = List(...),
      //   ending = List(...))
    }
  }
}
