package scalaxy.streams

import scala.collection.generic.CanBuildFrom

private[streams] trait JsArrayBuilderSinks
    extends BuilderSinks
    with ScalaJsSymbols
{
  val global: scala.reflect.api.Universe
  import global._

  case object JsArrayBuilderSink extends StreamSink
  {
    override def describe = Some("js.Array")

    override def lambdaCount = 0

    override def subTrees = Nil

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input._

      // Note: unlike in ArrayStreamSource, we don't optimize the case
      // where the output size is known, for JavaScript arrays often perform
      // just as well (or better) with sequential append than with
      // fixed alloc + indexed updates.

      require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")

      val array = fresh("array")

      val componentTpe = normalize(input.vars.tpe)

      val Block(List(
          arrayDecl,
          append), arrayRef) = typed(q"""
        private[this] var $array = new ${JsWrappedArraySymOpt.get}[$componentTpe](
          new ${JsArraySymOpt.get}[$componentTpe]()
        );
        $array.append(${input.vars.alias.get});
        $array.array
      """)

      StreamOutput(
        prelude = List(arrayDecl),
        body = List(append),
        ending = List(arrayRef))
    }
  }
}
