#
# $Id: makefile,v 1.5 2002/05/16 20:37:36 johnr Exp $
#
# Makefile for diva.pod
#
DIVA  = ..
include $(DIVA)/diva.mk

# Subpackage directories
SUBPACKAGES = \
	lwgraph

### JAVAC_FLAGS = -deprecation

# Java files in this directory
JSRCS = \
	Channel.java \
	Delta.java \
	Generator.java \
	Pod.java \
	Protocol.java \
	Receiver.java \
	Transmitter.java \
	View.java

# Include the compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
Channel.class: \
	Channel.java \
	Protocol.java \
	Receiver.java \
	Transmitter.java \
	../util/PropertyContainer.java

Delta.class: \
	Delta.java \
	../util/PropertyContainer.java

Generator.class: \
	Generator.java \
	Protocol.java \
	Transmitter.java \
	../util/PropertyContainer.java

Pod.class: \
	Pod.java \
	Protocol.java \
	Receiver.java \
	Transmitter.java \
	../util/PropertyContainer.java

Protocol.class: \
	Protocol.java \
	../util/PropertyContainer.java

Receiver.class: \
	Channel.java \
	Protocol.java \
	Receiver.java \
	../util/PropertyContainer.java

Transmitter.class: \
	Channel.java \
	Protocol.java \
	Transmitter.java \
	../util/PropertyContainer.java

View.class: \
	Receiver.java \
	View.java \
	../util/PropertyContainer.java


