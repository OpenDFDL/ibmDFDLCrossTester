name := "ibm-tdml-processor"
 
organization := "io.github.openDFDL"
 
version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.18"
 
Test / parallelExecution := false

libraryDependencies := Seq(
  "org.apache.daffodil" %% "daffodil-tdml-lib" % daffodilVersion.value,
  "javax.xml.bind" % "jaxb-api" % "2.3.1"
)

excludeDependencies += "com.ibm.icu" % "icu4j"

useCoursier := false

retrieveManaged := true

enablePlugins(DaffodilPlugin)
