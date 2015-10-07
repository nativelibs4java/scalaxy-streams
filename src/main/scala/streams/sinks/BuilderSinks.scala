package scalaxy.streams

private[streams] trait BuilderSinks extends StreamComponents {
  val global: scala.reflect.api.Universe
  import global._

  // Base class for builder-based sinks.
  trait BuilderSink extends StreamSink
  {
    override def lambdaCount = 0

    def usesSizeHint: Boolean

    def createBuilder(input: StreamInput): Tree

    override def emit(input: StreamInput, outputNeeds: OutputNeeds, nextOps: OpsAndOutputNeeds): StreamOutput =
    {
      import input._

      requireSinkInput(input, outputNeeds, nextOps)

      val builder = fresh("builder")
      require(input.vars.alias.nonEmpty, s"input.vars = $input.vars")

      // println("input.vars.alias.get = " + input.vars.alias.get + ": " + input.vars.tpe)
      val sizeHintOpt = input.outputSize.map(s => q"$builder.sizeHint($s)")

      val Block(List(
          builderDef,
          sizeHint,
          builderAdd), result) = typed(q"""
        private[this] val $builder = ${createBuilder(input)};
        ${sizeHintOpt.getOrElse(dummyStatement(fresh))};
        $builder += ${input.vars.alias.get};
        $builder.result()
      """)

      StreamOutput(
        prelude = List(builderDef),
        beforeBody = if (usesSizeHint) List(sizeHint) else Nil,
        body = List(builderAdd),
        ending = List(result))
    }
  }
}
