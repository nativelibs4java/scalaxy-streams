name := "scalaxy-streams"

organization := "com.nativelibs4java"

version := "0.4-SNAPSHOT"

scalaVersion := "2.11.5"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

fork in Test := true

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-Xlog-free-types",
  "-optimise",
  "-Yclosure-elim",
  "-Yinline",
  "-feature",
  "-unchecked"
)

watchSources <++= baseDirectory map { path => (path / "examples" ** "*.scala").get }

scalacOptions in console in Compile <+= (packageBin in Compile) map("-Xplugin:" + _)

scalacOptions in console in Compile ++= Seq(
  "-Xplugin-require:scalaxy-streams",
  "-Xprint:scalaxy-streams"
)

// ScalariformKeys.preferences := {
//   import scalariform.formatter.preferences._
//
//   FormattingPreferences()
//     .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
//     .setPreference(DoubleIndentClassDeclaration, true)
//     .setPreference(AlignSingleLineCaseStatements, true)
//     .setPreference(IndentSpaces, 2)
//     .setPreference(IndentWithTabs, false)
//     .setPreference(PreserveDanglingCloseParenthesis, true)
// }
//
// scalariformSettings


licenses := Seq("BSD-3-Clause" -> url("http://www.opensource.org/licenses/BSD-3-Clause"))

homepage := Some(url("https://github.com/nativelibs4java/scalaxy-streams"))

testOptions in Global += Tests.Argument(TestFrameworks.JUnit, "-v")

pomIncludeRepository := { _ => false }

pomExtra := (
  <scm>
    <url>git@github.com:nativelibs4java/scalaxy-streams.git</url>
    <connection>scm:git:git@github.com:nativelibs4java/scalaxy-streams.git</connection>
  </scm>
  <developers>
    <developer>
      <id>ochafik</id>
      <name>Olivier Chafik</name>
      <url>http://ochafik.com/</url>
    </developer>
  </developers>
)

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("-SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

