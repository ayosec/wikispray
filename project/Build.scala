import sbt._
import Keys._

object GrapheneBuild extends Build {
  import Dependencies._

  lazy val basicSettings = seq(
    version               := "0.1",
    homepage              := Some(new URL("http://ayosec.com/wikispray")),
    organization          := "com.ayosec",
    organizationHomepage  := Some(new URL("http://aentos.com")),
    description           := "Example application for Spray",
    startYear             := Some(2012),
    licenses              := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scalaVersion          := "2.9.2",
    resolvers             ++= Dependencies.resolutionRepos,
    scalacOptions         := Seq("-deprecation", "-encoding", "utf8", "-Xlint", "-Ydependent-method-types")
  )

  lazy val grapheneApi = Project("wikispray", file("."))
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++=
      compile(akka: _*) ++
      compile(spray: _*) ++
      compile(pegdown, subset, jodaTime) ++
      provided(jodaConvert) ++
      test(scalatest, akkaTestKit))
    .settings(sbtassembly.Plugin.assemblySettings: _*)

}
