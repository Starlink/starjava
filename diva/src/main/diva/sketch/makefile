#
# $Id: makefile,v 1.36 2002/08/12 06:36:58 johnr Exp $
#
# Makefile for diva.sketch
#
DIVA  = ..
include $(DIVA)/diva.mk

SUBPACKAGES = \
	classification \
	demo \
	features \
	multistroke \
	parser2d \
	recognition \
	rcl \
	toolbox \
	$(OPTSKETCHPKG)

# Java files in this directory
JSRCS = \
	BasicInterpreter.java \
	CompositeSymbol.java \
	CheckSelectionAction.java \
	DeletionAction.java \
	JSketch.java \
	LassoSelectionAction.java \
	MultiStateInterpreter.java \
	PenStroke.java \
	SketchController.java \
	SketchEvent.java \
	SketchLayer.java \
	SketchListener.java \
	SketchModel.java \
	SketchPane.java \
	SketchParser.java \
	SketchWriter.java \
	StrokeSymbol.java \
	Symbol.java

# Include the shared compilation rules
include $(DIVA)/compile.mk


### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
BasicInterpreter.class: \
	BasicInterpreter.java \
	SketchController.java \
	SketchLayer.java \
	SketchModel.java \
	SketchPane.java \
	StrokeSymbol.java \
	Symbol.java \
	recognition/TimedStroke.java \
	toolbox/RemoveDupPtsStrokeFilter.java \
	toolbox/StrokeFilter.java \
	../canvas/CanvasLayer.java \
	../canvas/CanvasPane.java \
	../canvas/GraphicsPane.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/LayerListener.java \
	../canvas/event/LayerMotionListener.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/Interactor.java \
	../util/java2d/Polyline2D.java

CompositeSymbol.class: \
	CompositeSymbol.java \
	Symbol.java \
	recognition/TimedStroke.java

CheckSelectionAction.class: \
	BasicInterpreter.java \
	CheckSelectionAction.java \
	SketchController.java \
	recognition/TimedStroke.java \
	../canvas/AbstractFigure.java \
	../canvas/AbstractFigureContainer.java \
	../canvas/CanvasComponent.java \
	../canvas/Figure.java \
	../canvas/FigureContainer.java \
	../canvas/FigureDecorator.java \
	../canvas/FigureSet.java \
	../canvas/VisibleComponent.java \
	../util/UserObjectContainer.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/SelectionModel.java \
	../util/java2d/Polyline2D.java

DeletionAction.class: \
	BasicInterpreter.java \
	DeletionAction.java \
	SketchController.java \
	SketchModel.java \
	Symbol.java \
	recognition/TimedStroke.java \
	../canvas/AbstractFigure.java \
	../canvas/AbstractFigureContainer.java \
	../canvas/CanvasComponent.java \
	../canvas/Figure.java \
	../canvas/FigureContainer.java \
	../canvas/FigureDecorator.java \
	../canvas/FigureSet.java \
	../canvas/VisibleComponent.java \
	../util/UserObjectContainer.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/SelectionModel.java \
	../util/java2d/Polyline2D.java

JSketch.class: \
	JSketch.java \
	SketchPane.java \
	../canvas/CanvasPane.java \
	../canvas/GraphicsPane.java \
	../canvas/JCanvas.java

LassoSelectionAction.class: \
	BasicInterpreter.java \
	LassoSelectionAction.java \
	SketchController.java \
	recognition/TimedStroke.java \
	../canvas/AbstractFigure.java \
	../canvas/AbstractFigureContainer.java \
	../canvas/CanvasComponent.java \
	../canvas/Figure.java \
	../canvas/FigureContainer.java \
	../canvas/FigureDecorator.java \
	../canvas/FigureSet.java \
	../canvas/VisibleComponent.java \
	../util/UserObjectContainer.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/SelectionModel.java \
	../util/java2d/Polygon2D.java \
	../util/java2d/Polyline2D.java

MultiStateInterpreter.class: \
	BasicInterpreter.java \
	MultiStateInterpreter.java \
	SketchController.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/LayerListener.java \
	../canvas/event/LayerMotionListener.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/Interactor.java

PenStroke.class: \
	PenStroke.java \
	../util/java2d/Polygon2D.java \
	../util/java2d/Polyline2D.java

SketchController.class: \
	BasicInterpreter.java \
	CompositeSymbol.java \
	SketchController.java \
	SketchEvent.java \
	SketchLayer.java \
	SketchListener.java \
	SketchModel.java \
	SketchPane.java \
	StrokeSymbol.java \
	Symbol.java \
	recognition/TimedStroke.java \
	../canvas/AbstractFigure.java \
	../canvas/AbstractFigureContainer.java \
	../canvas/BasicZList.java \
	../canvas/CanvasComponent.java \
	../canvas/CanvasLayer.java \
	../canvas/CanvasPane.java \
	../canvas/CompositeFigure.java \
	../canvas/Figure.java \
	../canvas/FigureContainer.java \
	../canvas/FigureDecorator.java \
	../canvas/FigureLayer.java \
	../canvas/FigureSet.java \
	../canvas/GeometricSet.java \
	../canvas/GraphicsPane.java \
	../canvas/Site.java \
	../canvas/VisibleComponent.java \
	../canvas/ZList.java \
	../util/PropertyContainer.java \
	../util/UserObjectContainer.java \
	../canvas/event/EventLayer.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/LayerListener.java \
	../canvas/event/LayerMotionListener.java \
	../canvas/event/MouseFilter.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/BasicSelectionModel.java \
	../canvas/interactor/BoundsManipulator.java \
	../canvas/interactor/CompositeInteractor.java \
	../canvas/interactor/DragInteractor.java \
	../canvas/interactor/GrabHandle.java \
	../canvas/interactor/Interactor.java \
	../canvas/interactor/Manipulator.java \
	../canvas/interactor/SelectionDragger.java \
	../canvas/interactor/SelectionInteractor.java \
	../canvas/interactor/SelectionModel.java \
	../canvas/interactor/ShapedFigure.java \
	../canvas/toolbox/BasicFigure.java \
	../util/java2d/Polygon2D.java \
	../util/java2d/Polyline2D.java \
	../util/java2d/ShapeUtilities.java

SketchEvent.class: \
	SketchEvent.java \
	Symbol.java

SketchLayer.class: \
	SketchLayer.java \
	../canvas/CanvasComponent.java \
	../canvas/CanvasLayer.java \
	../canvas/CanvasPane.java \
	../canvas/JCanvas.java \
	../canvas/event/EventAcceptor.java \
	../util/java2d/AbstractPaintedGraphic.java \
	../util/java2d/PaintedShape.java

SketchListener.class: \
	SketchEvent.java \
	SketchListener.java

SketchModel.class: \
	SketchEvent.java \
	SketchListener.java \
	SketchModel.java \
	Symbol.java

SketchPane.class: \
	SketchController.java \
	SketchLayer.java \
	SketchPane.java \
	../canvas/CanvasComponent.java \
	../canvas/CanvasLayer.java \
	../canvas/CanvasPane.java \
	../canvas/DamageRegion.java \
	../canvas/FigureLayer.java \
	../canvas/GraphicsPane.java \
	../canvas/JCanvas.java \
	../canvas/OverlayLayer.java \
	../util/UnitIterator.java \
	../canvas/event/EventAcceptor.java \
	../canvas/event/EventLayer.java

SketchParser.class: \
	CompositeSymbol.java \
	SketchModel.java \
	SketchParser.java \
	StrokeSymbol.java \
	Symbol.java \
	recognition/SSTrainingParser.java \
	recognition/TimedStroke.java \
	../util/LoggableOp.java \
	../util/ModelParser.java \
	../util/aelfred/HandlerBase.java \
	../util/aelfred/XmlHandler.java \
	../util/xml/XmlDocument.java \
	../util/xml/XmlElement.java

SketchWriter.class: \
	CompositeSymbol.java \
	SketchModel.java \
	SketchParser.java \
	SketchWriter.java \
	StrokeSymbol.java \
	Symbol.java \
	recognition/SSTrainingParser.java \
	recognition/SSTrainingWriter.java \
	recognition/TimedStroke.java \
	../util/ModelWriter.java

StrokeSymbol.class: \
	StrokeSymbol.java \
	Symbol.java \
	recognition/TimedStroke.java

Symbol.class: \
	Symbol.java \
	recognition/TimedStroke.java


