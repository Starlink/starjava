#
# $Id: makefile,v 1.8 2000/07/03 01:56:59 neuendor Exp $
#
# Makefile for diva.util.xml
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	XmlBuilder.java \
	XmlDemo.java \
	XmlDocument.java \
	XmlElement.java \
	XmlReader.java \
	XmlUtilities.java \
	XmlWriter.java

# Miscellaneous files that really should be here
MISC_FILES = xml1.xml

# Include the shared compilation rules
include $(DIVA)/compile.mk

# Run the "demo"
demo: XmlDemo.class
	$(JAVA) -classpath $(CLASSPATH) diva.util.xml.XmlDemo


### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
XmlBuilder.class: \
	XmlBuilder.java \
	XmlElement.java

XmlDemo.class: \
	XmlDemo.java \
	XmlDocument.java \
	XmlReader.java \
	XmlWriter.java \
	../LoggableOp.java \
	../aelfred/XmlHandler.java

XmlDocument.class: \
	XmlDocument.java \
	XmlElement.java \
	../aelfred/XmlHandler.java \
	../aelfred/XmlParser.java

XmlElement.class: \
	XmlElement.java \
	../Filter.java \
	../FilteredIterator.java \
	../IteratorUtilities.java \
	../aelfred/XmlHandler.java

XmlReader.class: \
	XmlDocument.java \
	XmlElement.java \
	XmlReader.java \
	../LoggableOp.java \
	../aelfred/XmlException.java \
	../aelfred/XmlHandler.java \
	../aelfred/XmlParser.java \
	../../resource/DefaultBundle.java \
	../../resource/RelativeBundle.java

XmlWriter.class: \
	XmlDocument.java \
	XmlElement.java \
	XmlWriter.java \
	../LoggableOp.java \
	../aelfred/XmlParser.java


