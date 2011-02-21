#!/bin/sh

#
# First argument must be the path to the workspace
# Second argument is the optional DB configuration Properties file
# Third argument is the input xml file
# Forth argument is the output xml file
#
# The mysql connector will move at some point into the tool set
#

workDir=$1
classPath=${CLASSPATH}:${workDir}/target/pDAQ-1.0.0-SNAPSHOT-dist.dir/bin/trigger-1.0.0-SNAPSHOT-iitrig.jar

echo $classPath

java -cp ${classPath} icecube.daq.trigger.db.DatabaseLoader $2 $3 $4
