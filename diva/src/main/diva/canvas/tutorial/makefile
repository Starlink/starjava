#
# $Id: makefile,v 1.27 2002/08/12 06:36:54 johnr Exp $
#
# Makefile for diva.canvas.tutorial
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	ArcTutorial.java \
	CompositeFigureTutorial.java \
	ConnectorTutorial.java \
	DragTutorial.java \
	FigureTutorial.java \
	LabelTutorial.java \
	SimpleTutorial.java \
	TerminalTutorial.java \
	IconTutorial.java \
	TransformedFigureTutorial.java \
	VectorFigureTutorial.java

# Miscellaneous files that really should be here
MISC_FILES = demo.gif

# Include the shared compilation rules
include $(DIVA)/compile.mk

# Rules for running the examples. These are just there to save messing
# about with classpath
ArcTutorial: ArcTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.ArcTutorial

CompositeFigureTutorial: CompositeFigureTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.CompositeFigureTutorial

ConnectorTutorial: ConnectorTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.ConnectorTutorial

DragTutorial: DragTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.DragTutorial

IconTutorial: IconTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.IconTutorial

LabelTutorial: LabelTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.LabelTutorial

FigureTutorial: FigureTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.FigureTutorial

SimpleTutorial: SimpleTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.SimpleTutorial

SwingTutorial: SwingTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.SwingTutorial

TerminalTutorial: TerminalTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.TerminalTutorial

TransformedFigureTutorial: TransformedFigureTutorial.class
	$(JAVA) -classpath $(CLASSPATH) \
		diva.canvas.tutorial.TransformedFigureTutorial

VectorFigureTutorial: VectorFigureTutorial.class
	$(JAVA) -classpath $(CLASSPATH) \
		diva.canvas.tutorial.VectorFigureTutorial

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
ArcTutorial.class: \
	ArcTutorial.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../CanvasUtilities.java \
	../Figure.java \
	../FigureDecorator.java \
	../FigureLayer.java \
	../GraphicsPane.java \
	../JCanvas.java \
	../Site.java \
	../VisibleComponent.java \
	../connector/AbstractConnector.java \
	../connector/AbstractConnectorTarget.java \
	../connector/ArcConnector.java \
	../connector/ArcManipulator.java \
	../connector/Arrowhead.java \
	../connector/Blob.java \
	../connector/Connector.java \
	../connector/ConnectorEnd.java \
	../connector/ConnectorEvent.java \
	../connector/ConnectorListener.java \
	../connector/ConnectorManipulator.java \
	../connector/ConnectorTarget.java \
	../connector/ManhattanConnector.java \
	../connector/PerimeterSite.java \
	../connector/PerimeterTarget.java \
	../connector/StraightConnector.java \
	../event/LayerAdapter.java \
	../event/LayerEvent.java \
	../event/LayerListener.java \
	../interactor/AbstractInteractor.java \
	../interactor/BasicSelectionRenderer.java \
	../interactor/BoundsManipulator.java \
	../interactor/CompositeInteractor.java \
	../interactor/DragInteractor.java \
	../interactor/Interactor.java \
	../interactor/Manipulator.java \
	../interactor/SelectionDragger.java \
	../interactor/SelectionInteractor.java \
	../interactor/SelectionModel.java \
	../interactor/SelectionRenderer.java \
	../interactor/ShapedFigure.java \
	../toolbox/BasicController.java \
	../toolbox/BasicEllipse.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../toolbox/TypedDecorator.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../util/UserObjectContainer.java

CompositeFigureTutorial.class: \
	CompositeFigureTutorial.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../CanvasUtilities.java \
	../CompositeFigure.java \
	../Figure.java \
	../FigureContainer.java \
	../FigureLayer.java \
	../FigureSet.java \
	../FigureWrapper.java \
	../GraphicsPane.java \
	../JCanvas.java \
	../Site.java \
	../TransformContext.java \
	../VisibleComponent.java \
	../connector/CenterSite.java \
	../connector/Connector.java \
	../connector/StraightConnector.java \
	../event/MouseFilter.java \
	../interactor/AbstractInteractor.java \
	../interactor/CompositeInteractor.java \
	../interactor/Interactor.java \
	../interactor/SelectionInteractor.java \
	../interactor/ShapedFigure.java \
	../toolbox/BasicController.java \
	../toolbox/BasicEllipse.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../util/UserObjectContainer.java

ConnectorTutorial.class: \
	ConnectorTutorial.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../CanvasUtilities.java \
	../Figure.java \
	../FigureDecorator.java \
	../FigureLayer.java \
	../GraphicsPane.java \
	../JCanvas.java \
	../Site.java \
	../VisibleComponent.java \
	../connector/AbstractConnector.java \
	../connector/AbstractConnectorTarget.java \
	../connector/Arrowhead.java \
	../connector/Blob.java \
	../connector/Connector.java \
	../connector/ConnectorEnd.java \
	../connector/ConnectorEvent.java \
	../connector/ConnectorListener.java \
	../connector/ConnectorManipulator.java \
	../connector/ConnectorTarget.java \
	../connector/ManhattanConnector.java \
	../connector/StraightConnector.java \
	../event/LayerAdapter.java \
	../event/LayerEvent.java \
	../event/LayerListener.java \
	../interactor/AbstractInteractor.java \
	../interactor/BasicSelectionRenderer.java \
	../interactor/BoundsGeometry.java \
	../interactor/CompositeInteractor.java \
	../interactor/DragInteractor.java \
	../interactor/Interactor.java \
	../interactor/Manipulator.java \
	../interactor/SelectionDragger.java \
	../interactor/SelectionInteractor.java \
	../interactor/SelectionModel.java \
	../interactor/SelectionRenderer.java \
	../interactor/ShapedFigure.java \
	../toolbox/BasicController.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../util/UserObjectContainer.java

DragTutorial.class: \
	DragTutorial.java \
	../AbstractFigure.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../Figure.java \
	../FigureLayer.java \
	../GraphicsPane.java \
	../JCanvas.java \
	../OverlayLayer.java \
	../event/LayerListener.java \
	../event/LayerMotionListener.java \
	../event/MouseFilter.java \
	../interactor/AbstractInteractor.java \
	../interactor/BoundedDragInteractor.java \
	../interactor/DragInteractor.java \
	../interactor/Interactor.java \
	../interactor/ShapedFigure.java \
	../toolbox/BasicEllipse.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../toolbox/SwingWrapper.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java

FigureTutorial.class: \
	FigureTutorial.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../Figure.java \
	../FigureDecorator.java \
	../FigureLayer.java \
	../GraphicsPane.java \
	../JCanvas.java \
	../VisibleComponent.java \
	../event/MouseFilter.java \
	../interactor/AbstractInteractor.java \
	../interactor/BoundsManipulator.java \
	../interactor/CompositeInteractor.java \
	../interactor/DragInteractor.java \
	../interactor/Interactor.java \
	../interactor/Manipulator.java \
	../interactor/SelectionInteractor.java \
	../toolbox/BasicController.java \
	../toolbox/BasicEllipse.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../toolbox/ImageFigure.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/ShapeUtilities.java

LabelTutorial.class: \
	LabelTutorial.java \
	../AbstractFigure.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../CanvasUtilities.java \
	../Figure.java \
	../FigureLayer.java \
	../GraphicsPane.java \
	../JCanvas.java \
	../interactor/AbstractInteractor.java \
	../interactor/CompositeInteractor.java \
	../interactor/Interactor.java \
	../interactor/SelectionInteractor.java \
	../interactor/ShapedFigure.java \
	../toolbox/BasicController.java \
	../toolbox/BasicEllipse.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../toolbox/LabelFigure.java \
	../toolbox/LabelWrapper.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../util/java2d/Polyline2D.java

SimpleTutorial.class: \
	SimpleTutorial.java \
	../AbstractFigure.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../Figure.java \
	../FigureLayer.java \
	../GraphicsPane.java \
	../JCanvas.java \
	../interactor/ShapedFigure.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../util/java2d/Polyline2D.java

TerminalTutorial.class: \
	TerminalTutorial.java \
	../AbstractFigure.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../CanvasUtilities.java \
	../Figure.java \
	../FigureLayer.java \
	../GraphicsPane.java \
	../JCanvas.java \
	../Site.java \
	../connector/Arrowhead.java \
	../connector/Blob.java \
	../connector/ConnectorEnd.java \
	../connector/StraightTerminal.java \
	../connector/Terminal.java \
	../interactor/AbstractInteractor.java \
	../interactor/BoundsGeometry.java \
	../interactor/CompositeInteractor.java \
	../interactor/Interactor.java \
	../interactor/SelectionInteractor.java \
	../interactor/ShapedFigure.java \
	../toolbox/BasicController.java \
	../toolbox/BasicEllipse.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../util/java2d/Polyline2D.java

IconTutorial.class: \
	IconTutorial.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../CanvasUtilities.java \
	../Figure.java \
	../FigureDecorator.java \
	../FigureLayer.java \
	../GraphicsPane.java \
	../JCanvas.java \
	../Site.java \
	../connector/Arrowhead.java \
	../connector/Blob.java \
	../connector/ConnectorEnd.java \
	../connector/StraightTerminal.java \
	../connector/Terminal.java \
	../interactor/AbstractInteractor.java \
	../interactor/BoundsManipulator.java \
	../interactor/CompositeInteractor.java \
	../interactor/Interactor.java \
	../interactor/Manipulator.java \
	../interactor/SelectionInteractor.java \
	../interactor/ShapedFigure.java \
	../toolbox/BasicController.java \
	../toolbox/BasicEllipse.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../toolbox/GraphicsParser.java \
	../toolbox/IconFigure.java \
	../toolbox/PaintedFigure.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../util/java2d/AbstractPaintedGraphic.java \
	../../util/java2d/PaintedGraphic.java \
	../../util/java2d/PaintedList.java \
	../../util/java2d/PaintedObject.java \
	../../util/java2d/PaintedPath.java \
	../../util/java2d/PaintedShape.java \
	../../util/java2d/Polygon2D.java \
	../../util/java2d/Polyline2D.java

TransformedFigureTutorial.class: \
	TransformedFigureTutorial.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../CanvasUtilities.java \
	../Figure.java \
	../FigureLayer.java \
	../GraphicsPane.java \
	../JCanvas.java \
	../TransformContext.java \
	../VisibleComponent.java \
	../event/MouseFilter.java \
	../interactor/AbstractInteractor.java \
	../interactor/DragInteractor.java \
	../interactor/Interactor.java \
	../toolbox/BasicEllipse.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../util/UserObjectContainer.java \
	../../util/java2d/ShapeUtilities.java

VectorFigureTutorial.class: \
	VectorFigureTutorial.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../Figure.java \
	../FigureDecorator.java \
	../FigureLayer.java \
	../GraphicsPane.java \
	../JCanvas.java \
	../event/MouseFilter.java \
	../interactor/AbstractInteractor.java \
	../interactor/BoundsManipulator.java \
	../interactor/CompositeInteractor.java \
	../interactor/DragInteractor.java \
	../interactor/Interactor.java \
	../interactor/Manipulator.java \
	../interactor/SelectionInteractor.java \
	../toolbox/BasicController.java \
	../toolbox/BasicRectangle.java \
	../toolbox/VectorFigure.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../util/java2d/FillStyle.java \
	../../util/java2d/LineStyle.java \
	../../util/java2d/ShapeUtilities.java


