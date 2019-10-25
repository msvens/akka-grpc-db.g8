import Dependencies._
import sbt.Keys._
import sbt._

lazy val scala212 = "2.12.9"
lazy val scala213 = "2.13.1"
lazy val supportedScalaVersions = List(scala212, scala213)


ThisBuild / version := "0.1-SNAPSHOT"
ThisBuild / organization := "$organization$"
ThisBuild / scalaVersion := scala213
ThisBuild / Test / publishArtifact := false
ThisBuild / Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/site/test-reports")

lazy val server = (project in file ("server")).
  enablePlugins(JavaServerAppPackaging,DebianPlugin,SystemVPlugin).
  settings(
    crossScalaVersions := supportedScalaVersions,
    name := "$name$-server",
    libraryDependencies ++= serverDependcies
  ).dependsOn(common)

lazy val client = (project in file ("client")).
  settings(
    crossScalaVersions := supportedScalaVersions,
    name := "$name$-client",
    libraryDependencies ++= clientDependcies,
  ).dependsOn(common)

lazy val common = (project in file ("common")).
  enablePlugins(AkkaGrpcPlugin).
  settings(
    crossScalaVersions := supportedScalaVersions,
    name := "$name$-common",
    libraryDependencies ++= commonsDependcies,
  )


lazy val root = (project in file (".")).aggregate(common,server,client).
  settings(
    crossScalaVersions := Nil,
    publish / skip := true
  )