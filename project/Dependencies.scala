import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    ScalaToolsSnapshots,
    "Typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
    "spray repo" at "http://repo.spray.cc/"
  )

  def compile   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided  (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def test      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def runtime   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  def container (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  object V {
    val akka     = "2.0.3"
    val spray    = "1.0-M2"
  }

  // Logging
  val slf4j       = "org.slf4j"         %  "slf4j-api"       % "1.6.4"
  val logback     = "ch.qos.logback"    %  "logback-classic" % "1.0.0"

  // Akka
  val akkaActor   = "com.typesafe.akka" %  "akka-actor"      % V.akka
  val akkaRemote  = "com.typesafe.akka" %  "akka-remote"     % V.akka
  val akkaSlf4j   = "com.typesafe.akka" %  "akka-slf4j"      % V.akka
  val akkaTestKit = "com.typesafe.akka" %  "akka-testkit"    % V.akka

  // Spray
  val sprayServer = "cc.spray"          %  "spray-server"    % V.spray
  val sprayCan    = "cc.spray"          %  "spray-can"       % V.spray

  // Tests
  val specs2      = "org.specs2"        %% "specs2"          % "1.7.1"
  val scalatest   = "org.scalatest"     %% "scalatest"       % "2.0.M3"

  // General
  val subset      = "com.osinka.subset" %% "subset"          % "1.0.0"
  val pegdown     = "org.pegdown"       %  "pegdown"         % "1.1.0"

  // Dependency sets
  val akka = Seq(akkaActor, akkaRemote, akkaSlf4j)
  val spray = Seq(sprayServer, sprayCan)
}
