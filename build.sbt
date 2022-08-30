import Dependencies._

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.github.windymelt.ffq"
ThisBuild / organizationName := "ffq"

lazy val root = (project in file("."))
  .settings(
    name := "ffq",
    libraryDependencies += scalaTest % Test,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.3.12"
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
