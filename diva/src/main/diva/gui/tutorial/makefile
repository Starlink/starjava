#
# $Id: makefile,v 1.2 2000/09/15 21:53:18 michaels Exp $
#
# Makefile for diva.gui.tutorial
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	AppletTutorial.java \
	ApplicationTutorial.java \
	MDIApplicationTutorial.java \
	TextDocument.java \
	TextView.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

# The tutorials
AppletTutorial:: classes
	appletviewer AppletTutorial.htm

ApplicationTutorial:: classes
	$(JAVA) -classpath $(CLASSPATH) diva.gui.tutorial.ApplicationTutorial

MDIApplicationTutorial:: classes
	$(JAVA) -classpath $(CLASSPATH) diva.gui.tutorial.MDIApplicationTutorial

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
