#
# $Id: makefile,v 1.13 2002/05/16 20:37:43 johnr Exp $
#
# Makefile for diva.util.jester
#
DIVA  = ../..
include $(DIVA)/diva.mk

# One subdir to demonstrate test cases
SUBPACKAGES = \
	demo

# Java files in this directory
JSRCS = \
	EventParser.java \
	EventPlayer.java \
	EventRecorder.java \
	EventWriter.java \
	TestCase.java \
	TestFailedException.java \
	TestHarness.java \
	TestSuite.java \
	TestUtilities.java

# Include the shared compilation rules
include $(DIVA)/compile.mk


### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
EventParser.class: \
	EventParser.java \
	EventPlayer.java \
	../LoggableOp.java \
	../ModelParser.java \
	../xml/XmlDocument.java \
	../xml/XmlElement.java \
	../xml/XmlReader.java \
	../../canvas/JCanvas.java \
	../../sketch/JSketch.java

EventPlayer.class: \
	EventPlayer.java

EventRecorder.class: \
	EventRecorder.java

EventWriter.class: \
	EventParser.java \
	EventRecorder.java \
	EventWriter.java \
	../ModelWriter.java \
	../../canvas/JCanvas.java \
	../../sketch/JSketch.java

TestCase.class: \
	TestCase.java \
	TestFailedException.java

TestFailedException.class: \
	TestFailedException.java

TestHarness.class: \
	TestCase.java \
	TestFailedException.java \
	TestHarness.java \
	TestSuite.java

TestSuite.class: \
	TestCase.java \
	TestHarness.java \
	TestSuite.java

TestUtilities.class: \
	TestUtilities.java


