package scalaxy.streams

private[streams] trait StreamResults extends TuploidValues {
  val global: scala.reflect.api.Universe
  import global._

  type OutputNeeds = Set[TuploidPath]

  case class StreamOutput(
      prelude: List[Tree] = Nil,
      beforeBody: List[Tree] = Nil,
      body: List[Tree] = Nil,
      afterBody: List[Tree] = Nil,
      ending: List[Tree] = Nil)
  {
    // val flatten: List[Tree] = {
    //   val b = collection.mutable.ListBuffer[Tree]()
    //   for (list <- List(prelude, beforeBody, body, afterBody);
    //        item <- list) {
    //     item match {
    //       case Block(items, v) =>
    //         b ++= items
    //         // println("V = " + v + ": " + v.getClass)
    //         b += v
    //       case t =>
    //         b += t
    //     }
    //   }
    //   b ++= ending
    //   b.result()
    // }
    def flatten: List[Tree] =
      prelude ++ beforeBody ++ body ++ afterBody ++ ending

    // for ((n, list) <- Map("prelude" -> prelude, "beforeBody" -> beforeBody, "body" -> body, "afterBody" -> afterBody, "ending" -> ending);
    //      Block(list, v) <- list) {
    //   println(s"FOUND block item $v in $n")
    // }

    def compose(typed: Tree => Tree) =
      typed(q"..$flatten")

    def map(f: Tree => Tree): StreamOutput =
      copy(
        prelude = prelude.map(f),
        beforeBody = beforeBody.map(f),
        body = body.map(f),
        afterBody = afterBody.map(f),
        ending = ending.map(f))
  }

  val NoStreamOutput = StreamOutput()

  case class StreamInput(
      vars: TuploidValue[Tree],
      outputSize: Option[Tree] = None,
      /** Type of the element (for verification purposes) and a tree that gives its ClassTag */
      elementClassTag: Option[(Type, Tree)] = None,
      index: Option[Tree] = None,
      loopInterruptor: Option[Tree] = None,
      fresh: String => TermName,
      transform: Tree => Tree,
      currentOwner: Symbol,
      typed: Tree => Tree)
  {
    for ((elementTpe, classTag) <- elementClassTag) {
      val componentTpe = vars.tpe.dealias
      if (!(elementTpe =:= componentTpe)) {
        sys.error(s"Internal error: mismatching element types from classTag ($elementTpe) and stream ($componentTpe)")
      }
    }
  }
}
