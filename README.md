# IBM DFDL Cross Tester for Daffodil

This is a test rig that drives TDML tests against IBM DFDL enabling
cross testing of tests easily against both Daffodl and the IBM DFDL implementation.
It uses the TDML Runner library from Daffodil, and complements that with a IBM-specific
TDML processor which drives IBM DFDL from Daffodil's TDML dialect test files.

The purpose of this tool is to help demonstrate interoperability of IBM DFDL and
Daffodil for the overlapping subset of DFDL that both implement. This will drive
convergence of the two implementations so that DFDL users can reliably create
portable DFDL schemas.

Similar cross testers can be created for other DFDL implementations as well.

Requirements

* IBM DFDL - Note that a developer edition is available.
* Daffodil - Version 2.3.0 or newer

Note that Daffodil 3.1.0 or newer is required for TDML parser negative test
cases to work properly. Many DFDL schemas do not have such tests and will work
with older revisions of Daffodil back to 2.3.0.

## Daffodil Setup

If you are testing against a particular version of Daffodil's TDML Runner, make
sure to edit ``build.sbt`` to specify the right Daffodil dependency version. If
you are testing a Daffodil snapshot, make sure to run ``sbt publishLocal`` from
the Daffodil repository and update ``build.sbt`` to match the snapshot version.

## IBM DFDL Setup

Some dependencies are only distributed as part of the IBM DFDL developer
edition, which is part of the IBM App Connect Enterprise (ACE) product.

> This cross test rig was originally created and tested against IBM ACE-11.0.0.1.

In the IBM ACE product you will find a DFDL jar that you can unzip--various
files must be copied out of this jar to enable building and testing the cross
tester.

1. Copy the IBM DFDL jars into the ``lib`` subdirectory of this project.
   
   Note that you must also copy the ``samples/dfdlsample_java.jar`` into the
   ``lib`` subdirectory, as that also has class files in it that this test rig
   uses.

   ```
   cp ~/path/to/IBMDFDL/*.jar lib/
   cp ~/path/to/IBMDFDL/samples/*.jar lib/
   ```
   The resulting directory looks like this.

   ```
   lib
   ├── dfdlsample_java.jar
   ├── emf.common_2.6.0.jar
   ├── emf.ecore_2.6.1.jar
   ├── emf.ecore.xmi_2.5.0.jar
   ├── gpb.jar
   ├── ibm-dfdl.jar
   ├── icu4j-charsets.jar
   ├── icu4j.jar
   ├── scd.jar
   └── xsd_2.6.0.jar
   ```

2. Copy the sample "company" files to ``src/test/resources``

   ```
   cp ~/path/to/IBMDFDL/company.* src/test/resources
   ```

3. Copy the sample schema file into ``src/test/resources/IBMdefined/``

   ```
   cp ~/path/to/IBMDFDL/IBMdefined/RecordSeparatedFieldFormat.xsd  src/test/resources/IBMdefined
   ```

The files in step 2 and 3 are examples created by IBM which this test rig will
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
└── IBMdefined
    └── RecordSeparatedFieldFormat.xsd (copy from IBM DFDL)
```

## Build & Test

To build and test the cross tester to make sure all dependencies and samples
files have been copied correctly, run the following commands:

```
sbt compile
sbt test
```

This runs the cross testers own self-tests and two versions of the IBM-supplied
example tests. One version is with a TDML file, ``company.tdml``, which refers
to the separate files supplied by IBM. The other is an example of a
self-contained test. It's the same test, just with the file contents collapsed
into the TDML file. This is just a useful thing to know about for creating
small bug-report TDML files, or for creating a TDML file that illustrates a
non-portability issue.

> See TestIBMDFDLSamples.scala, company.tdml, companySelfContained.tdml.

## Testing Other Test Suites

To use this cross-tester and run other test suites such as those in daffodil
or those for the DFDLSchemas projects on github, follow these steps:

1. Publish the IBM DFDL Cross Tester jar so hat other software modules can find
   it, with the following command:

   ```
   sbt publishLocal
   ```

2. Setup the sbt plugin that makes it easy to use this cross test rig, with the
   following command:

   ```
   mkdir -p ~/.sbt/1.0/plugins/
   cp plugin/ibmDFDLCrossTesterSBTPlugin.scala ~/.sbt/1.0./plugins/
   ```

3. Modify ``~/.sbt/1.0./plugins/bmDFDLCrossTesterSBTPlugin.scala`` to include
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

If you use the newer SBT config file style, add the settings like your
``project`` like so:

```
    .settings(IBMDFDLCrossTesterPlugin.settings)
```

With the settings added above, he plugin will modify the classpath so that the
IBM DFDL Cross Tester's ibm-tdml-processor will be used instead of the
daffodil-tdml-processor. Then when you run ``sbt test`` in your schema project,
it will use IBM DFDL to run the tests.

Keep in mind the tests must have "ibm" as a member of a TDML testSuite
``defaultImplementations`` attribute, or a TDML parserTestCase or
unparserTestCase ``implementations`` attribute. Otherwise the test is skipped
for that implementation. Tests intended to be portable should list
``defaultImplementations="ibm daffodil"`` (or on the test case,
``implemenations="ibm daffodil``) so that both implementations will attempt
to run the test. A test that works only on one of the implementations
should leave out the other implementation.

If you search the daffodil daffodil-test-ibm1 module for
``defaultImplementations`` or ``implementations`` you'll find examples of how
TDML files do this.
