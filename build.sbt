name := "ibmDFDLCrossTester"
 
organization := "io.github.openDFDL"
 
version := "0.0.1"

scalaVersion := "2.12.7"
 
crossPaths := true
 
testOptions in ThisBuild += Tests.Argument(TestFrameworks.JUnit, "-v")
 
libraryDependencies in ThisBuild := Seq(
  "junit" % "junit" % "4.11" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
  "org.apache.daffodil" %% "daffodil-tdml" % "2.3.0-SNAPSHOT"
)

