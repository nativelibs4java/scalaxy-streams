package scalaxy.streams

private[streams] trait ReductionOps
    extends StreamComponents
    with Strippers
    with SymbolMatchers
    with TuploidValues
    with UnusableSinks
    with TransformationClosures
{
  val global: scala.reflect.api.Universe
  import global._

  object SomeReductionOp extends StreamOpExtractor
  {
    private[this] lazy val NumericModule = rootMirror.staticModule("scala.math.Numeric")

    private[this] def isStandardNumeric(tpe: Type, numeric: Tree) =
      isPrimitiveNumeric(normalize(tpe)) &&
      Option(numeric.symbol).exists(_.owner.fullName == NumericModule.fullName)

    override def unapply(tree: Tree) = Option(tree) collect {
      case q"$target.sum[${tpt}]($numeric)" if isStandardNumeric(tree.tpe, numeric) =>
        (target, SumOp(tpt.tpe))

      case q"$target.product[${tpt}]($numeric)" if isStandardNumeric(tree.tpe, numeric) =>
        (target, ProductOp(tpt.tpe))

      case q"$target.reduceLeft[${tpt}](${Closure2(closure)})" =>
        (target, ReduceLeftOp(tpt.tpe, closure))
    }
  }

  trait SimpleReductorOp extends StreamOp
  {
    def opName: String
    override def describe = Some(opName)
    def throwsIfEmpty: Boolean
    def tpe: Type
    def initialAccumulatorValue: Option[Tree]
    def canAlterSize = true
    def accumulate(streamInput: StreamInput, accumulator: Tree, newValue: Tree): Tree

    override def transmitOutputNeedsBackwards(paths: Set[TuploidPath]) =
      Set(RootTuploidPath)

    override def lambdaCount = 0
    override def sinkOption = Some(ScalarSink)

    override def emit(streamInput: StreamInput,
                      outputNeeds: OutputNeeds,
                      nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      val List((ScalarSink, _)) = nextOps

      import streamInput._

      // requireSinkInput(input, outputNeeds, nextOps)

      val result = fresh(opName)
      val empty = fresh("empty")
      require(streamInput.vars.alias.nonEmpty, s"streamInput.vars = $streamInput.vars")

      // println("inputVars.alias.get = " + inputVars.alias.get + ": " + inputVars.tpe)
      val emptyMessage = s"empty.$opName"
      val Block(List(
          resultDef,
          emptyDef,
          emptyRef,
          setNotEmpty,
          throwIfEmpty), resultRef) = typed(q"""
        ${newVar(result, tpe, initialAccumulatorValue.getOrElse(getDefaultValueTree(tpe)))};
        private[this] var $empty = true;
        $empty;
        $empty = false;
        if ($empty) throw new UnsupportedOperationException($emptyMessage);
        $result
      """)

      val acc = q"""
        ${accumulate(streamInput, resultRef, streamInput.vars.alias.get)}
          .asInstanceOf[$tpe]
      """

      val Block(List(resultAdd), _) = typed(initialAccumulatorValue match {
        case Some(_) =>
          q"$resultRef = $acc; null"

        case None =>
          q"$resultRef = if ($emptyRef) ${streamInput.vars.alias.get} else $acc; null"
      })

      if (throwsIfEmpty)
        StreamOutput(
          prelude = List(resultDef, emptyDef),
          body = List(resultAdd) ++ (if (throwsIfEmpty) List(setNotEmpty) else Nil),
          ending = List(throwIfEmpty, resultRef))
      else
        StreamOutput(
          prelude = List(resultDef),
          body = List(resultAdd),
          ending = List(resultRef))
    }
  }

  case class ReduceLeftOp(tpe: Type, closure: Tree)
      extends SimpleReductorOp
  {
    private[this] lazy val q"($accumulatorValDef, $newValueValDef) => ${Strip(body)}" = closure
    private[this] lazy val closureSymbol = closure.symbol

    override def opName = "reduceLeft"
    override def initialAccumulatorValue = None//getDefaultValueTree(tpe)
    override def throwsIfEmpty = true
    override def subTrees = List(accumulatorValDef, newValueValDef, body)

    private[this] def makeParamScalarValue(param: ValDef): ScalarValue[Symbol] =
      ScalarValue(param.symbol.typeSignature, alias = param.symbol.asOption)
    
    private[this] lazy val accumulatorScalarValue = makeParamScalarValue(accumulatorValDef)
    private[this] lazy val newValueScalarValue = makeParamScalarValue(newValueValDef)

    override def accumulate(streamInput: StreamInput, accumulator: Tree, newValue: Tree): Tree = {
      // TODO: build a TransformationClosure with the newValue symbol, and re-replace the accumulator behind.
      import streamInput._
      
      val newValueReplacer =
        getReplacer(
          newValueScalarValue,
          ScalarValue[Tree](streamInput.vars.tpe, alias = Some(newValue)))
      val accumulatorReplacer =
        getReplacer(
          accumulatorScalarValue,
          ScalarValue[Tree](tpe, alias = Some(accumulator)))
      val fullTransform = (tree: Tree) => {
        transform(
          HacksAndWorkarounds.replaceDeletedOwner(global)(
            accumulatorReplacer(newValueReplacer(tree)),
            deletedOwner = closureSymbol,
            newOwner = currentOwner))
      }

      fullTransform(body)
    }
  }

  case class SumOp(tpe: Type) extends SimpleReductorOp
  {
    override def opName = "sum"
    override def initialAccumulatorValue = Some(q"0")
    override def throwsIfEmpty = false
    override def subTrees = Nil
    override def accumulate(streamInput: StreamInput, accumulator: Tree, newValue: Tree): Tree =
      q"$accumulator + $newValue"
  }

  case class ProductOp(tpe: Type) extends SimpleReductorOp
  {
    override def opName = "product"
    override def initialAccumulatorValue = Some(q"1")
    override def throwsIfEmpty = false
    override def subTrees = Nil
    override def accumulate(streamInput: StreamInput, accumulator: Tree, newValue: Tree): Tree =
      q"$accumulator * $newValue"
  }
}
