#
# $Id: makefile,v 1.9 2002/08/13 09:32:45 johnr Exp $
#
# Makefile for diva.graph.basic
#
DIVA  = ../..
include $(DIVA)/diva.mk

SUBPACKAGES = 

# Java files in this directory
JSRCS = \
	ArcRenderer.java \
	BasicEdgeRenderer.java \
	BasicGraphController.java \
	BasicGraphDemo.java \
	BasicGraphDemoApplet.java \
	BasicGraphModel.java \
	BasicLayoutTarget.java \
	BasicNodeRenderer.java \
	BubbleGraphController.java \
	BubbleGraphDemo.java \
	BubbleGraphDemoApplet.java \
	BubblePane.java \
	BubbleRenderer.java



# Include the shared compilation rules
include $(DIVA)/compile.mk

BasicGraphDemo:
	$(JAVA) -classpath $(CLASSPATH) diva.graph.basic.BasicGraphDemo

BubbleGraphDemo:
	$(JAVA) -classpath $(CLASSPATH) diva.graph.basic.BubbleGraphDemo


### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
ArcRenderer.class: \
	ArcRenderer.java \
	../EdgeRenderer.java \
	../../canvas/AbstractFigure.java \
	../../canvas/Site.java \
	../../canvas/connector/AbstractConnector.java \
	../../canvas/connector/ArcConnector.java \
	../../canvas/connector/Arrowhead.java \
	../../canvas/connector/Connector.java \
	../../canvas/connector/ConnectorEnd.java \
	../../canvas/connector/StraightConnector.java \
	../../canvas/toolbox/LabelFigure.java

BasicEdgeRenderer.class: \
	BasicEdgeRenderer.java \
	../EdgeRenderer.java \
	../../canvas/AbstractFigure.java \
	../../canvas/CanvasComponent.java \
	../../canvas/Figure.java \
	../../canvas/Site.java \
	../../canvas/VisibleComponent.java \
	../../util/UserObjectContainer.java \
	../../canvas/connector/AbstractConnector.java \
	../../canvas/connector/ArcConnector.java \
	../../canvas/connector/Arrowhead.java \
	../../canvas/connector/Connector.java \
	../../canvas/connector/ConnectorEnd.java \
	../../canvas/connector/StraightConnector.java

BasicGraphController.class: \
	BasicEdgeRenderer.java \
	BasicGraphController.java \
	BasicGraphModel.java \
	BasicNodeRenderer.java \
	../AbstractGraphController.java \
	../AbstractGraphModel.java \
	../BasicEdgeController.java \
	../BasicNodeController.java \
	../EdgeController.java \
	../EdgeCreator.java \
	../EdgeRenderer.java \
	../GraphController.java \
	../GraphModel.java \
	../GraphPane.java \
	../MutableGraphModel.java \
	../NodeController.java \
	../NodeInteractor.java \
	../NodeRenderer.java \
	../SimpleGraphController.java \
	../modular/BasicModularGraphModel.java \
	../modular/Edge.java \
	../modular/ModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../modular/Node.java \
	../../canvas/CanvasLayer.java \
	../../canvas/CanvasPane.java \
	../../canvas/Figure.java \
	../../canvas/FigureLayer.java \
	../../canvas/GraphicsPane.java \
	../../canvas/Site.java \
	../../util/Filter.java \
	../../canvas/connector/AutonomousSite.java \
	../../canvas/connector/CenterSite.java \
	../../canvas/connector/Connector.java \
	../../canvas/connector/ConnectorAdapter.java \
	../../canvas/connector/ConnectorEvent.java \
	../../canvas/connector/ConnectorListener.java \
	../../canvas/connector/ConnectorManipulator.java \
	../../canvas/connector/ConnectorTarget.java \
	../../canvas/connector/PerimeterSite.java \
	../../canvas/connector/PerimeterTarget.java \
	../../canvas/event/EventLayer.java \
	../../canvas/event/LayerAdapter.java \
	../../canvas/event/LayerEvent.java \
	../../canvas/event/LayerListener.java \
	../../canvas/event/LayerMotionListener.java \
	../../canvas/event/MouseFilter.java \
	../../canvas/interactor/AbstractInteractor.java \
	../../canvas/interactor/CompositeInteractor.java \
	../../canvas/interactor/DragInteractor.java \
	../../canvas/interactor/GrabHandle.java \
	../../canvas/interactor/Interactor.java \
	../../canvas/interactor/SelectionDragger.java \
	../../canvas/interactor/SelectionInteractor.java \
	../../canvas/interactor/SelectionModel.java

BasicGraphDemo.class: \
	BasicGraphController.java \
	BasicGraphDemo.java \
	BasicGraphModel.java \
	../AbstractGraphController.java \
	../AbstractGraphModel.java \
	../GraphController.java \
	../GraphModel.java \
	../GraphPane.java \
	../JGraph.java \
	../MutableGraphModel.java \
	../SimpleGraphController.java \
	../layout/GlobalLayout.java \
	../layout/GridAnnealingLayout.java \
	../layout/LevelLayout.java \
	../layout/RandomLayout.java \
	../modular/BasicModularGraphModel.java \
	../modular/ModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../toolbox/DeletionListener.java \
	../../canvas/CanvasPane.java \
	../../canvas/FigureLayer.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../gui/AppContext.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java

BasicGraphDemoApplet.class: \
	BasicGraphDemo.java \
	BasicGraphDemoApplet.java \
	../../gui/AppContext.java \
	../../gui/AppletContext.java

BasicGraphModel.class: \
	BasicGraphModel.java \
	../AbstractGraphModel.java \
	../GraphModel.java \
	../MutableGraphModel.java \
	../modular/BasicModularGraphModel.java \
	../modular/CompositeNode.java \
	../modular/Edge.java \
	../modular/Graph.java \
	../modular/ModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../modular/Node.java \
	../../util/IteratorAdapter.java \
	../../util/PropertyContainer.java \
	../../util/SemanticObjectContainer.java

BasicLayoutTarget.class: \
	BasicLayoutTarget.java \
	../GraphController.java \
	../GraphModel.java \
	../GraphPane.java \
	../layout/LayoutTarget.java \
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
	../../canvas/GeometricSet.java \
	../../canvas/GraphicsPane.java \
	../../canvas/VisibleComponent.java \
	../../canvas/ZList.java \
	../../util/Filter.java \
	../../util/FilteredIterator.java \
	../../util/IteratorAdapter.java \
	../../util/UserObjectContainer.java \
	../../canvas/connector/Connector.java

BasicNodeRenderer.class: \
	BasicNodeRenderer.java \
	../GraphController.java \
	../GraphModel.java \
	../NodeRenderer.java \
	../../canvas/AbstractFigure.java \
	../../canvas/AbstractFigureContainer.java \
	../../canvas/CanvasComponent.java \
	../../canvas/CompositeFigure.java \
	../../canvas/Figure.java \
	../../canvas/FigureContainer.java \
	../../canvas/FigureSet.java \
	../../canvas/TransformContext.java \
	../../canvas/VisibleComponent.java \
	../../canvas/ZList.java \
	../../util/UserObjectContainer.java \
	../../canvas/interactor/ShapedFigure.java \
	../../canvas/toolbox/BasicFigure.java

BubbleGraphController.class: \
	ArcRenderer.java \
	BasicGraphController.java \
	BubbleGraphController.java \
	BubbleRenderer.java \
	../AbstractGraphController.java \
	../BasicEdgeController.java \
	../BasicNodeController.java \
	../EdgeController.java \
	../EdgeRenderer.java \
	../GraphController.java \
	../NodeController.java \
	../NodeRenderer.java \
	../SimpleGraphController.java \
	../../canvas/Figure.java \
	../../canvas/FigureLayer.java \
	../../canvas/GraphicsPane.java \
	../../canvas/Site.java \
	../../canvas/connector/ArcConnector.java \
	../../canvas/connector/ArcManipulator.java \
	../../canvas/connector/AutonomousSite.java \
	../../canvas/connector/CenterSite.java \
	../../canvas/connector/Connector.java \
	../../canvas/connector/ConnectorEvent.java \
	../../canvas/connector/ConnectorListener.java \
	../../canvas/connector/ConnectorTarget.java \
	../../canvas/connector/PerimeterSite.java \
	../../canvas/connector/PerimeterTarget.java \
	../../canvas/event/LayerAdapter.java \
	../../canvas/event/LayerEvent.java \
	../../canvas/event/MouseFilter.java \
	../../canvas/interactor/AbstractInteractor.java \
	../../canvas/interactor/BoundsManipulator.java \
	../../canvas/interactor/DragInteractor.java \
	../../canvas/interactor/GrabHandle.java \
	../../canvas/interactor/Interactor.java \
	../../canvas/interactor/Manipulator.java \
	../../canvas/interactor/SelectionDragger.java \
	../../canvas/interactor/SelectionInteractor.java \
	../../canvas/interactor/SelectionModel.java

BubbleGraphDemo.class: \
	BubbleGraphDemo.java \
	BubblePane.java \
	../GraphPane.java \
	../JGraph.java \
	../toolbox/DeletionListener.java \
	../../canvas/CanvasPane.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../gui/AppContext.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java

BubbleGraphDemoApplet.class: \
	BubbleGraphDemo.java \
	BubbleGraphDemoApplet.java \
	../../gui/AppContext.java \
	../../gui/AppletContext.java

BubblePane.class: \
	BasicGraphController.java \
	BasicGraphModel.java \
	BubbleGraphController.java \
	BubblePane.java \
	../AbstractGraphController.java \
	../AbstractGraphModel.java \
	../GraphController.java \
	../GraphModel.java \
	../GraphPane.java \
	../MutableGraphModel.java \
	../SimpleGraphController.java \
	../modular/BasicModularGraphModel.java \
	../modular/ModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../../canvas/CanvasComponent.java \
	../../canvas/CanvasLayer.java \
	../../canvas/CanvasPane.java \
	../../canvas/FigureLayer.java \
	../../canvas/GraphicsPane.java \
	../../canvas/event/EventAcceptor.java

BubbleRenderer.class: \
	BubbleRenderer.java \
	../NodeRenderer.java \
	../toolbox/StateBubble.java \
	../../canvas/AbstractFigure.java \
	../../canvas/CompositeFigure.java \
	../../canvas/Figure.java \
	../../canvas/toolbox/BasicEllipse.java \
	../../canvas/toolbox/LabelWrapper.java


