#
# $Id: makefile,v 1.13 2000/07/07 22:17:22 nzamor Exp $
#
# Makefile for diva.canvas.interactor
#
DIVA  = ../..
include $(DIVA)/diva.mk

JSRCS = \
	AbstractInteractor.java \
	ActionInteractor.java \
	BasicGrabHandle.java \
	BasicGrabHandleFactory.java \
	BoundedDragInteractor.java \
	BoundsConstraint.java \
	BoundsGeometry.java \
	BoundsManipulator.java \
	BasicSelectionModel.java \
	BasicSelectionRenderer.java \
	CircleGeometry.java \
	CircleManipulator.java \
	CompositeInteractor.java \
	DragInteractor.java \
	Geometry.java \
	GrabHandle.java \
	GrabHandleFactory.java \
	Interactor.java \
	Manipulator.java \
	MoveHandle.java \
	PathGeometry.java \
	PathManipulator.java \
	PointConstraint.java \
	QuadrantConstraint.java \
	SelectionDragger.java \
	SelectionEvent.java \
	SelectionInteractor.java \
	SelectionListener.java \
	SelectionModel.java \
	SelectionRenderer.java \
	ShapedFigure.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
AbstractInteractor.class: \
	AbstractInteractor.java \
	Interactor.java \
	../event/LayerEvent.java \
	../event/LayerEventMulticaster.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../event/MouseFilter.java

ActionInteractor.class: \
	AbstractInteractor.java \
	ActionInteractor.java \
	Interactor.java \
	../event/LayerEvent.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../event/MouseFilter.java

BasicGrabHandle.class: \
	BasicGrabHandle.java \
	GrabHandle.java \
	ShapedFigure.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../TransformContext.java \
	../VisibleComponent.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../../util/UserObjectContainer.java

BasicGrabHandleFactory.class: \
	BasicGrabHandle.java \
	BasicGrabHandleFactory.java \
	GrabHandle.java \
	GrabHandleFactory.java \
	ShapedFigure.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../../util/UserObjectContainer.java

BoundedDragInteractor.class: \
	AbstractInteractor.java \
	BoundedDragInteractor.java \
	BoundsConstraint.java \
	DragInteractor.java \
	Interactor.java \
	PointConstraint.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../event/LayerEvent.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../../util/UserObjectContainer.java

BoundsConstraint.class: \
	BoundsConstraint.java \
	PointConstraint.java

BoundsGeometry.class: \
	BoundsGeometry.java \
	Geometry.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

BoundsManipulator.class: \
	AbstractInteractor.java \
	BasicGrabHandleFactory.java \
	BoundsGeometry.java \
	BoundsManipulator.java \
	DragInteractor.java \
	Geometry.java \
	GrabHandle.java \
	GrabHandleFactory.java \
	Interactor.java \
	Manipulator.java \
	MoveHandle.java \
	ShapedFigure.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../CanvasUtilities.java \
	../DamageRegion.java \
	../Figure.java \
	../FigureContainer.java \
	../FigureDecorator.java \
	../FigureSet.java \
	../Site.java \
	../VisibleComponent.java \
	../connector/CenterSite.java \
	../event/LayerEvent.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../event/MouseFilter.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../../util/UserObjectContainer.java

BasicSelectionModel.class: \
	AbstractInteractor.java \
	BasicSelectionModel.java \
	CompositeInteractor.java \
	Interactor.java \
	SelectionEvent.java \
	SelectionInteractor.java \
	SelectionListener.java \
	SelectionModel.java \
	SelectionRenderer.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../../util/UserObjectContainer.java

BasicSelectionRenderer.class: \
	BasicSelectionRenderer.java \
	SelectionRenderer.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasComponent.java \
	../Figure.java \
	../FigureContainer.java \
	../FigureDecorator.java \
	../FigureSet.java \
	../VisibleComponent.java \
	../ZList.java \
	../toolbox/BasicHighlighter.java \
	../../util/UserObjectContainer.java

CompositeInteractor.class: \
	AbstractInteractor.java \
	CompositeInteractor.java \
	Interactor.java \
	../event/LayerEvent.java \
	../event/LayerEventMulticaster.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../event/MouseFilter.java \
	../../util/BasicPropertyContainer.java \
	../../util/PropertyContainer.java

DragInteractor.class: \
	AbstractInteractor.java \
	CompositeInteractor.java \
	DragInteractor.java \
	Interactor.java \
	PointConstraint.java \
	SelectionInteractor.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../event/LayerEvent.java \
	../event/LayerEventMulticaster.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../event/MouseFilter.java \
	../../util/ArrayIterator.java \
	../../util/IteratorAdapter.java \
	../../util/UserObjectContainer.java

Geometry.class: \
	Geometry.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

GrabHandle.class: \
	GrabHandle.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

GrabHandleFactory.class: \
	GrabHandle.java \
	GrabHandleFactory.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

Interactor.class: \
	Interactor.java \
	../event/LayerEvent.java \
	../event/LayerEventMulticaster.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../event/MouseFilter.java

Manipulator.class: \
	AbstractInteractor.java \
	BasicGrabHandleFactory.java \
	DragInteractor.java \
	GrabHandle.java \
	GrabHandleFactory.java \
	Interactor.java \
	Manipulator.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasComponent.java \
	../CanvasUtilities.java \
	../DamageRegion.java \
	../Figure.java \
	../FigureContainer.java \
	../FigureDecorator.java \
	../FigureSet.java \
	../VisibleComponent.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../event/MouseFilter.java \
	../../util/UserObjectContainer.java

MoveHandle.class: \
	GrabHandle.java \
	MoveHandle.java \
	ShapedFigure.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../TransformContext.java \
	../VisibleComponent.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../../util/UserObjectContainer.java

PathGeometry.class: \
	Geometry.java \
	PathGeometry.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

PathManipulator.class: \
	AbstractInteractor.java \
	BasicGrabHandleFactory.java \
	DragInteractor.java \
	Geometry.java \
	GrabHandle.java \
	GrabHandleFactory.java \
	Interactor.java \
	Manipulator.java \
	PathGeometry.java \
	PathManipulator.java \
	ShapedFigure.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../CanvasUtilities.java \
	../DamageRegion.java \
	../Figure.java \
	../FigureContainer.java \
	../FigureDecorator.java \
	../FigureSet.java \
	../Site.java \
	../VisibleComponent.java \
	../event/LayerEvent.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../event/MouseFilter.java \
	../../util/UserObjectContainer.java

PointConstraint.class: \
	PointConstraint.java

QuadrantConstraint.class: \
	PointConstraint.java \
	QuadrantConstraint.java

SelectionDragger.class: \
	AbstractInteractor.java \
	CompositeInteractor.java \
	DragInteractor.java \
	Interactor.java \
	SelectionDragger.java \
	SelectionInteractor.java \
	SelectionModel.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../DamageRegion.java \
	../Figure.java \
	../FigureContainer.java \
	../FigureDecorator.java \
	../FigureLayer.java \
	../FigureSet.java \
	../GeometricSet.java \
	../GraphicsPane.java \
	../OverlayLayer.java \
	../VisibleComponent.java \
	../ZList.java \
	../event/EventAcceptor.java \
	../event/EventLayer.java \
	../event/LayerAdapter.java \
	../event/LayerConsumer.java \
	../event/LayerEvent.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../event/MouseFilter.java \
	../../util/CompoundIterator.java \
	../../util/UserObjectContainer.java

SelectionEvent.class: \
	SelectionEvent.java \
	../../util/IteratorAdapter.java \
	../../util/NullIterator.java

SelectionInteractor.class: \
	AbstractInteractor.java \
	BasicSelectionModel.java \
	BasicSelectionRenderer.java \
	CompositeInteractor.java \
	DragInteractor.java \
	Interactor.java \
	Manipulator.java \
	SelectionInteractor.java \
	SelectionModel.java \
	SelectionRenderer.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasComponent.java \
	../Figure.java \
	../FigureContainer.java \
	../FigureDecorator.java \
	../FigureSet.java \
	../VisibleComponent.java \
	../event/LayerEvent.java \
	../event/LayerEventMulticaster.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../event/MouseFilter.java \
	../../util/UserObjectContainer.java

SelectionListener.class: \
	SelectionEvent.java \
	SelectionListener.java

SelectionModel.class: \
	SelectionListener.java \
	SelectionModel.java

SelectionRenderer.class: \
	SelectionRenderer.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

ShapedFigure.class: \
	ShapedFigure.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java


