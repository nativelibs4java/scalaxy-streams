package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

import scala.collection.mutable.ListBuffer
import scala.tools.reflect.ToolBox
import scala.tools.reflect.FrontEnd

case class CompilerMessages(
  infos: List[String] = Nil,
  warnings: List[String] = Nil,
  errors: List[String] = Nil)

trait StreamComponentsTestBase extends Utils with ConsoleReporters
{
  scalaxy.streams.flags.logLevel = LogLevel.Normal
  scalaxy.streams.flags.quietWarnings = false

  val global = scala.reflect.runtime.universe
  val commonOptions = "-usejavacp "
  val optOptions = "-optimise -Yclosure-elim -Yinline "//-Ybackend:GenBCode"
  import scala.reflect.runtime.currentMirror

  private[this] lazy val toolbox = currentMirror.mkToolBox(options = commonOptions)

  def typecheck(t: global.Tree): global.Tree =
    toolbox.typecheck(t.asInstanceOf[toolbox.u.Tree]).asInstanceOf[global.Tree]

  def compileOpt(source: String) = compile(source, opt = true)
  def compileFast(source: String) = compile(source, opt = false)

  private[this] def threadLocal[A](a: => A) =
    new ThreadLocal[A] {
      override def initialValue() = a
    }

  private[this] class TestToolbox(opt: Boolean) {

    val infosBuilder, warningsBuilder, errorsBuilder =
      ListBuffer[String]()

    def reset() {
      infosBuilder.clear()
      warningsBuilder.clear()
      errorsBuilder.clear()
    }
    
    private[this] val frontEnd = new FrontEnd {
      override def display(info: Info) {
        val builder: ListBuffer[String] = info.severity match {
          case INFO => infosBuilder
          case WARNING => warningsBuilder
          case ERROR => errorsBuilder
        }

        builder += info.msg
      }
      override def interactive() {}
    }

    val toolbox = currentMirror.mkToolBox(
      frontEnd,
      if (opt) commonOptions + optOptions else commonOptions)
  }

  private[this] val optToolbox = threadLocal(new TestToolbox(true))
  private[this] val normalToolbox = threadLocal(new TestToolbox(false))

  private[this] def compile(source: String, opt: Boolean = false): (() => Any, CompilerMessages) = {
    
    val testToolbox = (if (opt) optToolbox else normalToolbox).get
    testToolbox.reset()
    import testToolbox.toolbox.u._

    try {
      val tree = testToolbox.toolbox.parse(source);
      val compilation = testToolbox.toolbox.compile(tree)

      (
        compilation,
        CompilerMessages(
          infos = testToolbox.infosBuilder.result,
          warnings = testToolbox.warningsBuilder.result,
          errors = testToolbox.errorsBuilder.result)
      )
    } catch { case ex: Throwable =>
      throw new RuntimeException(s"Failed to compile:\n$source", ex)
    }
  }

  def optimizedCode(source: String, strategy: OptimizationStrategy): String = {
    val src = s"""{
      import ${strategy.fullName}
      scalaxy.streams.optimize {
        $source
      }
    }"""
    // println(src)
    src
  }

  def testMessages(source: String, expectedMessages: CompilerMessages,
                   expectWarningRegexp: Option[List[String]] = None)
                  (implicit strategy: OptimizationStrategy) {

    val actualMessages = try {
      assertMacroCompilesToSameValue(
        source,
        strategy = strategy)
    } catch { case ex: Throwable =>
      ex.printStackTrace()
      if (ex.getCause != null)
        ex.getCause.printStackTrace()
      throw new RuntimeException(ex)
    }

    val processedActualMessages = actualMessages
      .copy(warnings = actualMessages.warnings
        .map(_.replaceAll("__wrapper[\\$\\w]+\\.", "")))

    if (expectedMessages.infos != processedActualMessages.infos) {
      processedActualMessages.infos.foreach(println)
      assertEquals(expectedMessages.infos, processedActualMessages.infos)
    }
    expectWarningRegexp match {
      case Some(rxs) =>
        val warnings = processedActualMessages.warnings
        assert(expectedMessages.warnings.isEmpty)
        assertEquals(warnings.toString,
          rxs.size, warnings.size)
        for ((rx, warning) <- rxs.zip(warnings)) {
          assertTrue(s"Expected '$rx', got '$warning'\n(full warnings: ${processedActualMessages.warnings})",
            warning.matches(rx))
        }
        // assertEquals(processedActualMessages.warnings.toString,
        //   count, processedActualMessages.warnings.size)

      case None =>
        assertEquals(
          expectedMessages.warnings.toSet,
          processedActualMessages.warnings.toSet)
    }
    assertEquals(expectedMessages.errors, processedActualMessages.errors)
  }

  val pluginCompiler = threadLocal {
    StreamsCompiler.makeCompiler(StreamsCompiler.consoleReportGetter)
  }

  def assertPluginCompilesSnippetFine(source: String) {
    val sourceFile = {
      import java.io._

      val f = File.createTempFile("test-", ".scala")
      val out = new PrintStream(f)
      out.println(s"object Test { def run = { $source } }")
      out.close()

      f
    }

    val args = Array(sourceFile.toString)
    try {
      pluginCompiler.get()(args)
    } finally {
      sourceFile.delete()
    }
  }

  case class Exceptional(exceptionString: String)
  def eval(f: () => Any) =
    try { f() }
    catch {
      case ex: java.lang.reflect.InvocationTargetException =>
        Exceptional(ex.getCause.toString)
    }

  def assertMacroCompilesToSameValue(source: String, strategy: OptimizationStrategy): CompilerMessages = {
    val (unoptimized, unoptimizedMessages) = compileFast(source)
    val (optimized, optimizedMessages) = compileFast(optimizedCode(source, strategy))

    val unopt = eval(unoptimized)
    val opt = eval(optimized)
    assertEqualValues(source + "\n" + optimizedMessages, unopt, opt)

    // assertEquals("Unexpected messages during unoptimized compilation",
    //   CompilerMessages(), unoptimizedMessages)

    optimizedMessages
  }

  def assertEqualValues(message: String, expected: Any, actual: Any) = {
    (expected, actual) match {
      case (expected: Array[Int], actual: Array[Int]) =>
        assertArrayEquals(message, expected, actual)
      case (expected: Array[Short], actual: Array[Short]) =>
        assertArrayEquals(message, expected, actual)
      case (expected: Array[Byte], actual: Array[Byte]) =>
        assertArrayEquals(message, expected, actual)
      case (expected: Array[Char], actual: Array[Char]) =>
        assertArrayEquals(message, expected, actual)
      case (expected: Array[Float], actual: Array[Float]) =>
        assertArrayEquals(message, expected, actual, 0)
      case (expected: Array[Double], actual: Array[Double]) =>
        assertArrayEquals(message, expected, actual, 0)
      case (expected: Array[Boolean], actual: Array[Boolean]) =>
        assertArrayEquals(message, expected.map(_.toString: AnyRef), actual.map(_.toString: AnyRef))
      case (expected: Array[AnyRef], actual: Array[AnyRef]) =>
        assertArrayEquals(message, expected, actual)
      case (expected, actual) =>
        assertEquals(message, expected, actual)
        assertEquals(message + " (different classes)",
          Option(expected).map(_.getClass).orNull,
          Option(actual).map(_.getClass).orNull)
    }
  }
}
