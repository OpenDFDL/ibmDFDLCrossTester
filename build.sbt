name := "ibm-tdml-processor"
 
organization := "io.github.openDFDL"
 
version := "1.0.0-SNAPSHOT"

Test / parallelExecution := false

libraryDependencies ++= Seq(
  "org.apache.daffodil" %% "daffodil-tdml-lib" % daffodilVersion.value,
  "javax.xml.bind" % "jaxb-api" % "2.3.1"
)

excludeDependencies += "com.ibm.icu" % "icu4j"

enablePlugins(DaffodilPlugin)
