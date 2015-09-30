package scalaxy.streams

import scala.collection.generic.CanBuildFrom

private[streams] trait ListBufferSinks extends BuilderSinks {
  val global: scala.reflect.api.Universe
  import global._

  case object ListBufferSink extends BuilderSink
  {
    override def describe = Some("List")

    override def usesSizeHint = false

    override def subTrees = Nil

    private[this] val ListBufferModule =
      rootMirror.staticModule("scala.collection.mutable.ListBuffer")
    
    // lazy val ListModule =
    //   rootMirror.staticModule("scala.collection.immutable.List")

    override def createBuilder(input: StreamInput) = {
      import input.typed
      typed(q"$ListBufferModule[${input.vars.tpe}]()")
      // typed(q"$ListModule.newBuilder[${inputVars.tpe}]")
    }
  }
}
