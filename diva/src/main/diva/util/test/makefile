#
# $Id: makefile,v 1.13 2000/06/30 20:36:40 michaels Exp $
#
# Makefile for diva.util.test
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	All.java \
	ShapeUtilitiesTest.java \
	XMLElementTest.java \
	XMLParserTest.java 

# Miscellaneous files that really should be here
MISC_FILES = xml1.xml xml1.dtd

# Include the shared compilation rules
include $(DIVA)/compile.mk

# The "test" rule is the same as All
test:: All

# Rules for running individual tests
All:
	$(JAVA) -classpath $(CLASSPATH) diva.util.test.All

ShapeUtilities:
	$(JAVA) -classpath $(CLASSPATH) diva.util.test.ShapeUtilitiesTest

XMLElement:
	$(JAVA) -classpath $(CLASSPATH) diva.util.test.XMLElement

XMLParser:
	$(JAVA) -classpath $(CLASSPATH) diva.util.test.XMLParser

