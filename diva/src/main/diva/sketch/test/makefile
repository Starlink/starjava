#
# $Id: makefile,v 1.1 2001/05/28 04:15:46 michaels Exp $
#
# Makefile for diva.canvas.test
#
DIVA  = ../..
include $(DIVA)/diva.mk

JSRCS = \
	All.java \
	JSketchSuite.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

# The "test" rule is the same as All
test:: All

# Run the sample tests
All: classes
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.sketch.test.All

JSketchSuite:
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.sketch.test.JSketchSuite

