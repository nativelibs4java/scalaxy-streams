name := "scalaxy-streams"

organization := "com.nativelibs4java"

version := "0.4-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _)

libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)

libraryDependencies ++= Seq(
  "org.scala-js" %% "scalajs-library" % "0.6.5" % "test",
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

testOptions in Global += Tests.Argument(TestFrameworks.JUnit, "-v")

fork in Test := true

// Needed to avoid cryptic EOFException crashes in forked tests
// in Travis with `sudo: false`.
// See https://github.com/sbt/sbt/issues/653
// and https://github.com/travis-ci/travis-ci/issues/3775
javaOptions += "-Xmx1G"

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation", "-feature", "-unchecked",
  "-optimise", "-Yclosure-elim", "-Yinline",
  "-YclasspathImpl:flat",
  "-Xlog-free-types"
)

watchSources <++= baseDirectory map { path => (path / "examples" ** "*.scala").get }

scalacOptions in console in Compile <+= (packageBin in Compile) map("-Xplugin:" + _)

scalacOptions in console in Compile ++= Seq(
  "-Xplugin-require:scalaxy-streams",
  "-Xprint:scalaxy-streams"
)

// ScalariformKeys.preferences := {
//   import scalariform.formatter.preferences._
//   FormattingPreferences()
//     .setPreference(AlignParameters, true)
//     .setPreference(AlignSingleLineCaseStatements, true)
//     .setPreference(CompactControlReadability, true)
//     .setPreference(DoubleIndentClassDeclaration, true)
//     .setPreference(IndentSpaces, 2)
//     .setPreference(IndentWithTabs, false)
//     .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
//     .setPreference(PreserveDanglingCloseParenthesis, true)
// }
// scalariformSettings

homepage := Some(url("https://github.com/nativelibs4java/scalaxy-streams"))

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

licenses := Seq("BSD-3-Clause" -> url("http://www.opensource.org/licenses/BSD-3-Clause"))

pomIncludeRepository := { _ => false }

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("-SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

credentials ++= (for {
  username <- Option(System.getenv("SONATYPE_USERNAME"));
  password <- Option(System.getenv("SONATYPE_PASSWORD"))
} yield Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org", username, password
)).toSeq
