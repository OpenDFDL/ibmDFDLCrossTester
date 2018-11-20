ibmDFDLCrossTester

This is a test rig that drives Daffodil TDML tests against IBM DFDL enabling
cross testing of tests easily against both Daffodl and the IBM DFDL implementation.

Requirements

IBM DFDL - a developer edition is available. 
Daffodil - snapshot of 2.3.0 development branch as of at least 2018-11-19 or a newer release

How to Install

Edit the build.sbt file so that the daffodil dependency requests the right version of Daffodil. 
If using a development snapshot, the dependency will end with something like "2.3.0-SNAPSHOT", and 
you must insure that you have a locally published snapshot of that branch.

Download and install the developer edition of IBM. (Tested using ace-11.0.0.1)
In it is a DFDL jar that you can open up.

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

Copy the data files company.xml, company.txt, and company.xsd into src/test/resources.
Copy the schema file IBMdefined/RecordSeparatedFieldFormat.xsd into src/test/resources/IBMdefined.

The resulting tree under src/test/resources will look like:

src/test/resources/
├── companySelfContained.tdml       - supplied by this cross test rig.
├── company.tdml                    - supplied by this cross test rig.
├── company.txt (copy from IBM DFDL)
├── company.xml (copy from IBM DFDL)
├── company.xsd (copy from IBM DFDL)
├── crossTestRigTestSchema.dfdl.xsd - supplied by this cross test rig.
├── crossTestRigTests.tdml          - supplied by this cross test rig.
└── IBMdefined
    └── RecordSeparatedFieldFormat.xsd (copy from IBM DFDL)


Now you should be able to run the example tests using 'sbt' by typing:

  sbt test

In the root directory of the project. 

Create the jar file for the tool by doing

  sbt package
  
Then put the jar onto the class path, along with all jars of the lib directory, and then 
any tests run as part of daffodil can be cross tested against IBM DFDL by just
adding implementations="ibm daffodil" (to test on both),
or for a whole test suite: defaultImplementations="ibm daffodil".

See the company.tdml or company1SelfContained.tdml files for examples of this
TDML syntax.

The Tests

There are tests of the test rig itself.

The "company" example supplied by IBM is provided here by way of showing how to create TDML tests that run against IBM DFDL, Daffodil, or both.

See TestIBMDFDLSamples.scala, company.tdml, companySelfContained.tdml.


