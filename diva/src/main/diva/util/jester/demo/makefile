#
# $Id: makefile,v 1.4 2002/05/16 20:37:43 johnr Exp $
#
# Makefile for diva.util.jester.demo
#
DIVA  = ../../..
include $(DIVA)/diva.mk

# Subpackage directories
SUBDIRS = 

# Java files in this directory
JSRCS = \
	Foo.java \
	Bar.java \
	FooTest.java \
	BarTest.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

# Run the sample tests
Foo:
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.util.jester.demo.FooTest

Bar:
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.util.jester.demo.BarTest


### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
Foo.class: \
	Foo.java

Bar.class: \
	Bar.java \
	Foo.java

FooTest.class: \
	Foo.java \
	FooTest.java \
	../TestCase.java \
	../TestFailedException.java \
	../TestHarness.java \
	../TestSuite.java

BarTest.class: \
	Bar.java \
	BarTest.java \
	Foo.java \
	FooTest.java \
	../TestCase.java \
	../TestFailedException.java \
	../TestHarness.java \
	../TestSuite.java


