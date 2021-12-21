#!/bin/sh
#
# A script to setup symlinks to IBM DFDL libraries and example files
# so that you can 'sbt publishLocal' this cross tester. 
#
# Assumes you have unpacked the IBM DFDL jar per the instructions 
# in the README.md file, and just want to "copy" the pieces into
# this module as those instructions suggest. 
#
# Uses symlinks, not copying, so is dependent on operating systems
# that support symlinks. (You can change it to copy pretty easily
# by replacing 'ln -s' below with 'cp'.)
#
# Once you have run this script, then 'sbt test' should self-test
# the cross tester, and 'sbt publishLocal' should publish the 
# component in your local ".ivy2" cache where managed dependency
# systems (like sbt) can find it. 
#
# Then if you setup the sbt plugin, you can run DFDL schemas against
# IBM DFDL via a one-line change in the build.sbt, as is described
# in the README.md file. 
#

cd ../ibm-dfdl # assumes the ibm libraries are in a peer directory
export IBMDIR=`pwd`
cd -
echo IBM DFDL found at $IBMDIR
 
echo Updating `pwd`/lib as symlink to $IBMDIR/lib
rm -rf lib
ln -s $IBMDIR/lib lib
touch lib/.keep

echo Setting up src/test/resources for example company.* files
cd src/test/resources
ln -s $IBMDIR/src/test/resources/company.txt company.txt
ln -s $IBMDIR/src/test/resources/company.xml company.xml
ln -s $IBMDIR/src/test/resources/company.xsd company.xsd
cd -

echo Setting up src/test/resources/IBMdefined for example DFDL schema
cd src/test/resources/IBMdefined
ln -s $IBMDIR/src/test/resources/IBMdefined/RecordSeparatedFieldFormat.xsd  RecordSeparatedFieldFormat.xsd 
cd -


