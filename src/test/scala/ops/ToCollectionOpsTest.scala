package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

class ToCollectionOpsTest extends StreamComponentsTestBase with StreamTransforms {
  import global._

  @Test
  def testToIterator {
    val SomeToCollectionOp(_, ToIteratorOp) =
      typecheck(q"Array(1).toIterator")
    val SomeReductionOp(SomeToCollectionOp(_, ToIteratorOp), _) =
      typecheck(q"Array(1).toIterator.sum")
  }

  @Test
  def testToList {
    val SomeToCollectionOp(_, ToListOp) =
      typecheck(q"Array(1).toList")
    val SomeReductionOp(SomeToCollectionOp(_, ToListOp), _) =
      typecheck(q"Array(1).toList.sum")
  }

  @Test
  def testToArray {
    val SomeToCollectionOp(_, ToArrayOp(_)) =
      typecheck(q"Array(1).toArray")
    val SomeReductionOp(SomeArrayOpsOp(SomeToCollectionOp(_, ToArrayOp(_)), _), _) =
      typecheck(q"Array(1).toArray.sum")
  }

  @Test
  def testToVector {
    val SomeToCollectionOp(_, ToVectorOp) =
      typecheck(q"Array(1).toVector")
    val SomeReductionOp(SomeToCollectionOp(_, ToVectorOp), _) =
      typecheck(q"Array(1).toVector.sum")
  }
}
