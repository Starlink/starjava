#
# $Id: makefile,v 1.8 2002/05/19 22:23:14 johnr Exp $
#
# Makefile for diva.graph.modular
#
DIVA  = ../..
include $(DIVA)/diva.mk

SUBPACKAGES = 

# Java files in this directory
JSRCS = \
	BasicModularGraphModel.java \
	CompositeModel.java \
	CompositeNode.java \
	CompositeNodeModel.java \
	Edge.java \
	EdgeModel.java \
	Graph.java \
	ModularGraphModel.java \
	MutableCompositeNodeModel.java \
	MutableEdgeModel.java \
	MutableModularGraphModel.java \
	MutableNodeModel.java \
	Node.java \
	NodeModel.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
BasicModularGraphModel.class: \
	BasicModularGraphModel.java \
	CompositeModel.java \
	CompositeNode.java \
	Edge.java \
	EdgeModel.java \
	Graph.java \
	ModularGraphModel.java \
	MutableCompositeNodeModel.java \
	MutableEdgeModel.java \
	MutableModularGraphModel.java \
	MutableNodeModel.java \
	Node.java \
	NodeModel.java \
	../AbstractGraphModel.java \
	../GraphModel.java \
	../MutableGraphModel.java

CompositeModel.class: \
	CompositeModel.java \
	../../util/PropertyContainer.java \
	../../util/SemanticObjectContainer.java

CompositeNode.class: \
	CompositeNode.java \
	Graph.java \
	Node.java \
	../../util/PropertyContainer.java \
	../../util/SemanticObjectContainer.java

CompositeNodeModel.class: \
	CompositeModel.java \
	CompositeNodeModel.java \
	NodeModel.java \
	../../util/PropertyContainer.java \
	../../util/SemanticObjectContainer.java

Edge.class: \
	Edge.java \
	Node.java \
	../../util/PropertyContainer.java \
	../../util/SemanticObjectContainer.java

EdgeModel.class: \
	EdgeModel.java \
	../../util/PropertyContainer.java \
	../../util/SemanticObjectContainer.java

Graph.class: \
	Graph.java \
	../../util/PropertyContainer.java \
	../../util/SemanticObjectContainer.java

ModularGraphModel.class: \
	CompositeModel.java \
	EdgeModel.java \
	ModularGraphModel.java \
	NodeModel.java \
	../AbstractGraphModel.java \
	../GraphModel.java \
	../../util/PropertyContainer.java \
	../../util/SemanticObjectContainer.java

MutableCompositeNodeModel.class: \
	CompositeModel.java \
	MutableCompositeNodeModel.java \
	MutableNodeModel.java \
	NodeModel.java \
	../../util/PropertyContainer.java \
	../../util/SemanticObjectContainer.java

MutableEdgeModel.class: \
	EdgeModel.java \
	MutableEdgeModel.java \
	../../util/PropertyContainer.java \
	../../util/SemanticObjectContainer.java

MutableModularGraphModel.class: \
	EdgeModel.java \
	ModularGraphModel.java \
	MutableEdgeModel.java \
	MutableModularGraphModel.java \
	MutableNodeModel.java \
	NodeModel.java \
	../AbstractGraphModel.java \
	../GraphEvent.java \
	../GraphModel.java \
	../GraphUtilities.java \
	../MutableGraphModel.java \
	../../util/PropertyContainer.java \
	../../util/SemanticObjectContainer.java

MutableNodeModel.class: \
	MutableNodeModel.java \
	NodeModel.java \
	../../util/PropertyContainer.java \
	../../util/SemanticObjectContainer.java

Node.class: \
	Graph.java \
	Node.java \
	../../util/PropertyContainer.java \
	../../util/SemanticObjectContainer.java

NodeModel.class: \
	NodeModel.java \
	../../util/PropertyContainer.java \
	../../util/SemanticObjectContainer.java


