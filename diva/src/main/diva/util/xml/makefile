#
# $Id: makefile,v 1.20 2002/07/09 22:00:28 johnr Exp $
#
# Makefile for diva.util.xml
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	AbstractXmlBuilder.java \
	CompositeBuilder.java \
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
AbstractXmlBuilder.class: \
	AbstractXmlBuilder.java

CompositeBuilder.class: \
	CompositeBuilder.java

XmlBuilder.class: \
	XmlBuilder.java

XmlDemo.class: \
	XmlDemo.java

XmlDocument.class: \
	XmlDocument.java

XmlElement.class: \
	XmlElement.java

XmlReader.class: \
	XmlReader.java

XmlUtilities.class: \
	XmlUtilities.java

XmlWriter.class: \
	XmlWriter.java


