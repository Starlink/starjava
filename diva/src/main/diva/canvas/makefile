#
# $Id: makefile,v 1.65 2002/05/16 20:37:29 johnr Exp $
#
# Makefile for diva.canvas
#
DIVA  = ..
include $(DIVA)/diva.mk

# Subpackage directories
SUBPACKAGES = \
	connector \
	demo \
	event \
	interactor \
	toolbox \
	tutorial

### JAVAC_FLAGS = -deprecation

# Java files in this directory
JSRCS = \
	AbstractFigure.java \
	AbstractFigureContainer.java \
	AbstractSite.java \
	BasicZList.java \
	CanvasComponent.java \
	CanvasLayer.java \
	CanvasPane.java \
	CanvasDebugUtilities.java \
	CanvasUtilities.java \
	CompositeFigure.java \
	DamageRegion.java \
	Figure.java \
	FigureContainer.java \
	FigureDecorator.java \
	FigureLayer.java \
	FigureSet.java \
	FigureWrapper.java \
	GeometricSet.java \
	GraphicsPane.java \
	JCanvas.java \
	OverlayLayer.java \
	PaneWrapper.java \
	Site.java \
	SiteDecorator.java \
	TransformContext.java \
	VisibleComponent.java \
	ZList.java

# Include the compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
AbstractFigure.class: \
	AbstractFigure.java \
	CanvasComponent.java \
	CanvasLayer.java \
	DamageRegion.java \
	Figure.java \
	TransformContext.java \
	VisibleComponent.java \
	interactor/Interactor.java \
	../util/UserObjectContainer.java

AbstractFigureContainer.class: \
	AbstractFigure.java \
	AbstractFigureContainer.java \
	CanvasComponent.java \
	CanvasUtilities.java \
	DamageRegion.java \
	Figure.java \
	FigureContainer.java \
	FigureDecorator.java \
	FigureSet.java \
	VisibleComponent.java \
	../util/Filter.java \
	../util/FilteredIterator.java \
	../util/UserObjectContainer.java

AbstractSite.class: \
	AbstractSite.java \
	CanvasComponent.java \
	CanvasUtilities.java \
	Figure.java \
	Site.java \
	TransformContext.java \
	VisibleComponent.java \
	../util/UserObjectContainer.java

BasicZList.class: \
	BasicZList.java \
	CanvasComponent.java \
	CanvasUtilities.java \
	Figure.java \
	FigureSet.java \
	GeometricSet.java \
	VisibleComponent.java \
	ZList.java \
	../util/NullIterator.java \
	../util/UserObjectContainer.java

CanvasComponent.class: \
	CanvasComponent.java \
	DamageRegion.java \
	TransformContext.java

CanvasLayer.class: \
	CanvasComponent.java \
	CanvasLayer.java \
	CanvasPane.java \
	DamageRegion.java \
	TransformContext.java \
	event/EventAcceptor.java \
	event/LayerEvent.java

CanvasPane.class: \
	CanvasComponent.java \
	CanvasLayer.java \
	CanvasPane.java \
	DamageRegion.java \
	JCanvas.java \
	TransformContext.java \
	VisibleComponent.java \
	event/EventAcceptor.java \
	event/LayerEvent.java \
	../util/java2d/ShapeUtilities.java

CanvasDebugUtilities.class: \
	CanvasComponent.java \
	CanvasDebugUtilities.java \
	CanvasLayer.java \
	Figure.java \
	FigureLayer.java \
	FigureSet.java \
	TransformContext.java \
	VisibleComponent.java \
	../util/Filter.java \
	../util/FilteredIterator.java \
	../util/UserObjectContainer.java \
	../util/java2d/Polygon2D.java \
	../util/java2d/Polyline2D.java \
	../util/java2d/ShapeUtilities.java

CanvasUtilities.class: \
	CanvasComponent.java \
	CanvasUtilities.java \
	Figure.java \
	FigureContainer.java \
	FigureSet.java \
	Site.java \
	TransformContext.java \
	VisibleComponent.java \
	../util/Filter.java \
	../util/FilteredIterator.java \
	../util/IteratorAdapter.java \
	../util/UserObjectContainer.java \
	../util/java2d/Polygon2D.java \
	../util/java2d/Polyline2D.java \
	../util/java2d/ShapeUtilities.java

CompositeFigure.class: \
	AbstractFigure.java \
	AbstractFigureContainer.java \
	BasicZList.java \
	CanvasComponent.java \
	CanvasUtilities.java \
	CompositeFigure.java \
	DamageRegion.java \
	Figure.java \
	FigureContainer.java \
	FigureSet.java \
	GeometricSet.java \
	TransformContext.java \
	VisibleComponent.java \
	ZList.java \
	../util/Filter.java \
	../util/FilteredIterator.java \
	../util/IteratorAdapter.java \
	../util/UnitIterator.java \
	../util/UserObjectContainer.java \
	../util/java2d/ShapeUtilities.java

DamageRegion.class: \
	DamageRegion.java \
	JCanvas.java \
	TransformContext.java \
	../util/java2d/ShapeUtilities.java

Figure.class: \
	CanvasComponent.java \
	CanvasLayer.java \
	Figure.java \
	VisibleComponent.java \
	interactor/Interactor.java \
	../util/UserObjectContainer.java

FigureContainer.class: \
	CanvasComponent.java \
	Figure.java \
	FigureContainer.java \
	FigureDecorator.java \
	FigureSet.java \
	VisibleComponent.java \
	../util/Filter.java

FigureDecorator.class: \
	AbstractFigure.java \
	AbstractFigureContainer.java \
	CanvasComponent.java \
	Figure.java \
	FigureContainer.java \
	FigureDecorator.java \
	FigureSet.java \
	VisibleComponent.java \
	../util/IteratorAdapter.java \
	../util/NullIterator.java \
	../util/UnitIterator.java \
	../util/UserObjectContainer.java

FigureLayer.class: \
	AbstractFigure.java \
	AbstractFigureContainer.java \
	BasicZList.java \
	CanvasComponent.java \
	CanvasLayer.java \
	CanvasPane.java \
	CanvasUtilities.java \
	DamageRegion.java \
	Figure.java \
	FigureContainer.java \
	FigureDecorator.java \
	FigureLayer.java \
	FigureSet.java \
	GeometricSet.java \
	VisibleComponent.java \
	ZList.java \
	event/EventAcceptor.java \
	event/LayerEvent.java \
	event/LayerListener.java \
	event/LayerMotionListener.java \
	interactor/Interactor.java \
	../util/Filter.java \
	../util/FilteredIterator.java \
	../util/UserObjectContainer.java

FigureSet.class: \
	Figure.java \
	FigureSet.java

FigureWrapper.class: \
	AbstractFigure.java \
	CanvasComponent.java \
	Figure.java \
	FigureWrapper.java \
	VisibleComponent.java \
	../util/NullIterator.java \
	../util/UnitIterator.java \
	../util/UserObjectContainer.java

GeometricSet.class: \
	FigureSet.java \
	GeometricSet.java

GraphicsPane.class: \
	CanvasComponent.java \
	CanvasLayer.java \
	CanvasPane.java \
	FigureLayer.java \
	GraphicsPane.java \
	OverlayLayer.java \
	event/EventAcceptor.java \
	event/EventLayer.java

JCanvas.class: \
	CanvasComponent.java \
	CanvasPane.java \
	CanvasUtilities.java \
	DamageRegion.java \
	GraphicsPane.java \
	JCanvas.java \
	TransformContext.java \
	event/EventAcceptor.java \
	event/LayerEvent.java

OverlayLayer.class: \
	CanvasComponent.java \
	CanvasLayer.java \
	DamageRegion.java \
	OverlayLayer.java \
	VisibleComponent.java

PaneWrapper.class: \
	AbstractFigure.java \
	CanvasComponent.java \
	CanvasPane.java \
	DamageRegion.java \
	Figure.java \
	PaneWrapper.java \
	TransformContext.java \
	VisibleComponent.java \
	event/EventAcceptor.java \
	event/LayerEvent.java \
	interactor/AbstractInteractor.java \
	interactor/CompositeInteractor.java \
	interactor/Interactor.java \
	interactor/SelectionInteractor.java \
	interactor/SelectionModel.java \
	../util/UserObjectContainer.java

Site.class: \
	Figure.java \
	Site.java \
	TransformContext.java

SiteDecorator.class: \
	Figure.java \
	Site.java \
	SiteDecorator.java \
	TransformContext.java

TransformContext.class: \
	CanvasComponent.java \
	TransformContext.java

VisibleComponent.class: \
	CanvasComponent.java \
	VisibleComponent.java

ZList.class: \
	Figure.java \
	FigureSet.java \
	GeometricSet.java \
	ZList.java


