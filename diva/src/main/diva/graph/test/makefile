#
# $Id: makefile,v 1.15 2002/01/11 02:31:50 johnr Exp $
#
# Makefile for diva.graph.test
#
DIVA  = ../..
include $(DIVA)/diva.mk

JSRCS = \
	All.java \
	BasicGraphModelTest.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

# The "test" rule is the same as All
test:: All

# Run the sample tests
All: classes
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.graph.test.All

BasicGraphModelTest: BasicGraphModelTest.class
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.graph.test.BasicGraphModelTest
