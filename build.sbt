name := "ibmDFDLCrossTester"
 
organization := "io.github.openDFDL"
 
version := "0.0.0-SNAPSHOT"

scalaVersion := "2.12.7"
 
crossPaths := false

parallelExecution in Test := false

testOptions in ThisBuild += Tests.Argument(TestFrameworks.JUnit, "-v")

// This shuts up sbt warning about being slow when using snapshots
updateOptions := updateOptions.value.withLatestSnapshots(false)

//
// Add daffodil/daffodil-test-ibm1 
//
// Note this assumes that you have daffodil checked out in a 
// peer directory to this cross tester so that ../daffodil gets you
// from the root of this module to the root of daffodil.
// 
unmanagedSourceDirectories in Test += baseDirectory.value /
   ".." / "daffodil" /  "daffodil-test-ibm1" / "src" / "test" / "scala"

unmanagedResourceDirectories in Test += baseDirectory.value /
   ".." / "daffodil" /  "daffodil-test-ibm1" / "src" / "test" / "resources"

//
// Add daffodil/daffodil-test 
// 
// unmanagedSourceDirectories in Test += baseDirectory.value /
//    ".." / "daffodil" /  "daffodil-test" / "src" / "test" / "scala"
//
// unmanagedResourceDirectories in Test += baseDirectory.value /
//   ".." / "daffodil" /  "daffodil-test" / "src" / "test" / "resources"
//

libraryDependencies in ThisBuild := Seq(
  "junit" % "junit" % "4.11" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "org.apache.daffodil" %% "daffodil-tdml" % "latest.integration"
)




