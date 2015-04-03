package scalaxy.streams

package test

import SideEffectSeverity._

import org.junit._
import org.junit.Assert._

class StreamsOutputNeedsTest extends StreamComponentsTestBase with StreamTransforms {
  import global._

  @Test
  def testArrayMapMapFilterMap {
    println("TODO(ochafik): Finish this test!!!")

      // for (o <- Some(Some((1, 2))); (a, b) <- o) yield a + b
    val SomeStream(stream) = typecheck(q"""
      Some(Some((1, 2))).flatMap(o => o.map(p => (p._1, p._2)))
    """)
    // assertEquals("Some.flatMap(Option.withFilter.withFilter.map) -> Option",
    assertEquals("Some.flatMap(Option.map) -> Option",
      stream.describe())

    val List(fop @ FlatMapOp(_, _, _)) = stream.ops
    val Some(nestedStream) = fop.nestedStream

    // val List(f1 @ CoerceOp(_), f2 @ CoerceOp(_), m @ MapOp(_, _)) = nestedStream.ops
    val List(m @ MapOp(_, _)) = nestedStream.ops

    assertEquals(Set(RootTuploidPath), stream.sink.outputNeeds)
    val inputNeeds = stream.computeOutputNeedsBackwards(stream.sink.outputNeeds)

    // val List(Set())
    println(s"inputNeeds = $inputNeeds")
    assertTrue(inputNeeds.head.contains(RootTuploidPath))
  }
}
