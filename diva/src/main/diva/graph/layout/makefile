#
# $Id: makefile,v 1.35 2002/05/16 20:37:33 johnr Exp $
#
# Makefile for diva.graph.layout
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	AbstractGlobalLayout.java \
	GlobalLayout.java \
	GridAnnealingLayout.java \
	IncrLayoutAdapter.java \
	IncrementalLayout.java \
	IncrementalLayoutListener.java \
	LayoutTarget.java \
	LayoutUtilities.java \
	LevelLayout.java \
	ProxyIncrLayout.java \
	RandomIncrLayout.java \
	RandomLayout.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
AbstractGlobalLayout.class: \
	AbstractGlobalLayout.java \
	GlobalLayout.java \
	LayoutTarget.java

GlobalLayout.class: \
	GlobalLayout.java \
	LayoutTarget.java

GridAnnealingLayout.class: \
	AbstractGlobalLayout.java \
	GlobalLayout.java \
	GridAnnealingLayout.java \
	LayoutTarget.java \
	LayoutUtilities.java \
	../GraphModel.java

IncrLayoutAdapter.class: \
	GlobalLayout.java \
	IncrLayoutAdapter.java \
	IncrementalLayout.java \
	LayoutTarget.java \
	../GraphEvent.java \
	../GraphModel.java

IncrementalLayout.class: \
	GlobalLayout.java \
	IncrementalLayout.java

IncrementalLayoutListener.class: \
	GlobalLayout.java \
	IncrementalLayout.java \
	IncrementalLayoutListener.java \
	../GraphEvent.java \
	../GraphModel.java \
	../GraphViewEvent.java \
	../GraphViewListener.java \
	../../util/Filter.java

LayoutTarget.class: \
	LayoutTarget.java \
	../GraphModel.java

LayoutUtilities.class: \
	LayoutTarget.java \
	LayoutUtilities.java \
	../GraphModel.java \
	../GraphUtilities.java

LevelLayout.class: \
	AbstractGlobalLayout.java \
	GlobalLayout.java \
	LayoutTarget.java \
	LayoutUtilities.java \
	LevelLayout.java \
	../AbstractGraphModel.java \
	../GraphException.java \
	../GraphModel.java \
	../GraphUtilities.java \
	../MutableGraphModel.java \
	../basic/BasicGraphModel.java \
	../modular/BasicModularGraphModel.java \
	../modular/CompositeNode.java \
	../modular/Edge.java \
	../modular/ModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../modular/Node.java \
	../../util/ArrayIterator.java \
	../../util/IteratorAdapter.java

ProxyIncrLayout.class: \
	GlobalLayout.java \
	IncrementalLayout.java \
	LayoutTarget.java \
	ProxyIncrLayout.java \
	../GraphEvent.java \
	../GraphListener.java

RandomIncrLayout.class: \
	AbstractGlobalLayout.java \
	GlobalLayout.java \
	IncrementalLayout.java \
	LayoutTarget.java \
	LayoutUtilities.java \
	RandomIncrLayout.java \
	RandomLayout.java \
	../GraphEvent.java \
	../GraphModel.java

RandomLayout.class: \
	AbstractGlobalLayout.java \
	GlobalLayout.java \
	LayoutTarget.java \
	LayoutUtilities.java \
	RandomLayout.java \
	../GraphModel.java


