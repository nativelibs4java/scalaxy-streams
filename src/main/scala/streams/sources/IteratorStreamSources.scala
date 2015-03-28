package scalaxy.streams

private[streams] trait IteratorStreamSources
    extends IteratorSinks
    with StreamInterruptors
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeIteratorStreamSource {
    def unapply(tree: Tree): Option[IteratorStreamSource] =
      Option(tree)
        .flatMap(t => Option(t.tpe))
        .filter(_ <:< typeOf[Iterator[Any]])
        .map(tpe => IteratorStreamSource(tree))
  }

  case class IteratorStreamSource(
      iterator: Tree)
    extends StreamSource
  {
    override def describe = Some("Iterator")
    override def sinkOption = Some(IteratorSink)

    override def lambdaCount = 0

    override def subTrees = List(iterator)

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ fresh, transform, typed }

      val itVal = fresh("it")
      val itemVal = fresh("item")

      // Early typing / symbolization.
      val Block(List(
          itValDef,
          itHasNext,
          itemValDef),
          itemValRef) = typed(q"""
        private[this] val $itVal = ${transform(iterator)};
        $itVal.hasNext;
        private[this] val $itemVal = $itVal.next;
        $itemVal
      """)
      val TuploidPathsExtractionDecls(extractionCode, outputVars, coercionSuccessVarDefRef) =
        createTuploidPathsExtractionDecls(
          itemValRef.tpe, itemValRef, outputNeeds, fresh, typed,
          newCoercionSuccessVarDefRef(nextOps, fresh, typed))

      val interruptor = new StreamInterruptor(input, nextOps)

      val sub = emitSub(
        input.copy(
          vars = outputVars,
          loopInterruptor = interruptor.loopInterruptor,
          outputSize = None),
        nextOps,
        coercionSuccessVarDefRef._2)
      sub.copy(
        beforeBody = Nil,
        body = List(typed(q"""
          $itValDef;
          ..${interruptor.defs}
          ..${sub.beforeBody};
          while (${interruptor.composeTest(itHasNext)}) {
            $itemValDef;
            ..$extractionCode
            ..${sub.body};
          }
          ..${sub.afterBody}
        """)),
        afterBody = Nil
      )
    }
  }
}
