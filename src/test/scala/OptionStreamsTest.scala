package scalaxy.streams

package test

import org.junit._
import org.junit.runner.RunWith
import org.junit.runners.Parameterized.Parameters

import scala.collection.JavaConversions._

import IntegrationTests.{msgs, potentialSideEffectMsgs}

/** This is just a testbed for "fast" manual tests */
class OptionStreamsTest
    extends StreamComponentsTestBase
    with PerformanceTestBase
    with StreamTransforms
{
  import global._

  scalaxy.streams.flags.verbose = true

  // scalaxy.streams.flags.veryVerbose = true
  // scalaxy.streams.flags.debug = true
  // scalaxy.streams.flags.quietWarnings = true

  @Test
  def testOptionCombinations {
    val options = List(
      "None" -> "None",
      "(None: Option[Int])" -> "Option",
      "Option[Any](null)" -> "Option",
      "Option[String](null)" -> "Option",
      "Option[String](\"Y\")" -> "Option",
      "Some(0)" -> "Some",
      "Some(\"X\")" -> "Some")
    val suffixes = List(
      None,
      Some("orNull" -> "orNull"),
      Some("getOrElse(\"Z\")" -> "getOrElse"),
      Some("get" -> "get"),
      Some("find(_.contains(\"2\"))" -> "find"))

    val src = s"""
      def f1(x: Any) = x.toString + "1"
      def f2(x: Any) = x.toString + "2"
      def f3(x: Any) = x.toString + "3"
      def wrap[A](a: => A): Either[A, String] = try { Left(a) } catch { case ex => Right(ex.getMessage) }

      List(
        ${{
          for ((lhs, _) <- options; (rhs, _) <- options; suf <- suffixes) yield {
            val stream = s"$lhs.map(f1).orElse($rhs.map(f2)).map(f3)"
            suf.map({case (s, _) => stream + "." + s}).getOrElse(stream)
          }
        }.map("wrap(" + _ + ")").mkString(",\n        ")}
      )
    """
    // println(src)

    assertMacroCompilesToSameValue(
      src,
      strategy = scalaxy.streams.strategy.foolish)

    // {
    //   import scalaxy.streams.strategy.foolish
    //   testMessages(src, msgs("Some.orElse(Some.map).map -> Option"),
    //     expectWarningRegexp = Some(List("there were \\d+ inliner warnings; re-run with -Yinline-warnings for details")))
    // }
  }

}
