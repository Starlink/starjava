#
# $Id: makefile,v 1.13 2000/07/14 23:47:05 hwawen Exp $
#
# Makefile for diva.sketch.demo
#
DIVA  = ../..
include $(DIVA)/diva.mk

SUBPACKAGES = 

# Java files in this directory
JSRCS = SketchDemo.java \
	SketchDemoApplet.java

SketchDemo:	SketchDemo.class
	$(JAVA) -classpath $(CLASSPATH) diva.sketch.demo.SketchDemo


# Include the shared compilation rules
include $(DIVA)/compile.mk


### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE

