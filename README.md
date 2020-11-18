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
* Daffodil - Version 2.3.0

Note that Daffodil 3.1.0-SNAPSHOT git hash xyzzy or later are necessary for
TDML parser negative test cases to work properly. Many DFDL schemas do not have
such tests and will work with older revisions of Daffodil back to 2.3.0.  

How to Install

Edit the build.sbt file so that the daffodil dependency requests the
right version of Daffodil.

The first released version of Daffodil with support for cross-testing
is 2.3.0.

Download and install the developer edition of IBM DFDL.

This is part of the IBM App Connect Enterprise (ACE) product.
This cross test rig was originally created and tested against IBM ACE-11.0.0.1.

In the IBM ACE product you will find a DFDL jar that you can open up.

Copy the IBM DFDL jars into the 'lib' subdirectory of this project.

You must also copy the samples/dfdlsample_java.jar into the 'lib' subdirectory, as that also has class files in it that this test rig uses.  

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

Copying the Samples

Copy the data files company.xml, company.txt, and company.xsd into src/test/resources.
Copy the schema file IBMdefined/RecordSeparatedFieldFormat.xsd into src/test/resources/IBMdefined.

These are examples created by IBM which this test rig will invoke using TDML to show that
everything is working properly.

The resulting tree under src/test/resources will look like:

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

Now you should be able to run the example tests using 'sbt' by typing:

  `sbt test`

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
or those for the DFDLSchemas projects on github, you must publish the Jar file for it, so 
that other software modules can find it. To do this issue the command:

  `sbt publishLocal`
     
You must also setup the sbt plugin that makes it easy to use this cross test rig. 
To do this, copy the file plugin/ibmDFDLCrossTesterSBTPlugin.scala into 
your `~/.sbt/1.0/plugins` directory, and edit it
to provide the path name to the "lib" directory (see comments in the file). 

This sbt plugin enables one to easily run a DFDL schema project against Daffodil
or IBM by changing only one line in the build.sbt file.

This is perhaps best understood by example. See the build.sbt file in the DFDLSchemas
NACHA schema. You'll find a comment like this:

```
    ).
    settings(nachaSettings)
    //
    // Uncomment this line below to run against IBM DFDL.
    // You need to have IBM DFDL installed and the IBM DFDL Cross Tester
    // 
    //.settings(IBMDFDLCrossTesterPlugin.settings)
```

If you uncomment that final line, the plugin will modify the classpath so that
the IBM DFDL Cross Tester's ibm-tdml-processor will be used instead of the daffodil-tdml-processor. Then when you run 'sbt test' it will use IBM DFDL to run the tests.
Keep in mind the tests must have "ibm" as a member of a TDML testSuite defaultImplementations attribute, or a TDML parserTestCase or unparserTestCase implementations attribute. Otherwise the test is skipped for that implementation. Tests intended to be portable should list defaultImplementations="ibm daffodil" (or on the test case, implemenations="ibm daffodil") so that both implementations will attempt to run the test. A test that works only on one of the implementations should leave out the other implementation. 

If you search the daffodil daffodil-test-ibm1 module for "defaultImplementations" or "implementations" you'll find examples of how TDML files do this.





