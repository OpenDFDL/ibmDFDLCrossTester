name := "ibm-tdml-processor"
 
organization := "io.github.openDFDL"
 
version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.11"
 
parallelExecution in Test := false

testOptions in ThisBuild += Tests.Argument(TestFrameworks.JUnit, "-v")

crossScalaVersions := Seq("2.12.11", "2.11.12")

libraryDependencies in ThisBuild := Seq(
  "junit" % "junit" % "4.11" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.apache.daffodil" %% "daffodil-tdml-lib" % "2.6.0",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0"
)

useCoursier := false

retrieveManaged := true


