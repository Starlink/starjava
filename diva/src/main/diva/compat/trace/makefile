#
# $Id: makefile,v 1.9 2002/05/16 21:20:06 johnr Exp $
#
# Makefile for diva.surfaces.trace
#
DIVA  = ../..

include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	BasicTraceRenderer.java \
	TraceController.java \
	TraceModel.java \
	TracePane.java \
	TraceRenderer.java \
	TraceTutorial.java \
	TraceView.java

# Include the shared compilation rules
include $(DIVA)/compile.mk
 
# Tutorial example
TraceTutorial: TraceTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.compat.trace.TraceTutorial


### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
BasicTraceRenderer.class: \
	BasicTraceRenderer.java \
	TraceModel.java \
	TraceRenderer.java \
	../../canvas/AbstractFigure.java \
	../../canvas/AbstractFigureContainer.java \
	../../canvas/CompositeFigure.java \
	../../canvas/Figure.java \
	../../canvas/interactor/ShapedFigure.java \
	../../canvas/toolbox/BasicFigure.java \
	../../canvas/toolbox/BasicRectangle.java

TraceController.class: \
	TraceController.java \
	TraceModel.java \
	TraceView.java \
	../../canvas/FigureDecorator.java \
	../../canvas/GraphicsPane.java \
	../../canvas/event/MouseFilter.java \
	../../canvas/interactor/AbstractInteractor.java \
	../../canvas/interactor/BasicSelectionRenderer.java \
	../../canvas/interactor/CompositeInteractor.java \
	../../canvas/interactor/DragInteractor.java \
	../../canvas/interactor/Interactor.java \
	../../canvas/interactor/Manipulator.java \
	../../canvas/interactor/SelectionDragger.java \
	../../canvas/interactor/SelectionInteractor.java \
	../../canvas/interactor/SelectionModel.java \
	../../canvas/interactor/SelectionRenderer.java

TraceModel.class: \
	TraceModel.java

TracePane.class: \
	TraceController.java \
	TraceModel.java \
	TracePane.java \
	TraceView.java \
	../../canvas/CanvasComponent.java \
	../../canvas/CanvasPane.java \
	../../canvas/FigureLayer.java \
	../../canvas/GraphicsPane.java \
	../../canvas/event/EventAcceptor.java

TraceRenderer.class: \
	TraceModel.java \
	TraceRenderer.java \
	../../canvas/CompositeFigure.java \
	../../canvas/Figure.java

TraceView.class: \
	BasicTraceRenderer.java \
	TraceController.java \
	TraceModel.java \
	TracePane.java \
	TraceRenderer.java \
	TraceView.java \
	../../canvas/AbstractFigure.java \
	../../canvas/AbstractFigureContainer.java \
	../../canvas/CanvasComponent.java \
	../../canvas/CanvasLayer.java \
	../../canvas/CanvasPane.java \
	../../canvas/CompositeFigure.java \
	../../canvas/Figure.java \
	../../canvas/FigureContainer.java \
	../../canvas/FigureLayer.java \
	../../canvas/FigureSet.java \
	../../canvas/GraphicsPane.java \
	../../canvas/VisibleComponent.java \
	../../util/UserObjectContainer.java \
	../../canvas/interactor/ShapedFigure.java \
	../../canvas/toolbox/BasicFigure.java \
	../../canvas/toolbox/LabelFigure.java


