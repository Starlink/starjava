#
# $Id: makefile,v 1.22 2002/05/16 20:37:34 johnr Exp $
#
# Makefile for diva.graph.schematic
#
DIVA  = ../..
include $(DIVA)/diva.mk

SUBPACKAGES = 

# Java files in this directory
JSRCS = \
	ContextGraphView.java \
	GraphDocument.java \
	GraphDropTarget.java \
	GraphEditor.java \
	GraphEditorApplet.java \
	GraphView.java \
	MultiWindowGraphEditor.java \
	SchematicGraphController.java \
	SchematicGraphModel.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

GraphEditor:
	$(JAVA) -classpath $(CLASSPATH) diva.graph.schematic.GraphEditor

MultiWindowGraphEditor:
	$(JAVA) -classpath $(CLASSPATH) diva.graph.schematic.MultiWindowGraphEditor

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
ContextGraphView.class: \
	ContextGraphView.java \
	GraphDocument.java \
	GraphDropTarget.java \
	MultiWindowGraphEditor.java \
	SchematicGraphController.java \
	../AbstractGraphController.java \
	../GraphController.java \
	../GraphModel.java \
	../GraphPane.java \
	../JGraph.java \
	../MutableGraphModel.java \
	../basic/BasicGraphModel.java \
	../modular/BasicModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../toolbox/DeletionListener.java \
	../../canvas/CanvasPane.java \
	../../canvas/FigureLayer.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../gui/AbstractApplication.java \
	../../gui/AbstractDocument.java \
	../../gui/AbstractView.java \
	../../gui/AppContext.java \
	../../gui/ApplicationContext.java \
	../../gui/ContextView.java \
	../../gui/DefaultActions.java \
	../../gui/Document.java \
	../../gui/GUIUtilities.java \
	../../gui/MultiWindowApplication.java \
	../../gui/View.java \
	../../resource/DefaultBundle.java \
	../../resource/RelativeBundle.java \
	../../gui/toolbox/JPalette.java \
	../../gui/toolbox/JShadePane.java \
	../../gui/toolbox/JStatusBar.java

GraphDocument.class: \
	GraphDocument.java \
	SchematicGraphModel.java \
	../AbstractGraphModel.java \
	../GraphModel.java \
	../MutableGraphModel.java \
	../basic/BasicGraphModel.java \
	../modular/BasicModularGraphModel.java \
	../modular/CompositeNode.java \
	../modular/Edge.java \
	../modular/ModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../modular/Node.java \
	../../gui/AbstractDocument.java \
	../../gui/Application.java \
	../../gui/Document.java \
	../../gui/DocumentFactory.java \
	../../util/LoggableOp.java \
	../../util/xml/XmlBuilder.java \
	../../util/xml/XmlDocument.java \
	../../util/xml/XmlElement.java \
	../../util/xml/XmlReader.java \
	../../util/xml/XmlWriter.java

GraphDropTarget.class: \
	GraphDropTarget.java \
	../AbstractGraphModel.java \
	../GraphController.java \
	../GraphModel.java \
	../GraphPane.java \
	../JGraph.java \
	../MutableGraphModel.java \
	../basic/BasicGraphModel.java \
	../modular/BasicModularGraphModel.java \
	../modular/CompositeNode.java \
	../modular/ModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../modular/Node.java \
	../../canvas/CanvasPane.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java

GraphEditor.class: \
	GraphDocument.java \
	GraphEditor.java \
	GraphView.java \
	../../gui/AbstractApplication.java \
	../../gui/AbstractStoragePolicy.java \
	../../gui/AbstractView.java \
	../../gui/AppContext.java \
	../../gui/AppContextDelegate.java \
	../../gui/Application.java \
	../../gui/ApplicationContext.java \
	../../gui/DefaultActions.java \
	../../gui/DefaultStoragePolicy.java \
	../../gui/DesktopContext.java \
	../../gui/Document.java \
	../../gui/DocumentFactory.java \
	../../gui/GUIUtilities.java \
	../../gui/MDIApplication.java \
	../../gui/MDIContext.java \
	../../gui/StoragePolicy.java \
	../../gui/View.java \
	../../gui/ViewAdapter.java \
	../../gui/ViewEvent.java \
	../../resource/DefaultBundle.java \
	../../resource/RelativeBundle.java \
	../../gui/toolbox/JPalette.java \
	../../gui/toolbox/JShadePane.java

GraphEditorApplet.class: \
	GraphEditor.java \
	GraphEditorApplet.java \
	../../gui/AbstractApplication.java \
	../../gui/AppContext.java \
	../../gui/AppContextDelegate.java \
	../../gui/AppletContext.java \
	../../gui/DesktopContext.java \
	../../gui/MDIApplication.java \
	../../gui/MDIContext.java

GraphView.class: \
	GraphDocument.java \
	GraphDropTarget.java \
	GraphView.java \
	SchematicGraphController.java \
	../AbstractGraphController.java \
	../GraphController.java \
	../GraphModel.java \
	../GraphPane.java \
	../JGraph.java \
	../MutableGraphModel.java \
	../basic/BasicGraphModel.java \
	../modular/BasicModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../toolbox/DeletionListener.java \
	../../canvas/CanvasPane.java \
	../../canvas/FigureLayer.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../gui/AbstractDocument.java \
	../../gui/AbstractView.java \
	../../gui/Document.java \
	../../gui/View.java

MultiWindowGraphEditor.class: \
	ContextGraphView.java \
	GraphDocument.java \
	MultiWindowGraphEditor.java \
	../../gui/AbstractApplication.java \
	../../gui/AbstractStoragePolicy.java \
	../../gui/AbstractView.java \
	../../gui/AppContext.java \
	../../gui/AppContextDelegate.java \
	../../gui/Application.java \
	../../gui/ApplicationContext.java \
	../../gui/ContextView.java \
	../../gui/DefaultActions.java \
	../../gui/DefaultStoragePolicy.java \
	../../gui/DesktopContext.java \
	../../gui/Document.java \
	../../gui/DocumentFactory.java \
	../../gui/GUIUtilities.java \
	../../gui/MDIApplication.java \
	../../gui/MDIContext.java \
	../../gui/MultiWindowApplication.java \
	../../gui/StoragePolicy.java \
	../../gui/View.java \
	../../gui/ViewAdapter.java \
	../../gui/ViewEvent.java \
	../../resource/DefaultBundle.java \
	../../resource/RelativeBundle.java \
	../../gui/toolbox/JPalette.java \
	../../gui/toolbox/JShadePane.java

SchematicGraphController.class: \
	SchematicGraphController.java \
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
	../GraphViewListener.java \
	../MutableGraphModel.java \
	../NodeController.java \
	../NodeRenderer.java \
	../basic/BasicGraphModel.java \
	../basic/BasicLayoutTarget.java \
	../basic/BasicNodeRenderer.java \
	../layout/AbstractGlobalLayout.java \
	../layout/GlobalLayout.java \
	../layout/IncrLayoutAdapter.java \
	../layout/IncrementalLayout.java \
	../layout/IncrementalLayoutListener.java \
	../layout/LayoutTarget.java \
	../modular/BasicModularGraphModel.java \
	../modular/Edge.java \
	../modular/ModularGraphModel.java \
	../modular/MutableModularGraphModel.java \
	../../canvas/AbstractFigure.java \
	../../canvas/AbstractFigureContainer.java \
	../../canvas/AbstractSite.java \
	../../canvas/CanvasComponent.java \
	../../canvas/CanvasUtilities.java \
	../../canvas/CompositeFigure.java \
	../../canvas/Figure.java \
	../../canvas/FigureLayer.java \
	../../canvas/GraphicsPane.java \
	../../canvas/Site.java \
	../../canvas/SiteDecorator.java \
	../../canvas/VisibleComponent.java \
	../../gui/Application.java \
	../../util/Filter.java \
	../../util/UserObjectContainer.java \
	../../canvas/connector/AbstractConnector.java \
	../../canvas/connector/BoundsSite.java \
	../../canvas/connector/Connector.java \
	../../canvas/connector/FixedNormalSite.java \
	../../canvas/connector/ManhattanConnector.java \
	../../canvas/connector/PerimeterSite.java \
	../../canvas/connector/Terminal.java \
	../../canvas/connector/TerminalFigure.java \
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
	../../canvas/interactor/SelectionModel.java \
	../../canvas/interactor/ShapedFigure.java \
	../../canvas/toolbox/BasicFigure.java \
	../../canvas/toolbox/LabelFigure.java

SchematicGraphModel.class: \
	SchematicGraphModel.java \
	../AbstractGraphModel.java \
	../GraphModel.java \
	../MutableGraphModel.java \
	../basic/BasicGraphModel.java \
	../modular/BasicModularGraphModel.java \
	../modular/ModularGraphModel.java \
	../modular/MutableModularGraphModel.java


