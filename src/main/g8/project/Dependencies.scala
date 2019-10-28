import sbt._

object Dependencies {

  val logbackClassicVersion = "1.2.3"
  val scallopVersion = "3.3.1"

  //versions
  val slickVersion = "3.3.2"
  val postgresqlVersion = "42.2.6"
  val idGeneratorVersion = "1.2.1"

  val akkaHttpVersion = "10.1.10"
  val akkaVersion = "2.5.25"

  val junitVersion = "4.12"
  val scalatestVersion = "3.1.0-RC3"
  val flexmarkVersion = "0.35.10"
  val h2Version = "1.4.199"

  //test deps
  val junit = "junit" % "junit" % junitVersion % Test
  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion % Test
  val flexmark = "com.vladsch.flexmark" % "flexmark-all" % flexmarkVersion % Test
  val testDeps = Seq(junit,scalatest, flexmark)

  //akka deps
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  val akkaDeps = Seq(akkaHttp, akkaStream, akkaSlf4j)

  //db deps:
  val slick = "com.typesafe.slick" %% "slick" % slickVersion
  val slickCodegen = "com.typesafe.slick" %% "slick-codegen" % slickVersion
  val slickHikariCp = "com.typesafe.slick" %% "slick-hikaricp" % slickVersion
  val postgresql = "org.postgresql" % "postgresql" % postgresqlVersion
  val h2 = "com.h2database" % "h2" % h2Version
  val idGenerator = "com.softwaremill.common" %% "id-generator" % idGeneratorVersion
  val dbDeps = Seq(h2, slick, slickCodegen, slickHikariCp, idGenerator) //for production likely change to postgresql

  //misc
  val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackClassicVersion
  val scallop = "org.rogach" %% "scallop" % scallopVersion

  //module dependencies:
  val serverDependcies = akkaDeps ++ testDeps ++ dbDeps ++ Seq(logbackClassic)
  val clientDependcies = akkaDeps ++ testDeps ++ Seq(scallop, logbackClassic)
  val commonsDependcies = testDeps ++ dbDeps
}