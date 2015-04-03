package scalaxy.streams

private[streams] object Optimizations
{
  def messageHeader = "[Scalaxy] "

  def optimizedStreamMessage(streamDescription: String, strategy: OptimizationStrategy): String =
      messageHeader +
      "Optimized stream " + streamDescription +
      " (strategy: " + strategy.name + ")"
}

private[streams] trait Optimizations extends OptionalSymbols
{
  val global: scala.reflect.api.Universe
  import global._

  private[this] lazy val OptimizationStrategySymOpt =
    optionalStaticClass("scalaxy.streams.OptimizationStrategy")

  def matchStrategyTree(inferImplicitValue: Type => Tree): OptimizationStrategy = 
  {
    flags.strategy.getOrElse {
      val optimizationStrategyValue: Tree =
        try {
          OptimizationStrategySymOpt
            .map(sym => inferImplicitValue(sym.asType.toType))
            .getOrElse(EmptyTree)
        } catch {
          case ex: Throwable =>
            ex.printStackTrace()
            EmptyTree
        }

      optimizationStrategyValue match {
        case EmptyTree =>
          scalaxy.streams.strategy.global

        case strategyTree =>
          scalaxy.streams.strategy.forName(strategyTree.symbol.name.toString).get
      }
    }
  }
}
