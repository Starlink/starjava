#
# $Id: makefile,v 1.28 2002/05/16 20:37:35 johnr Exp $
#
# Makefile for diva.graph.tutorial
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	EditorTutorial.java \
	PrepopulatedTutorial.java \
	NodeRendererTutorial.java \
	NestedTutorial.java \
	SimpleTutorial.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

EditorTutorial:	EditorTutorial.class
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.graph.tutorial.EditorTutorial

NodeRendererTutorial:	NodeRendererTutorial.class
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.graph.tutorial.NodeRendererTutorial

PrepopulatedTutorial:	PrepopulatedTutorial.class
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.graph.tutorial.PrepopulatedTutorial

NestedTutorial:	NestedTutorial.class
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.graph.tutorial.NestedTutorial

SimpleTutorial:		SimpleTutorial.class
	CLASSPATH=$(CLASSPATH) $(JAVA) diva.graph.tutorial.SimpleTutorial

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
EditorTutorial.class: \
	EditorTutorial.java \
	../AbstractGraphController.java \
	../AbstractGraphModel.java \
	../GraphController.java \
	../GraphModel.java \
	../GraphPane.java \
	../JGraph.java \
	../MutableGraphModel.java \
	../SimpleGraphController.java \
	../basic/BasicGraphController.java \
	../basic/BasicGraphModel.java \
	../basic/BasicLayoutTarget.java \
	../layout/AbstractGlobalLayout.java \
	../layout/GlobalLayout.java \
	../layout/LayoutTarget.java \
	../layout/LevelLayout.java \
	../layout/RandomLayout.java \
	../modular/BasicModularGraphModel.java \
	../modular/ModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../../canvas/CanvasPane.java \
	../../canvas/FigureLayer.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../gui/AppContext.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java

PrepopulatedTutorial.class: \
	PrepopulatedTutorial.java \
	../AbstractGraphController.java \
	../AbstractGraphModel.java \
	../GraphController.java \
	../GraphModel.java \
	../GraphPane.java \
	../GraphViewListener.java \
	../JGraph.java \
	../MutableGraphModel.java \
	../SimpleGraphController.java \
	../basic/BasicGraphController.java \
	../basic/BasicGraphModel.java \
	../basic/BasicLayoutTarget.java \
	../layout/AbstractGlobalLayout.java \
	../layout/GlobalLayout.java \
	../layout/IncrLayoutAdapter.java \
	../layout/IncrementalLayout.java \
	../layout/IncrementalLayoutListener.java \
	../layout/LayoutTarget.java \
	../layout/LevelLayout.java \
	../layout/RandomLayout.java \
	../modular/BasicModularGraphModel.java \
	../modular/CompositeNode.java \
	../modular/Edge.java \
	../modular/ModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../modular/Node.java \
	../../canvas/CanvasPane.java \
	../../canvas/FigureLayer.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../gui/AppContext.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java

NodeRendererTutorial.class: \
	NodeRendererTutorial.java \
	../AbstractGraphController.java \
	../AbstractGraphModel.java \
	../GraphController.java \
	../GraphModel.java \
	../GraphPane.java \
	../JGraph.java \
	../MutableGraphModel.java \
	../NodeController.java \
	../NodeRenderer.java \
	../SimpleGraphController.java \
	../basic/BasicGraphController.java \
	../basic/BasicGraphModel.java \
	../basic/BasicLayoutTarget.java \
	../basic/BasicNodeRenderer.java \
	../layout/AbstractGlobalLayout.java \
	../layout/LayoutTarget.java \
	../layout/LevelLayout.java \
	../modular/BasicModularGraphModel.java \
	../modular/Edge.java \
	../modular/Graph.java \
	../modular/ModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../modular/Node.java \
	../toolbox/TypedNodeRenderer.java \
	../../canvas/CanvasPane.java \
	../../canvas/FigureLayer.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../gui/AppContext.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java

NestedTutorial.class: \
	NestedTutorial.java \
	../AbstractGraphController.java \
	../AbstractGraphModel.java \
	../GraphController.java \
	../GraphModel.java \
	../GraphPane.java \
	../JGraph.java \
	../MutableGraphModel.java \
	../SimpleGraphController.java \
	../basic/BasicGraphController.java \
	../basic/BasicGraphModel.java \
	../basic/BasicLayoutTarget.java \
	../layout/AbstractGlobalLayout.java \
	../layout/LayoutTarget.java \
	../layout/RandomLayout.java \
	../modular/BasicModularGraphModel.java \
	../modular/CompositeNode.java \
	../modular/Edge.java \
	../modular/ModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../modular/Node.java \
	../../canvas/CanvasPane.java \
	../../canvas/FigureLayer.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../gui/AppContext.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java

SimpleTutorial.class: \
	SimpleTutorial.java \
	../AbstractGraphController.java \
	../AbstractGraphModel.java \
	../GraphController.java \
	../GraphModel.java \
	../GraphPane.java \
	../JGraph.java \
	../MutableGraphModel.java \
	../SimpleGraphController.java \
	../basic/BasicGraphController.java \
	../basic/BasicGraphModel.java \
	../modular/BasicModularGraphModel.java \
	../modular/ModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../../canvas/CanvasPane.java \
	../../canvas/FigureLayer.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../gui/AppContext.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java


