lazy val commonSettings = Seq (
  organization := "io.github.openDFDL",

  version := "1.0.0-SNAPSHOT",
)

lazy val root = (project in file("."))
  .aggregate(plugin, processor.componentProjects.head)
  .settings(
    publish / skip := true,
    publishLocal / skip := true
  )

lazy val plugin = (project in file("plugin"))
  .settings(commonSettings)
  .settings(
    name := "sbt-ibm-dfdl",

    addSbtPlugin("org.apache.daffodil" % "sbt-daffodil" % "1.6.0")
  )
  .enablePlugins(SbtPlugin)

lazy val processor = (project in file("processor"))
  .settings(commonSettings)
  .settings(
    name := "ibm-tdml-processor",

    Test / parallelExecution := false,

    // this is marked as provided to avoid bundling it in the ibm-tdml-processor jar.
    // We really only want the bundled jars to be IBM jars and the IBM TDML processor.
    // The plugin will provide all other dependencies.
    libraryDependencies ++= Seq(
      "javax.xml.bind" % "jaxb-api" % "2.3.1" % "provided"
    ) ++ {
      libraryDependencies.value
        .filter { module =>
          module.name == "daffodil-tdml-junit"
        }
        .map { _.withConfigurations(Some("provided,test")) }
    },
    unmanagedBase := baseDirectory.value.getParentFile / "ibm-dfdl" / "lib",
    Test / unmanagedResourceDirectories += baseDirectory.value.getParentFile / "ibm-dfdl" / "src" / "test" / "resources",
    assembly / assemblyMergeStrategy := {
      case "plugin.properties" => MergeStrategy.concat
      case "plugin.xml" => MergeStrategy.first
      case PathList("META-INF", xs@_*) if
        xs.last.endsWith(".SF") ||
          xs.last.endsWith(".DSA") ||
          xs.last.endsWith(".RSA") ||
          xs.last == "MANIFEST.MF" ||
          xs.last.contains("LICENSE") ||
          xs.last.contains("NOTICE") => MergeStrategy.discard
      case "module-info.class" | "about.html" | "about.properties" |
           "about.mappings" | "plugin.xml" => MergeStrategy.first
      case _ => MergeStrategy.deduplicate
    },
    Compile / packageBin := (assembly).value,
  )
  .daffodilProject()
