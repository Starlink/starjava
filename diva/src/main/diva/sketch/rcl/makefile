#
# $Id: makefile,v 1.5 2002/05/16 20:37:40 johnr Exp $
#
# Makefile for diva.canvas.test
#
DIVA  = ../..
include $(DIVA)/diva.mk

JSRCS = \
	BasicStrokeBuilder.java \
	ClassBuilder.java \
	SceneClassBuilder.java \
	StrokeSceneBuilder.java \
	VotingSceneBuilder.java \
	VotingStrokeBuilder.java 

# Include the shared compilation rules
include $(DIVA)/compile.mk



### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
BasicStrokeBuilder.class: \
	BasicStrokeBuilder.java \
	../classification/Classifier.java \
	../recognition/BasicStrokeRecognizer.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java \
	../../util/xml/XmlElement.java

ClassBuilder.class: \
	ClassBuilder.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java \
	../../util/xml/XmlElement.java

SceneClassBuilder.class: \
	SceneClassBuilder.java \
	../recognition/SceneRecognizer.java \
	../recognition/VotingSceneRecognizer.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java \
	../../util/xml/XmlElement.java

StrokeSceneBuilder.class: \
	StrokeSceneBuilder.java \
	../recognition/StrokeRecognizer.java \
	../recognition/StrokeSceneRecognizer.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java \
	../../util/xml/XmlElement.java

VotingSceneBuilder.class: \
	VotingSceneBuilder.java \
	../recognition/SceneRecognizer.java \
	../recognition/VotingSceneRecognizer.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java \
	../../util/xml/XmlElement.java

VotingStrokeBuilder.class: \
	VotingStrokeBuilder.java \
	../recognition/StrokeRecognizer.java \
	../recognition/VotingStrokeRecognizer.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java \
	../../util/xml/XmlElement.java


