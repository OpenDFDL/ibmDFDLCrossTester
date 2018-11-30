import sbt._
import Keys._

object IBMDFDLCrossTesterPlugin extends AutoPlugin {

  //
  // MODIFY THIS PATH TO REFER TO THE lib DIRECTORY WHERE YOU HAVE THE 
  // IBM DFDL CROSS TESTER CHECKED OUT. 
  // 
  // THESE JARS MUST BE COPIED FROM THE IBM DFDL DISTRIBUTION INTO THIS lib
  // DIRECTORY AS PART OF SETUP OF THE IBM DFDL CROSS TESTER
  //
  val ibmDFDLJarDirectory = file("/home/mbeckerle/dataiti/git/ibmDFDLCrossTester/lib/")

  lazy val settings = Seq(
    libraryDependencies ++= Seq(
      //
      // MODIFY THE VERSION (OR "latest.integration" WHICH SELECTS THE LATEST SNAPSHOT)
      // TO REFLECT THE VERSION OF THE IBM DFDL CROSS TESTER YOU REQUIRE.
      //
      "io.github.openDFDL" %% "ibm-tdml-processor" % "latest.integration" % "test"
    ),
    dependencyClasspath in Test := (dependencyClasspath in Test).value.filter { dep =>
      //
      // Modifies classpath to exclude daffodil's tdml-processor module
      // and any icu4j dependencies (which conflict between IBM DFDL and Daffodil.
      //
      // Over time this code may need to be adjusted. It is specific to the
      // particular library conflicts between the two TDML processors, and the
      // means of working around them.
      //
      val name = dep.data.name
      if (name.startsWith("daffodil-tdml-processor")) false
      else if (name.startsWith("icu4j")) false
      else true
    } ++ Attributed.blankSeq((ibmDFDLJarDirectory ** "*.jar").get)
  )
}
