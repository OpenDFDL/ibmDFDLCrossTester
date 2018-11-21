ibmDFDLCrossTester

This is a test rig that drives Daffodil TDML tests against IBM DFDL enabling
cross testing of tests easily against both Daffodl and the IBM DFDL implementation.

The purpose of this tool is to help demonstrate interoperability of IBM DFDL and
Daffodil for the overlapping subset of DFDL that both implement. This will drive
convergence of the two implementations so that DFDL users can reliably create 
portable DFDL schemas. 

Similar cross testers can be created for other DFDL implementations as well. 

Requirements

* IBM DFDL - Note that a developer edition is available. 
* Daffodil - Version 2.3.0 

How to Install

Edit the build.sbt file so that the daffodil dependency requests the
right version of Daffodil.

The first released version of Daffodil with support for cross-testing
is 2.3.0.

(Note: As of this writing, version 2.3.0 is not yet released, so a
development snapshot must be used. If using a development snapshot,
the dependency will end with something like "2.3.0-SNAPSHOT" or maybe
"latest.integration". You must insure that you have a locally
published snapshot of that branch - that is, do sbt publishLocal.)

Download and install the developer edition of IBM DFDL.

This is part of the IBM App Connect Enterprise (ACE) product.
This cross test rig was originally created and tested against IBM ACE-11.0.0.1.

In the IBM ACE product you will find a DFDL jar that you can open up.

Copy the IBM DFDL jars into the 'lib' subdirectory of this project.

You must also copy the samples/dfdlsample_java.jar into the 'lib' subdirectory, as that also has class files in it that this test rig uses.  

The resulting directory looks like this.

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

Copying the Samples

Copy the data files company.xml, company.txt, and company.xsd into src/test/resources.
Copy the schema file IBMdefined/RecordSeparatedFieldFormat.xsd into src/test/resources/IBMdefined.

These are examples created by IBM which this test rig will invoke using TDML to show that
everything is working properly.

The resulting tree under src/test/resources will look like:

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

Now you should be able to run the example tests using 'sbt' by typing:

  sbt test

In the root directory of this project. This will run the test-rig's own self-tests, and 
run two versions of the IBM-supplied example tests. One version is with a TDML file, company.tdml,
which refers to the separate files supplied by IBM. 

The other is an example of a self-contained
test. It's the same test, just with the file contents collapsed into the TDML file. This is 
just a useful thing to know about for creating small bug-report TDML files, or for creating
a TDML file that illustrates a non-portability issue.

See TestIBMDFDLSamples.scala, company.tdml, companySelfContained.tdml.

Testing Other Test Suites

To use this cross-tester and run other test suites such as those in daffodil
(for example daffodil-test-ibm1, or daffodil-test), look at the 
build.sbt file of this module, and add new test source directories and
test resource directories as suggested there.



