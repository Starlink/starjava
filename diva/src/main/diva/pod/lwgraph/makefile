#
# $Id: makefile,v 1.7 2002/05/16 20:37:37 johnr Exp $
#
# Makefile for diva.pod.lwgraph
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	LightweightGraph.java \
	LightweightNetwork.java \
	BasicLWEdge.java \
	BasicLWNode.java \
	BasicLWPort.java \
	LWEdge.java \
	LWNode.java \
	LWPort.java \
	Topology.java \
	Traversal.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
LightweightGraph.class: \
	LWEdge.java \
	LWNode.java \
	LightweightGraph.java \
	Topology.java \
	Traversal.java \
	../../util/FilteredArrayIterator.java \
	../../util/IteratorAdapter.java \
	../../util/NullArrayIterator.java

LightweightNetwork.class: \
	LWEdge.java \
	LWNode.java \
	LWPort.java \
	LightweightGraph.java \
	LightweightNetwork.java \
	Topology.java \
	../../util/FilteredArrayIterator.java \
	../../util/IteratorAdapter.java

BasicLWEdge.class: \
	BasicLWEdge.java \
	LWEdge.java \
	../../util/PropertyContainer.java

BasicLWNode.class: \
	BasicLWNode.java \
	LWNode.java \
	../../util/PropertyContainer.java

BasicLWPort.class: \
	BasicLWPort.java \
	LWPort.java \
	../../util/PropertyContainer.java

LWEdge.class: \
	LWEdge.java

LWNode.class: \
	LWNode.java

LWPort.class: \
	LWPort.java

Topology.class: \
	Topology.java

Traversal.class: \
	Topology.java \
	Traversal.java


