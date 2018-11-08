name := "ibmDFDLCrossTester"
 
organization := "io.github.openDFDL"
 
version := "0.0.1"

scalaVersion := "2.12.7"
 
packageOptions += Package.ManifestAttributes(
  // This module uses versions of icu that are incompatible with other things
  // so must be loaded specially by a facility that reads this property and insures
  // this code uses these jars in preference to anything else on the classpath,
  // but without interfereing with anything else.
  "module-classpath" -> (Compile / unmanagedClasspath).value.files.mkString(":")
)


crossPaths := true
 
testOptions in ThisBuild += Tests.Argument(TestFrameworks.JUnit, "-v")
 
libraryDependencies in ThisBuild := Seq(
  "junit" % "junit" % "4.11" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
  "commons-io" % "commons-io" % "2.6",
  "org.apache.daffodil" %% "daffodil-tdml" % "2.3.0-SNAPSHOT"
)

