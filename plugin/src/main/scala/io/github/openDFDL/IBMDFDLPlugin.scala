package io.github.openDFDL

import sbt._
import Keys._

import org.apache.daffodil.DaffodilPlugin

object IBMDFDLPlugin extends AutoPlugin {
  // only enable this plugin if the DaffodilPlugin is enabled
  override def requires = DaffodilPlugin
  override def trigger  = allRequirements

  object autoImport {
    lazy val IbmConfig = Configuration.of("IBM", "ibm").extend(Test).describedAs("Configurations for using IBM DFDL CrossTester")
    lazy val ibmTest = taskKey[Unit]("Run tests using IBM DFDL CrossTester")
  }

  import autoImport._

  private val sbtIbmDfdlPluginVersion = this.getClass.getPackage.getImplementationVersion

  override lazy val projectConfigurations = Seq(IbmConfig)

  override lazy val projectSettings: Seq[Setting[_]] =
    inConfig(IbmConfig)(
      Defaults.testSettings ++ Seq(
        sourceDirectory := (Test / sourceDirectory).value,
        sources := (Test / sources).value,
        dependencyClasspath := (dependencyClasspath.value).filterNot { _.data.name.startsWith("icu4j")},
      )
    ) ++
    Seq(
      libraryDependencies ++= Seq(
        "junit" % "junit" % "4.13.2" % IbmConfig.name,
        "com.github.sbt" % "junit-interface" % "0.13.3" % IbmConfig.name,
        "io.github.openDFDL" % "ibm-tdml-processor" % sbtIbmDfdlPluginVersion % s"${IbmConfig.name}->compile"
      ),
      ibmTest := (IbmConfig / test).value
    )
}