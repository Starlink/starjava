#
# $Id: makefile,v 1.4 2002/01/15 05:06:23 johnr Exp $
#
# Makefile for diva.canvas.test
#
DIVA  = ../..
include $(DIVA)/diva.mk

JSRCS = \
	All.java \
	LightweightGraphTest.java \
	LightweightNetworkTest.java \
	TopologyTest.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

# The "test" rule is the same as All
test:: All

# Run the sample tests
All: classes
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.pod.test.All

LightweightGraphTest: LightweightGraphTest.class
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.pod.test.LightweightGraphTest

LightweightNetworkTest: LightweightNetworkTest.class
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.pod.test.LightweightNetworkTest

TopologyTest: TopologyTest.class
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.pod.test.TopologyTest
