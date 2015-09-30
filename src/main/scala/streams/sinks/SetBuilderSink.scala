package scalaxy.streams

import scala.collection.generic.CanBuildFrom

private[streams] trait SetBuilderSinks extends BuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  case object SetBuilderSink extends BuilderSink
  {
    override def describe = Some("Set")

    override def usesSizeHint = false

    override def subTrees = Nil

    override def isFinalOnly = true

    override def createBuilder(input: StreamInput) = {
      import input.typed
      typed(q"$ImmutableSetModule.canBuildFrom[${input.vars.tpe}]()")
    }

    private[this] val ImmutableSetModule =
      rootMirror.staticModule("scala.collection.immutable.Set")
  }
}
