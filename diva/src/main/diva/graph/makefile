#
# $Id: makefile,v 1.83 2002/05/16 20:37:32 johnr Exp $
#
# Makefile for diva.graph
#
DIVA  = ..
include $(DIVA)/diva.mk



SUBPACKAGES = \
	basic \
	schematic \
	layout \
	modular \
	toolbox \
	tutorial

# Java files in this directory
JSRCS = \
	AbstractGraphModel.java \
	AbstractGraphController.java \
	BasicNodeController.java \
	BasicEdgeController.java \
	EdgeController.java \
	EdgeCreator.java \
	EdgeInteractor.java \
	EdgeRenderer.java \
	GraphController.java \
	GraphEvent.java \
	GraphException.java \
	GraphListener.java \
	GraphModel.java \
	GraphPane.java \
	GraphUtilities.java \
	GraphViewEvent.java \
	GraphViewListener.java \
	JGraph.java \
	MutableGraphModel.java \
	NodeController.java \
	NodeDragInteractor.java \
	NodeInteractor.java \
	NodeRenderer.java \
	SimpleGraphController.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
AbstractGraphModel.class: \
	AbstractGraphModel.java \
	GraphEvent.java \
	GraphListener.java \
	GraphModel.java \
	toolbox/GraphEventMulticaster.java \
	../util/PropertyContainer.java \
	../util/SemanticObjectContainer.java

AbstractGraphController.class: \
	AbstractGraphController.java \
	EdgeController.java \
	GraphController.java \
	GraphEvent.java \
	GraphListener.java \
	GraphModel.java \
	GraphPane.java \
	GraphUtilities.java \
	GraphViewEvent.java \
	GraphViewListener.java \
	NodeController.java \
	../canvas/CanvasComponent.java \
	../canvas/CanvasPane.java \
	../canvas/Figure.java \
	../canvas/FigureLayer.java \
	../canvas/GraphicsPane.java \
	../canvas/VisibleComponent.java \
	../util/UserObjectContainer.java \
	../canvas/interactor/BasicSelectionModel.java \
	../canvas/interactor/SelectionModel.java

BasicNodeController.class: \
	BasicNodeController.java \
	GraphController.java \
	GraphException.java \
	GraphModel.java \
	GraphPane.java \
	GraphViewEvent.java \
	MutableGraphModel.java \
	NodeController.java \
	NodeInteractor.java \
	NodeRenderer.java \
	../canvas/AbstractFigure.java \
	../canvas/AbstractFigureContainer.java \
	../canvas/CanvasComponent.java \
	../canvas/CanvasLayer.java \
	../canvas/CanvasPane.java \
	../canvas/CanvasUtilities.java \
	../canvas/CompositeFigure.java \
	../canvas/Figure.java \
	../canvas/FigureContainer.java \
	../canvas/FigureLayer.java \
	../canvas/FigureSet.java \
	../canvas/GraphicsPane.java \
	../canvas/Site.java \
	../canvas/VisibleComponent.java \
	../util/Filter.java \
	../util/UserObjectContainer.java \
	../canvas/connector/AutonomousSite.java \
	../canvas/connector/CenterSite.java \
	../canvas/connector/Connector.java \
	../canvas/connector/ConnectorAdapter.java \
	../canvas/connector/ConnectorEvent.java \
	../canvas/connector/ConnectorListener.java \
	../canvas/connector/ConnectorManipulator.java \
	../canvas/connector/ConnectorTarget.java \
	../canvas/connector/PerimeterSite.java \
	../canvas/connector/PerimeterTarget.java \
	../canvas/event/LayerAdapter.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/MouseFilter.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/CompositeInteractor.java \
	../canvas/interactor/DragInteractor.java \
	../canvas/interactor/GrabHandle.java \
	../canvas/interactor/Interactor.java \
	../canvas/interactor/SelectionDragger.java \
	../canvas/interactor/SelectionInteractor.java \
	../canvas/interactor/SelectionModel.java

BasicEdgeController.class: \
	BasicEdgeController.java \
	EdgeController.java \
	EdgeInteractor.java \
	EdgeRenderer.java \
	GraphController.java \
	GraphException.java \
	GraphModel.java \
	GraphPane.java \
	GraphViewEvent.java \
	MutableGraphModel.java \
	../canvas/AbstractFigure.java \
	../canvas/AbstractFigureContainer.java \
	../canvas/AbstractSite.java \
	../canvas/CanvasComponent.java \
	../canvas/CanvasLayer.java \
	../canvas/CanvasPane.java \
	../canvas/CompositeFigure.java \
	../canvas/Figure.java \
	../canvas/FigureContainer.java \
	../canvas/FigureDecorator.java \
	../canvas/FigureLayer.java \
	../canvas/FigureSet.java \
	../canvas/GraphicsPane.java \
	../canvas/Site.java \
	../canvas/TransformContext.java \
	../canvas/VisibleComponent.java \
	../util/Filter.java \
	../util/UserObjectContainer.java \
	../canvas/connector/AbstractConnectorTarget.java \
	../canvas/connector/AutonomousSite.java \
	../canvas/connector/CenterSite.java \
	../canvas/connector/Connector.java \
	../canvas/connector/ConnectorAdapter.java \
	../canvas/connector/ConnectorEvent.java \
	../canvas/connector/ConnectorListener.java \
	../canvas/connector/ConnectorManipulator.java \
	../canvas/connector/ConnectorTarget.java \
	../canvas/connector/PerimeterSite.java \
	../canvas/connector/PerimeterTarget.java \
	../canvas/connector/Terminal.java \
	../canvas/event/EventAcceptor.java \
	../canvas/event/LayerAdapter.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/MouseFilter.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/BasicSelectionRenderer.java \
	../canvas/interactor/CompositeInteractor.java \
	../canvas/interactor/GrabHandle.java \
	../canvas/interactor/Interactor.java \
	../canvas/interactor/Manipulator.java \
	../canvas/interactor/SelectionDragger.java \
	../canvas/interactor/SelectionInteractor.java \
	../canvas/interactor/SelectionModel.java \
	../canvas/interactor/SelectionRenderer.java

EdgeController.class: \
	EdgeController.java \
	EdgeRenderer.java \
	GraphController.java \
	GraphModel.java \
	MutableGraphModel.java \
	../canvas/CanvasComponent.java \
	../canvas/CompositeFigure.java \
	../canvas/Figure.java \
	../canvas/FigureLayer.java \
	../canvas/GraphicsPane.java \
	../canvas/Site.java \
	../util/Filter.java \
	../canvas/connector/AutonomousSite.java \
	../canvas/connector/CenterSite.java \
	../canvas/connector/Connector.java \
	../canvas/connector/ConnectorAdapter.java \
	../canvas/connector/ConnectorEvent.java \
	../canvas/connector/ConnectorListener.java \
	../canvas/connector/ConnectorManipulator.java \
	../canvas/connector/ConnectorTarget.java \
	../canvas/connector/PerimeterSite.java \
	../canvas/connector/PerimeterTarget.java \
	../canvas/connector/Terminal.java \
	../canvas/event/LayerAdapter.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/MouseFilter.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/BasicSelectionRenderer.java \
	../canvas/interactor/GrabHandle.java \
	../canvas/interactor/Interactor.java \
	../canvas/interactor/SelectionDragger.java \
	../canvas/interactor/SelectionInteractor.java \
	../canvas/interactor/SelectionModel.java

EdgeCreator.class: \
	EdgeCreator.java \
	GraphController.java \
	../canvas/AbstractFigure.java \
	../canvas/AbstractFigureContainer.java \
	../canvas/CanvasComponent.java \
	../canvas/CanvasLayer.java \
	../canvas/Figure.java \
	../canvas/FigureContainer.java \
	../canvas/FigureDecorator.java \
	../canvas/FigureLayer.java \
	../canvas/FigureSet.java \
	../canvas/GraphicsPane.java \
	../canvas/Site.java \
	../canvas/VisibleComponent.java \
	../util/Filter.java \
	../util/UserObjectContainer.java \
	../canvas/connector/ConnectorEvent.java \
	../canvas/connector/ConnectorManipulator.java \
	../canvas/event/LayerAdapter.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/LayerListener.java \
	../canvas/event/LayerMotionListener.java \
	../canvas/event/MouseFilter.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/CompositeInteractor.java \
	../canvas/interactor/GrabHandle.java \
	../canvas/interactor/Interactor.java \
	../canvas/interactor/Manipulator.java \
	../canvas/interactor/SelectionDragger.java \
	../canvas/interactor/SelectionInteractor.java \
	../canvas/interactor/SelectionModel.java

EdgeInteractor.class: \
	EdgeInteractor.java \
	../canvas/event/LayerListener.java \
	../canvas/event/LayerMotionListener.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/CompositeInteractor.java \
	../canvas/interactor/Interactor.java \
	../canvas/interactor/SelectionInteractor.java \
	../canvas/interactor/SelectionModel.java

EdgeRenderer.class: \
	EdgeRenderer.java \
	../canvas/Site.java \
	../canvas/connector/Connector.java

GraphController.class: \
	EdgeController.java \
	GraphController.java \
	GraphModel.java \
	GraphPane.java \
	GraphViewEvent.java \
	GraphViewListener.java \
	NodeController.java \
	../canvas/Figure.java \
	../canvas/interactor/SelectionModel.java

GraphEvent.class: \
	GraphEvent.java

GraphException.class: \
	GraphException.java

GraphListener.class: \
	GraphEvent.java \
	GraphListener.java

GraphModel.class: \
	GraphEvent.java \
	GraphListener.java \
	GraphModel.java \
	../util/PropertyContainer.java \
	../util/SemanticObjectContainer.java

GraphPane.class: \
	GraphController.java \
	GraphModel.java \
	GraphPane.java \
	../canvas/CanvasComponent.java \
	../canvas/CanvasPane.java \
	../canvas/FigureLayer.java \
	../canvas/GraphicsPane.java \
	../canvas/event/EventAcceptor.java

GraphUtilities.class: \
	GraphException.java \
	GraphModel.java \
	GraphUtilities.java \
	MutableGraphModel.java \
	../util/CompoundIterator.java \
	../util/Filter.java \
	../util/FilteredIterator.java \
	../util/IteratorAdapter.java \
	../util/ProxyIterator.java

GraphViewEvent.class: \
	GraphViewEvent.java

GraphViewListener.class: \
	GraphViewEvent.java \
	GraphViewListener.java

JGraph.class: \
	GraphPane.java \
	JGraph.java \
	../canvas/CanvasPane.java \
	../canvas/GraphicsPane.java \
	../canvas/JCanvas.java

MutableGraphModel.class: \
	GraphException.java \
	GraphModel.java \
	MutableGraphModel.java \
	../util/PropertyContainer.java \
	../util/SemanticObjectContainer.java

NodeController.class: \
	GraphController.java \
	GraphModel.java \
	MutableGraphModel.java \
	NodeController.java \
	NodeRenderer.java \
	../canvas/CanvasComponent.java \
	../canvas/CanvasUtilities.java \
	../canvas/CompositeFigure.java \
	../canvas/Figure.java \
	../canvas/FigureLayer.java \
	../canvas/GraphicsPane.java \
	../canvas/Site.java \
	../util/Filter.java \
	../canvas/connector/AutonomousSite.java \
	../canvas/connector/CenterSite.java \
	../canvas/connector/Connector.java \
	../canvas/connector/ConnectorAdapter.java \
	../canvas/connector/ConnectorEvent.java \
	../canvas/connector/ConnectorListener.java \
	../canvas/connector/ConnectorManipulator.java \
	../canvas/connector/ConnectorTarget.java \
	../canvas/connector/PerimeterSite.java \
	../canvas/connector/PerimeterTarget.java \
	../canvas/event/LayerAdapter.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/MouseFilter.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/GrabHandle.java \
	../canvas/interactor/Interactor.java \
	../canvas/interactor/SelectionDragger.java \
	../canvas/interactor/SelectionInteractor.java \
	../canvas/interactor/SelectionModel.java

NodeDragInteractor.class: \
	GraphController.java \
	GraphModel.java \
	GraphUtilities.java \
	NodeDragInteractor.java \
	../canvas/CanvasComponent.java \
	../canvas/Figure.java \
	../canvas/TransformContext.java \
	../canvas/VisibleComponent.java \
	../util/UserObjectContainer.java \
	../canvas/connector/Connector.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/LayerListener.java \
	../canvas/event/LayerMotionListener.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/DragInteractor.java \
	../canvas/interactor/Interactor.java \
	../canvas/interactor/SelectionModel.java

NodeInteractor.class: \
	GraphController.java \
	NodeDragInteractor.java \
	NodeInteractor.java \
	../canvas/Figure.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/LayerListener.java \
	../canvas/event/LayerMotionListener.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/CompositeInteractor.java \
	../canvas/interactor/DragInteractor.java \
	../canvas/interactor/Interactor.java \
	../canvas/interactor/SelectionInteractor.java \
	../canvas/interactor/SelectionModel.java

NodeRenderer.class: \
	NodeRenderer.java \
	../canvas/Figure.java

SimpleGraphController.class: \
	AbstractGraphController.java \
	EdgeController.java \
	GraphController.java \
	NodeController.java \
	SimpleGraphController.java \
	../canvas/Figure.java \
	../canvas/FigureLayer.java \
	../canvas/GraphicsPane.java \
	../canvas/Site.java \
	../util/Filter.java \
	../canvas/connector/AutonomousSite.java \
	../canvas/connector/CenterSite.java \
	../canvas/connector/Connector.java \
	../canvas/connector/ConnectorAdapter.java \
	../canvas/connector/ConnectorEvent.java \
	../canvas/connector/ConnectorListener.java \
	../canvas/connector/ConnectorManipulator.java \
	../canvas/connector/ConnectorTarget.java \
	../canvas/connector/PerimeterSite.java \
	../canvas/connector/PerimeterTarget.java \
	../canvas/event/LayerAdapter.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/MouseFilter.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/GrabHandle.java \
	../canvas/interactor/Interactor.java \
	../canvas/interactor/SelectionDragger.java \
	../canvas/interactor/SelectionInteractor.java \
	../canvas/interactor/SelectionModel.java


