
# IBM DFDL Cross Tester for Daffodil

This is a test rig that drives TDML tests against IBM DFDL enabling
cross testing of tests easily against both Daffodil and the IBM DFDL implementation.
It uses the TDML Runner library from Daffodil, and complements that with an IBM-specific
TDML processor which drives IBM DFDL from Daffodil's TDML dialect test files.

The purpose of this tool is to help demonstrate interoperability of IBM DFDL and
Daffodil for the overlapping subset of DFDL that both implement. This will drive
convergence of the two implementations so that DFDL users can reliably create
portable DFDL schemas.

Similar cross testers can be created for other DFDL implementations as well.

## Requirements

* IBM DFDL - included in IBM App Connect Enterprise (ACE). A developer edition is available.
* Daffodil 4.1.0 or newer (resolved automatically by the sbt-daffodil plugin)
* **Java 17** - required by Daffodil 4.1.0. See the Java setup note below.

> Earlier versions of Daffodil (3.x and older) used a different internal API.
> See `UPGRADE-NOTES.md` for details on the migration.

Note that Daffodil 3.1.0 or newer is required for TDML parser negative test
cases to work properly. Many DFDL schemas do not have such tests.

## Java Setup

Daffodil 4.1.0 is compiled for Java 17. If your system default Java is older,
pass the `-java-home` flag to every sbt invocation:

```
sbt -java-home "C:\Program Files\IBM\ACE\12.0.12.17\common\java17" compile
sbt -java-home "C:\Program Files\IBM\ACE\12.0.12.17\common\java17" test
```

Java 17 is bundled with ACE at `<ACE_ROOT>\common\java17`.

## Daffodil Setup

If you are testing against a particular version of Daffodil's TDML Runner, make
sure to edit ``build.sbt`` to specify the right Daffodil dependency version. If
you are testing a Daffodil snapshot, make sure to run ``sbt publishLocal`` from
the Daffodil repository and update ``build.sbt`` to match the snapshot version.

## IBM DFDL Setup

Some dependencies are only distributed as part of the IBM DFDL developer
edition, which is part of the IBM App Connect Enterprise (ACE) product.

> This cross test rig was originally created and tested against IBM ACE-11.0.0.1.
> It has since been updated and verified against ACE 12 (12.0.12.17) and ACE 13 (13.0.6.0).

### Quick setup using the PowerShell script (Windows, ACE 12 or 13)

A setup script is included that copies all required jars and sample files
automatically. Run it from the project root:

```powershell
.\setup-ace-jars.ps1                                  # defaults to ACE 12 default path
.\setup-ace-jars.ps1 -AceVersion 13                   # ACE 13 default path
.\setup-ace-jars.ps1 -AcePath "C:\Program Files\IBM\ACE\12.0.12.17" # custom install location
```

### Manual setup

1. Copy the IBM DFDL jars into the ``lib`` subdirectory of this project.

   Also copy ``dfdlsample_java.jar`` from the samples directory into ``lib``,
   as it contains class files used by this test rig.

   On ACE 12/13 the jars are pre-extracted — no unzip step is needed:

   ```
   cp "<ACE_ROOT>\server\dfdl\lib\*.jar" lib\
   cp "<ACE_ROOT>\server\sample\dfdl\dfdlsample_java.jar" lib\
   ```

   The resulting ``lib`` directory looks like this (ACE 12/13):

   ```
   lib
   ├── dfdlsample_java.jar
   ├── ibm-dfdl.jar
   ├── ibm-dfdl-eclipse-dependencies.jar
   ├── icu4j.jar
   ├── icu4j-charset.jar
   ├── icu4j-localespi.jar
   ├── org.eclipse.emf.common-2.30.0.jar
   ├── org.eclipse.emf.ecore-2.36.0.jar
   ├── org.eclipse.emf.ecore.xmi-2.37.0.jar
   ├── org.eclipse.xsd-2.12.0.jar
   └── protobuf-java-3.25.5.jar
   ```

   > **ACE 11 jar names differ.** See `UPGRADE-NOTES.md` for the name mapping
   > between ACE 11 and ACE 12/13.

2. Copy the sample "company" files to ``src/test/resources``:

   ```
   cp "<ACE_ROOT>\server\sample\dfdl\company.*" src\test\resources\
   ```

3. Copy the sample schema file into ``src/test/resources/IBMdefined/``:

   ```
   cp "<ACE_ROOT>\server\sample\dfdl\IBMdefined\RecordSeparatedFieldFormat.xsd" src\test\resources\IBMdefined\
   ```

The files in steps 2 and 3 are examples created by IBM which this test rig will
invoke using TDML to show that everything is working properly.

After completing those steps, the resulting tree under ``src/test/resources``
should look like:

```
src/test/resources/
├── companySelfContained.tdml       - supplied by this cross test rig.
├── company.tdml                    - supplied by this cross test rig.
├── company.txt (copy from IBM DFDL)
├── company.xml (copy from IBM DFDL)
├── company.xsd (copy from IBM DFDL)
├── crossTestRigTestSchema.dfdl.xsd - supplied by this cross test rig, to test the rig itself.
├── crossTestRigTests.tdml          - supplied by this cross test rig, to test the rig itself.
├── SimpleEDI.dfdl.xsd              - supplied by this cross test rig, example EDI schema.
├── SimpleEDITests.tdml             - supplied by this cross test rig, example EDI tests.
├── SimpleEDI-happy.txt             - supplied by this cross test rig, valid test data.
├── SimpleEDI-error.txt             - supplied by this cross test rig, malformed test data.
└── IBMdefined
    └── RecordSeparatedFieldFormat.xsd (copy from IBM DFDL)
```

## Ad-hoc Validation (validate.ps1)

If you just want to parse a data file against a DFDL schema without writing
TDML test files, use the included `validate.ps1` script:

```powershell
.\validate.ps1 -Schema "path\to\MySchema.xsd" -Data "path\to\mydata.txt"
```

The script auto-detects the root element from the schema and prints the parsed
XML infoset on success, or a clear error message on failure. Exit code is 0
on success and non-zero on failure (usable in CI).

```powershell
# Explicit root element (when schema has multiple doc-root candidates)
.\validate.ps1 -Schema "..." -Data "..." -Root "MyRootElement"

# Custom ACE install path
.\validate.ps1 -Schema "..." -Data "..." -AcePath "C:\Program Files\IBM\ACE\12.0.12.17"
```

> The script automatically copies `IBMdefined/RecordSeparatedFieldFormat.xsd`
> next to your schema if the schema imports it via a relative path. Run
> `setup-ace-jars.ps1` first to populate that folder.

See [HOW-TO-TEST.md](HOW-TO-TEST.md) for a step-by-step walkthrough.

## Build & Test

To build and test the cross tester to make sure all dependencies and sample
files have been copied correctly, run the following commands:

```
sbt -java-home "C:\Program Files\IBM\ACE\12.0.12.17\common\java17" compile
sbt -java-home "C:\Program Files\IBM\ACE\12.0.12.17\common\java17" test
```

This runs the cross tester's own self-tests, two versions of the IBM-supplied
example tests, and the example SimpleEDI schema tests. One version of the
company test is with a TDML file, ``company.tdml``, which refers to the separate
files supplied by IBM. The other is an example of a self-contained test. It's
the same test, just with the file contents collapsed into the TDML file. This is
just a useful thing to know about for creating small bug-report TDML files, or
for creating a TDML file that illustrates a non-portability issue.

> See TestIBMDFDLSamples.scala, company.tdml, companySelfContained.tdml.

## Testing Other Test Suites

To use this cross-tester and run other test suites such as those in daffodil
or those for the DFDLSchemas projects on github, follow these steps:

1. Publish the IBM DFDL Cross Tester jar so that other software modules can find
   it, with the following command:

   ```
   sbt -java-home "C:\Program Files\IBM\ACE\12.0.12.17\common\java17" publishLocal
   ```

2. Set up the sbt plugin that makes it easy to use this cross test rig, with the
   following command:

   ```
   mkdir -p ~/.sbt/1.0/plugins/
   cp plugin/ibmDFDLCrossTesterSBTPlugin.scala ~/.sbt/1.0/plugins/
   ```

3. Modify ``~/.sbt/1.0/plugins/ibmDFDLCrossTesterSBTPlugin.scala`` to include
   the path to the lib directory containing the IBM DFDL jars, for example:

   ```scala
   val ibmDFDLJarDirectory = file("/home/user/git/ibmDFDLCrossTester/lib/")
   ```

The above steps allow one to easily run a DFDL schema project against Daffodil
or IBM by changing only one line in the ``build.sbt`` file.

If your build.sbt uses old-style SBT configuration, add this line anywhere in
your file to enable the IBM cross tester:

```
IBMDFDLCrossTesterPlugin.settings
```

If you use the newer SBT config file style, add the settings to your
``project`` like so:

```
    .settings(IBMDFDLCrossTesterPlugin.settings)
```

With the settings added above, the plugin will modify the classpath so that the
IBM DFDL Cross Tester's ibm-tdml-processor will be used instead of the
daffodil-tdml-processor. Then when you run ``sbt test`` in your schema project,
it will use IBM DFDL to run the tests.

Keep in mind the tests must have "ibm" as a member of a TDML testSuite
``defaultImplementations`` attribute, or a TDML parserTestCase or
unparserTestCase ``implementations`` attribute. Otherwise the test is skipped
for that implementation. Tests intended to be portable should list
``defaultImplementations="ibm daffodil"`` (or on the test case,
``implementations="ibm daffodil"``) so that both implementations will attempt
to run the test. A test that works only on one of the implementations
should leave out the other implementation.

If you search the daffodil daffodil-test-ibm1 module for
``defaultImplementations`` or ``implementations`` you'll find examples of how
TDML files do this.
