#
# $Id: makefile,v 1.19 2000/09/22 19:12:22 neuendor Exp $
#
# Makefile for diva.graph.schematic
#
DIVA  = ../..
include $(DIVA)/diva.mk

SUBPACKAGES = 

# Java files in this directory
JSRCS = \
	ContextGraphView.java \
	GraphDocument.java \
	GraphDropTarget.java \
	GraphEditor.java \
	GraphEditorApplet.java \
	GraphView.java \
	MultiWindowGraphEditor.java \
	SchematicGraphController.java \
	SchematicGraphModel.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

GraphEditor:
	$(JAVA) -classpath $(CLASSPATH) diva.graph.schematic.GraphEditor
MultiWindowGraphEditor:
	$(JAVA) -classpath $(CLASSPATH) diva.graph.schematic.MultiWindowGraphEditor

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
