package scalaxy.streams

package test

import org.junit._
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

@RunWith(classOf[Parallelized])
class SubTreeEvaluationOrderTest(
    name: String,
    source: String,
    expectedMessages: CompilerMessages)
{
  scalaxy.streams.flags.verbose = true
  scalaxy.streams.flags.veryVerbose = false
  scalaxy.streams.flags.quietWarnings = true

  import SubTreeEvaluationOrderTest._

  @Test
  def test = testMessages(source, expectedMessages)(strategy)
}

object SubTreeEvaluationOrderTest
    extends StreamComponentsTestBase with StreamTransforms {
  
  import IntegrationTests.msgs
  
  implicit def strategy = scalaxy.streams.strategy.safe
  
  @Parameters(name = "{0}")
  def data: java.util.Collection[Array[AnyRef]] = List[(String, CompilerMessages)](

    """
      List(named("a", 1))
        .map(_ + named("added", 1))
        .mkString(named("pre", "{{"), named("sep", "; "), named("suf", "}}"))
    """
      -> msgs(/* side effects + safe strategy */),

    """
      List(1)
        .map(_ + 1)
        .mkString("{{", "; ", "}}")
    """
      -> msgs("List.map.mkString"),

    """
      List(named("a", 1), named("b", 2)).map(_ + 1).toList
    """
      -> msgs("List.map -> List")

  ).map({
    case (src, msgs) =>
      Array[AnyRef](
        src.replaceAll(raw"(?m)\s+", " ").trim,
        """
          var names = collection.mutable.ArrayBuffer[String]();
          def named[A](name: String, a: A): A = {
            names += name
            a
          }
          val value = {""" + src + """};
          (names.toList, value)
        """,
        msgs
      )
  })
}
