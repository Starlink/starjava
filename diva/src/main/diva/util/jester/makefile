#
# $Id: makefile,v 1.11 2000/09/19 16:37:25 michaels Exp $
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


