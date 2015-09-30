package scalaxy.streams

private[streams] trait VectorStreamSources
    extends VectorBuilderSinks
    with StreamInterruptors
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeVectorStreamSource
      extends TypeBasedExtractor[VectorStreamSource] {
    override lazy val tpe = {
      var s = rootMirror.staticClass("scala.collection.immutable.Vector")
      internal.typeRef(s.asType.toType, s, List(WildcardType))
    }
    override def result(tree: Tree) = VectorStreamSource(tree)
  }

  case class VectorStreamSource(vector: Tree)
    extends StreamSource
  {
    override def describe = Some("Vector")
    override def sinkOption = Some(VectorBuilderSink)
    override def lambdaCount = 0

    override def subTrees = List(vector)

    override def emit(input: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input.{ fresh, transform, typed }

      val vectorVal = fresh("vector")
      val lengthVal = fresh("length")
      val iVar = fresh("i")
      val itemVal = fresh("item")

      val tvector = if (Option(vector.tpe).exists(_ != NoType)) vector else typed(vector)
      val vectorTpe = findType(tvector).get

      // Early typing / symbolization.
      val Block(List(
          vectorValDef,
          lengthValDef,
          iVarDef,
          itemValDef),
          TupleCreation(List(
            vectorValRef, lengthValRef, iVarRef, itemValRef))) = typed(q"""
        private[this] val $vectorVal: $vectorTpe = ${transform(tvector)};
        private[this] val $lengthVal = $vectorVal.length;
        private[this] var $iVar = 0;
        private[this] val $itemVal = $vectorVal($iVar);
        ($vectorVal, $lengthVal, $iVar, $itemVal)
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
          outputSize = Some(lengthValRef)),
        nextOps,
        coercionSuccessVarDefRef._2)

      sub.copy(
        prelude = List(vectorValDef, lengthValDef) ++ sub.prelude,
        beforeBody = Nil,
        body = List(typed(q"""
          ..${interruptor.defs}
          ..${sub.beforeBody};
          $iVarDef;
          while (${interruptor.composeTest(q"$iVarRef < $lengthValRef")}) {
            $itemValDef;
            ..$extractionCode
            ..${sub.body};
            $iVarRef += 1
          }
          ..${sub.afterBody}
        """)),
        afterBody = Nil
      )
    }
  }
}
