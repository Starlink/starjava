#!/bin/sh 
#  Start up the test EchoServer. Note how the various default
#  components of the Axis server are overidden with our versions that
#  do not write tempororay files and make lots of logging output.

java \
  -Daxis.EngineConfigFactory=uk.ac.starlink.soap.AppEngineConfigurationFactory \
  -Daxis.ServerFactory=uk.ac.starlink.soap.AppAxisServerFactory \
  EchoServer >tmp.log 2>&1 &
