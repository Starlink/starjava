#
# $Id: makefile,v 1.12 2002/05/16 20:37:35 johnr Exp $
#
# Makefile for diva.graph.toolbox
#
DIVA  = ../..
include $(DIVA)/diva.mk

SUBPACKAGES = 

# Java files in this directory
JSRCS = \
	DeletionListener.java \
	GraphEventMulticaster.java \
	GraphLogger.java \
	StateBubble.java \
	TypedNodeRenderer.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
DeletionListener.class: \
	DeletionListener.java \
	../GraphController.java \
	../GraphModel.java \
	../GraphPane.java \
	../JGraph.java \
	../../canvas/CanvasComponent.java \
	../../canvas/CanvasPane.java \
	../../canvas/Figure.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../canvas/VisibleComponent.java \
	../../util/UserObjectContainer.java \
	../../canvas/interactor/SelectionModel.java

GraphEventMulticaster.class: \
	GraphEventMulticaster.java \
	../GraphEvent.java \
	../GraphListener.java

GraphLogger.class: \
	GraphLogger.java \
	../GraphEvent.java \
	../GraphListener.java

StateBubble.class: \
	StateBubble.java \
	../../canvas/AbstractFigure.java \
	../../canvas/CanvasComponent.java \
	../../canvas/Figure.java \
	../../canvas/VisibleComponent.java \
	../../util/UserObjectContainer.java \
	../../canvas/interactor/ShapedFigure.java \
	../../util/java2d/AbstractPaintedGraphic.java \
	../../util/java2d/PaintedShape.java \
	../../util/java2d/ShapeUtilities.java

TypedNodeRenderer.class: \
	TypedNodeRenderer.java \
	../GraphController.java \
	../GraphModel.java \
	../NodeRenderer.java \
	../../canvas/Figure.java


