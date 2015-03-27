package scalaxy.streams

import scala.collection.generic.CanBuildFrom

private[streams] trait CanBuildFromSinks
    extends BuilderSinks
    with ArrayBuilderSinks
    with JsArrayBuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  class CanBuildFromSink(canBuildFrom: Tree) extends BuilderSink
  {
    override def subTrees = List(canBuildFrom)

    val TypeRef(_, _, List(_, _, toTpe: Type)) = {
      val sym = rootMirror.staticClass("scala.collection.generic.CanBuildFrom")
      canBuildFrom.tpe.baseType(sym)
    }

    override def describe =
      Some(toTpe.typeSymbol.fullName.replaceAll("^scala\\.collection(\\.immutable)?\\.", ""))

    override def usesSizeHint = true

    override def createBuilder(inputVars: TuploidValue[Tree], typed: Tree => Tree) = {
      typed(q"$canBuildFrom()")
    }
  }

  object CanBuildFromSink
  {
    def unapply(op: StreamOp): Option[CanBuildFromSink] =
      Option(op) collect { case op: CanBuildFromSink => op }

    def apply(canBuildFrom: Tree): StreamSink =
      Option(canBuildFrom.symbol)
        .filter(_ != NoSymbol)
        .map(s => (s.owner.fullName, s.name.toString)) match {
          case Some(("scala.Array", "canBuildFrom")) =>
            ArrayBuilderSink

          case Some(("scala.scalajs.js.Any", "canBuildFromArray")) =>
            JsArrayBuilderSink

          case _ =>
            new CanBuildFromSink(canBuildFrom)
        }
  }
}
