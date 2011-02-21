#!/bin/sh

#
# First argument must be the path to the workspace
# Second argument is the optional DB configuration Properties file
#
# The mysql connector will move at some point into the tool set
#

workDir=$1
classPath=${CLASSPATH}:${workDir}/target/pDAQ-1.0.0-SNAPSHOT-dist.dir/bin/trigger-1.0.0-SNAPSHOT-iitrig.jar

echo $classPath

java -cp ${classPath} icecube.daq.trigger.db.TriggerDatabaseUtil $2
