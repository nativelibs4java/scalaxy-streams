package scalaxy.streams

import scala.collection.generic.CanBuildFrom

private[streams] trait VectorBuilderSinks extends BuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  case object VectorBuilderSink extends BuilderSink
  {
    override def describe = Some("Vector")

    override def lambdaCount = 0
    
    override def subTrees = Nil

    override def usesSizeHint = false

    // TODO build Vector of same size as source collection if it is known.
    override def createBuilder(input: StreamInput) = {
      import input.typed
      typed(q"$VectorModule.newBuilder[${input.vars.tpe.dealias}]")
    }
    
    private[this] val VectorModule = rootMirror.staticModule("scala.collection.immutable.Vector")
  }
}
