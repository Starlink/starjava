#
# $Id: makefile,v 1.16 1999/10/19 01:36:15 johnr Exp $
#
# Makefile for diva.canvas.test
#
DIVA  = ../..
include $(DIVA)/diva.mk

JSRCS = \
	All.java \
	ConcreteFigures.java \
	FigureFactory.java \
	FigureTest.java \
	JCanvasTest.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

# The "test" rule is the same as All
test:: All

# Run the sample tests
All: classes
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.canvas.test.All

JCanvas:
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.canvas.test.JCanvasTest

ConcreteFigures:
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.canvas.test.ConcreteFigures

