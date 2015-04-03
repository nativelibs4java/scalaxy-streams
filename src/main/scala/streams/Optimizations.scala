package scalaxy.streams

private[streams] object Optimizations
{
  def messageHeader = "[Scalaxy] "

  def optimizedStreamMessage(streamDescription: String, strategy: OptimizationStrategy): String =
      messageHeader +
      "Optimized stream " + streamDescription +
      " (strategy: " + strategy.name + ")"
}

private[streams] trait Optimizations
{
  val global: scala.reflect.api.Universe
  import global._

  lazy val OptimizationStrategyClass =
    rootMirror.staticClass("scalaxy.streams.OptimizationStrategy")

  def matchStrategyTree(inferImplicitValue: Type => Tree): OptimizationStrategy = 
  {
    flags.strategy.getOrElse {
      val optimizationStrategyValue: Tree = try {
        val tpe = OptimizationStrategyClass.asType.toType
        inferImplicitValue(tpe)
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
