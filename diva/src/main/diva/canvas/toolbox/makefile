#
# $Id: makefile,v 1.43 2002/09/26 10:26:52 johnr Exp $
#
# Makefile for diva.canvas.toolbox
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	BasicCanvasPane.java \
	BasicController.java \
	BasicEllipse.java \
	BasicFigure.java \
	BasicHighlighter.java \
	BasicRectangle.java \
	GraphicsParser.java \
	GridLayer.java \
	IconFigure.java \
	ImageFigure.java \
	LabelFigure.java \
	LabelWrapper.java \
	PathFigure.java \
	PaintedFigure.java \
	SVGParser.java \
	SVGUtilities.java \
	SwingWrapper.java \
	TypedDecorator.java \
	VectorFigure.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
BasicCanvasPane.class: \
	BasicCanvasPane.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../Figure.java \
	../FigureLayer.java \
	../event/EventAcceptor.java

BasicController.class: \
	BasicController.java \
	../FigureDecorator.java \
	../GraphicsPane.java \
	../event/MouseFilter.java \
	../interactor/AbstractInteractor.java \
	../interactor/BasicSelectionRenderer.java \
	../interactor/CompositeInteractor.java \
	../interactor/DragInteractor.java \
	../interactor/Interactor.java \
	../interactor/Manipulator.java \
	../interactor/SelectionDragger.java \
	../interactor/SelectionInteractor.java \
	../interactor/SelectionModel.java \
	../interactor/SelectionRenderer.java

BasicEllipse.class: \
	BasicEllipse.java \
	BasicFigure.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../interactor/ShapedFigure.java \
	../../util/UserObjectContainer.java

BasicFigure.class: \
	BasicFigure.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../interactor/ShapedFigure.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/ShapeUtilities.java

BasicHighlighter.class: \
	BasicHighlighter.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasComponent.java \
	../DamageRegion.java \
	../Figure.java \
	../FigureContainer.java \
	../FigureDecorator.java \
	../FigureSet.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

BasicRectangle.class: \
	BasicFigure.java \
	BasicRectangle.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../interactor/ShapedFigure.java \
	../../util/UserObjectContainer.java

GraphicsParser.class: \
	GraphicsParser.java \
	../../util/java2d/AbstractPaintedGraphic.java \
	../../util/java2d/PaintedGraphic.java \
	../../util/java2d/PaintedList.java \
	../../util/java2d/PaintedObject.java \
	../../util/java2d/PaintedPath.java \
	../../util/java2d/PaintedShape.java \
	../../util/java2d/PaintedString.java \
	../../util/java2d/Polygon2D.java \
	../../util/java2d/Polyline2D.java

GridLayer.class: \
	GridLayer.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../VisibleComponent.java \
	../event/EventAcceptor.java

IconFigure.class: \
	IconFigure.java \
	LabelFigure.java \
	../AbstractFigure.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../connector/BoundsSite.java \
	../connector/Terminal.java \
	../interactor/ShapedFigure.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/PaintedObject.java \
	../../util/java2d/ShapeUtilities.java

ImageFigure.class: \
	ImageFigure.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

LabelFigure.class: \
	LabelFigure.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../CanvasUtilities.java \
	../Figure.java \
	../TransformContext.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/ShapeUtilities.java

LabelWrapper.class: \
	LabelFigure.java \
	LabelWrapper.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../CanvasUtilities.java \
	../Figure.java \
	../VisibleComponent.java \
	../../util/NullIterator.java \
	../../util/UnitIterator.java \
	../../util/UserObjectContainer.java

PathFigure.class: \
	PathFigure.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../interactor/ShapedFigure.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/ShapeUtilities.java

PaintedFigure.class: \
	PaintedFigure.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../interactor/ShapedFigure.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/PaintedList.java \
	../../util/java2d/PaintedObject.java \
	../../util/java2d/PaintedShape.java \
	../../util/java2d/ShapeUtilities.java

SVGParser.class: \
	SVGParser.java \
	../../util/java2d/AbstractPaintedGraphic.java \
	../../util/java2d/PaintedGraphic.java \
	../../util/java2d/PaintedImage.java \
	../../util/java2d/PaintedList.java \
	../../util/java2d/PaintedObject.java \
	../../util/java2d/PaintedPath.java \
	../../util/java2d/PaintedShape.java \
	../../util/java2d/PaintedString.java \
	../../util/java2d/Polygon2D.java \
	../../util/java2d/Polyline2D.java \
	../../util/xml/XmlElement.java

SwingWrapper.class: \
	SwingWrapper.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../Figure.java \
	../JCanvas.java \
	../VisibleComponent.java \
	../event/EventAcceptor.java \
	../../util/UserObjectContainer.java

TypedDecorator.class: \
	BasicHighlighter.java \
	TypedDecorator.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasComponent.java \
	../Figure.java \
	../FigureContainer.java \
	../FigureDecorator.java \
	../FigureSet.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

VectorFigure.class: \
	VectorFigure.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../Figure.java \
	../TransformContext.java \
	../VisibleComponent.java \
	../interactor/ShapedFigure.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/ShapeUtilities.java


