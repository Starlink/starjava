#
# $Id: makefile,v 1.2 2002/08/19 07:12:36 johnr Exp $
#
# Makefile for diva.graphx.tutorial
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	SimpleEdgeController.java \
	SimpleGraphController.java \
	SimpleNodeController.java \
	SimpleTutorial.java

# Include the shared compilation rules
include $(DIVA)/compile.mk


SimpleTutorial:		SimpleTutorial.class
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.graphx.tutorial.SimpleTutorial

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
