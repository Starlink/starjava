#
# $Id: makefile,v 1.29 2000/05/01 20:19:35 johnr Exp $
#
# Makefile for diva.canvas
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	AbstractConnector.java \
	ArcConnector.java \
	ArcManipulator.java \
	Arrowhead.java \
	AutonomousSite.java \
	BasicManhattanRouter.java \
	Blob.java \
	BoundsSite.java \
	CenterSite.java \
	CenterTarget.java \
	Connector.java \
	ConnectorAdapter.java \
	ConnectorEnd.java \
	ConnectorEvent.java \
	ConnectorInteractor.java \
	ConnectorListener.java \
	ConnectorManipulator.java \
	ConnectorTarget.java \
	FixedNormalSite.java \
	IncrManhattanRouter.java \
	ManhattanConnector.java \
	ManhattanRouter.java \
	NullSite.java \
	ParametricSite.java \
	PerimeterSite.java \
	PerimeterTarget.java \
	Router.java \
	StraightConnector.java \
	StraightTerminal.java \
	Terminal.java \
	TerminalFigure.java

# Include the compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
AbstractConnector.class: \
	AbstractConnector.java \
	Connector.java \
	ConnectorEnd.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../toolbox/LabelFigure.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/PaintedObject.java \
	../../util/java2d/PaintedPath.java

ArcConnector.class: \
	AbstractConnector.java \
	ArcConnector.java \
	Connector.java \
	ConnectorEnd.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../CanvasUtilities.java \
	../Figure.java \
	../Site.java \
	../TransformContext.java \
	../VisibleComponent.java \
	../toolbox/LabelFigure.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/PaintedObject.java \
	../../util/java2d/PaintedPath.java

ArcManipulator.class: \
	ArcManipulator.java \
	ConnectorManipulator.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasComponent.java \
	../Figure.java \
	../FigureContainer.java \
	../FigureDecorator.java \
	../FigureSet.java \
	../Site.java \
	../VisibleComponent.java \
	../event/LayerEvent.java \
	../event/LayerEventMulticaster.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../interactor/BasicGrabHandleFactory.java \
	../interactor/DragInteractor.java \
	../interactor/GrabHandle.java \
	../interactor/GrabHandleFactory.java \
	../interactor/Interactor.java \
	../interactor/Manipulator.java \
	../toolbox/BasicHighlighter.java \
	../../util/UserObjectContainer.java

Arrowhead.class: \
	Arrowhead.java \
	ConnectorEnd.java \
	../Figure.java \
	../Site.java \
	../../util/java2d/Polygon2D.java

AutonomousSite.class: \
	AutonomousSite.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../Figure.java \
	../Site.java \
	../TransformContext.java \
	../VisibleComponent.java \
	../event/EventAcceptor.java \
	../../util/UserObjectContainer.java

BasicManhattanRouter.class: \
	BasicManhattanRouter.java \
	Connector.java \
	ManhattanRouter.java \
	Router.java \
	../CanvasComponent.java \
	../CanvasUtilities.java \
	../Figure.java \
	../Site.java \
	../TransformContext.java \
	../VisibleComponent.java \
	../toolbox/LabelFigure.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/Polyline2D.java

Blob.class: \
	Blob.java \
	ConnectorEnd.java \
	../Figure.java \
	../Site.java \
	../../util/java2d/Polygon2D.java

BoundsSite.class: \
	BoundsSite.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

CenterSite.class: \
	CenterSite.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

CenterTarget.class: \
	AbstractConnectorTarget.java \
	CenterSite.java \
	CenterTarget.java \
	Connector.java \
	ConnectorTarget.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/Filter.java \
	../../util/UserObjectContainer.java

Connector.class: \
	Connector.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

ConnectorAdapter.class: \
	ConnectorAdapter.java \
	ConnectorEvent.java \
	ConnectorListener.java

ConnectorEnd.class: \
	ConnectorEnd.java

ConnectorEvent.class: \
	Connector.java \
	ConnectorEvent.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

ConnectorInteractor.class: \
	AutonomousSite.java \
	Connector.java \
	ConnectorEvent.java \
	ConnectorInteractor.java \
	ConnectorListener.java \
	ConnectorManipulator.java \
	ConnectorTarget.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../CanvasUtilities.java \
	../CompositeFigure.java \
	../Figure.java \
	../FigureContainer.java \
	../FigureDecorator.java \
	../FigureSet.java \
	../Site.java \
	../TransformContext.java \
	../VisibleComponent.java \
	../event/EventAcceptor.java \
	../event/LayerEvent.java \
	../event/LayerEventMulticaster.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../interactor/AbstractInteractor.java \
	../interactor/DragInteractor.java \
	../interactor/GrabHandle.java \
	../interactor/Interactor.java \
	../interactor/Manipulator.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/ShapeUtilities.java

ConnectorListener.class: \
	ConnectorEvent.java \
	ConnectorListener.java

ConnectorManipulator.class: \
	Connector.java \
	ConnectorInteractor.java \
	ConnectorListener.java \
	ConnectorManipulator.java \
	ConnectorTarget.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasComponent.java \
	../Figure.java \
	../FigureContainer.java \
	../FigureDecorator.java \
	../FigureSet.java \
	../Site.java \
	../VisibleComponent.java \
	../event/LayerEvent.java \
	../event/LayerEventMulticaster.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../interactor/AbstractInteractor.java \
	../interactor/BasicGrabHandleFactory.java \
	../interactor/DragInteractor.java \
	../interactor/GrabHandle.java \
	../interactor/GrabHandleFactory.java \
	../interactor/Interactor.java \
	../interactor/Manipulator.java \
	../toolbox/BasicHighlighter.java \
	../../util/UserObjectContainer.java

ConnectorTarget.class: \
	Connector.java \
	ConnectorTarget.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

FixedNormalSite.class: \
	FixedNormalSite.java \
	../Site.java \
	../SiteDecorator.java

IncrManhattanRouter.class: \
	Connector.java \
	IncrManhattanRouter.java \
	ManhattanRouter.java \
	Router.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/Polyline2D.java

ManhattanConnector.class: \
	AbstractConnector.java \
	BasicManhattanRouter.java \
	Connector.java \
	ManhattanConnector.java \
	ManhattanRouter.java \
	Router.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/PaintedObject.java \
	../../util/java2d/PaintedPath.java \
	../../util/java2d/Polyline2D.java

ManhattanRouter.class: \
	Connector.java \
	ManhattanRouter.java \
	Router.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/Polyline2D.java

NullSite.class: \
	NullSite.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

ParametricSite.class: \
	ParametricSite.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

PerimeterSite.class: \
	PerimeterSite.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/Polygon2D.java

PerimeterTarget.class: \
	AbstractConnectorTarget.java \
	Connector.java \
	ConnectorTarget.java \
	PerimeterSite.java \
	PerimeterTarget.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/Filter.java \
	../../util/UserObjectContainer.java

Router.class: \
	Connector.java \
	Router.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

StraightConnector.class: \
	AbstractConnector.java \
	Connector.java \
	ConnectorEnd.java \
	StraightConnector.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../CanvasUtilities.java \
	../Figure.java \
	../Site.java \
	../TransformContext.java \
	../VisibleComponent.java \
	../toolbox/LabelFigure.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/PaintedObject.java \
	../../util/java2d/PaintedPath.java

StraightTerminal.class: \
	ConnectorEnd.java \
	NullSite.java \
	StraightTerminal.java \
	Terminal.java \
	../AbstractFigure.java \
	../AbstractSite.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

Terminal.class: \
	Terminal.java \
	../CanvasComponent.java \
	../Figure.java \
	../Site.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

TerminalFigure.class: \
	Terminal.java \
	TerminalFigure.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../DamageRegion.java \
	../Figure.java \
	../Site.java \
	../TransformContext.java \
	../VisibleComponent.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../interactor/Interactor.java \
	../../util/UserObjectContainer.java


