name := "ibm-tdml-processor"
 
organization := "io.github.openDFDL"
 
version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.14"
 
parallelExecution in Test := false

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

crossScalaVersions := Seq("2.12.11")

libraryDependencies := Seq(
  "junit" % "junit" % "4.13.1" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.apache.daffodil" %% "daffodil-tdml-lib" % "3.2.0-SNAPSHOT",
  "org.scala-lang.modules" %% "scala-xml" % "2.0.0",
  "javax.xml.bind" % "jaxb-api" % "2.3.1"
)

excludeDependencies += "com.ibm.icu" % "icu4j"

useCoursier := false

retrieveManaged := true
