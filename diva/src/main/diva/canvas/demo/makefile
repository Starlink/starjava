#
# $Id: makefile,v 1.48 2002/05/16 20:37:30 johnr Exp $
#
# Makefile for diva.canvas.demo
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	FigureDemo.java \
	MainWindow.java \
	RootPane.java \
	SelectionPane.java \
	SimplePane.java \
	SpaceDemo.java \
	SpaceWindow.java \
	WarpImageFigure.java \
	ZoomDemo.java \

# Miscellaneous files that really should be here
MISC_FILES = demo.gif

# Include the shared compilation rules
include $(DIVA)/compile.mk

# This is just a handy way of getting the classpath
FigureDemo: classes
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.demo.FigureDemo

ZoomDemo: ZoomDemo.class WarpImageFigure.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.demo.ZoomDemo

SpaceDemo: classes
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.demo.SpaceDemo

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
FigureDemo.class: \
	FigureDemo.java \
	MainWindow.java \
	SelectionPane.java \
	SimplePane.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../Figure.java \
	../FigureLayer.java \
	../GraphicsPane.java \
	../JCanvas.java \
	../PaneWrapper.java \
	../event/EventAcceptor.java \
	../interactor/AbstractInteractor.java \
	../interactor/CompositeInteractor.java \
	../interactor/Interactor.java \
	../interactor/SelectionInteractor.java \
	../interactor/ShapedFigure.java \
	../toolbox/BasicFigure.java \
	../../graph/AbstractGraphController.java \
	../../graph/AbstractGraphModel.java \
	../../graph/GraphController.java \
	../../graph/GraphModel.java \
	../../graph/GraphPane.java \
	../../graph/MutableGraphModel.java \
	../../graph/SimpleGraphController.java \
	../../graph/basic/BasicGraphController.java \
	../../graph/basic/BasicGraphModel.java \
	../../graph/modular/BasicModularGraphModel.java \
	../../graph/modular/Edge.java \
	../../graph/modular/ModularGraphModel.java \
	../../graph/modular/MutableModularGraphModel.java \
	../../graph/modular/Node.java \
	../../util/java2d/ShapeUtilities.java

MainWindow.class: \
	MainWindow.java \
	../CanvasComponent.java \
	../CanvasPane.java \
	../JCanvas.java \
	../TransformContext.java \
	../event/EventAcceptor.java

RootPane.class: \
	RootPane.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../Figure.java \
	../FigureDecorator.java \
	../FigureLayer.java \
	../GraphicsPane.java \
	../PaneWrapper.java \
	../event/EventAcceptor.java \
	../interactor/AbstractInteractor.java \
	../interactor/BoundsManipulator.java \
	../interactor/CompositeInteractor.java \
	../interactor/Interactor.java \
	../interactor/Manipulator.java \
	../interactor/SelectionInteractor.java \
	../interactor/ShapedFigure.java \
	../toolbox/BasicFigure.java \
	../../graph/AbstractGraphController.java \
	../../graph/AbstractGraphModel.java \
	../../graph/GraphController.java \
	../../graph/GraphModel.java \
	../../graph/GraphPane.java \
	../../graph/MutableGraphModel.java \
	../../graph/SimpleGraphController.java \
	../../sketch/SketchController.java \
	../../graph/basic/BasicGraphController.java \
	../../graph/basic/BasicGraphModel.java \
	../../graph/basic/BasicLayoutTarget.java \
	../../graph/basic/BubblePane.java \
	../../graph/layout/AbstractGlobalLayout.java \
	../../graph/layout/LayoutTarget.java \
	../../graph/layout/LevelLayout.java \
	../../graph/modular/BasicModularGraphModel.java \
	../../graph/modular/Edge.java \
	../../graph/modular/ModularGraphModel.java \
	../../graph/modular/MutableModularGraphModel.java \
	../../graph/modular/Node.java \
	../../graph/toolbox/StateBubble.java \
	../../sketch/toolbox/PanZoomController.java \
	../../util/java2d/ShapeUtilities.java

SelectionPane.class: \
	SelectionPane.java \
	../AbstractFigure.java \
	../AbstractFigureContainer.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../CompositeFigure.java \
	../Figure.java \
	../FigureContainer.java \
	../FigureDecorator.java \
	../FigureLayer.java \
	../FigureSet.java \
	../GraphicsPane.java \
	../VisibleComponent.java \
	../event/EventAcceptor.java \
	../interactor/AbstractInteractor.java \
	../interactor/BoundsManipulator.java \
	../interactor/CompositeInteractor.java \
	../interactor/Interactor.java \
	../interactor/Manipulator.java \
	../interactor/SelectionInteractor.java \
	../interactor/ShapedFigure.java \
	../toolbox/BasicEllipse.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../toolbox/LabelFigure.java \
	../../sketch/SketchController.java \
	../../util/UserObjectContainer.java \
	../../sketch/toolbox/PanZoomController.java \
	../../util/java2d/Polygon2D.java \
	../../util/java2d/ShapeUtilities.java

SimplePane.class: \
	SimplePane.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../Figure.java \
	../FigureLayer.java \
	../GraphicsPane.java \
	../event/EventAcceptor.java \
	../interactor/AbstractInteractor.java \
	../interactor/CompositeInteractor.java \
	../interactor/Interactor.java \
	../interactor/SelectionInteractor.java \
	../interactor/ShapedFigure.java \
	../toolbox/BasicController.java \
	../toolbox/BasicEllipse.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../../util/java2d/Polygon2D.java \
	../../util/java2d/ShapeUtilities.java

SpaceDemo.class: \
	RootPane.java \
	SpaceDemo.java \
	SpaceWindow.java \
	../CanvasPane.java \
	../GraphicsPane.java \
	../JCanvas.java \
	../../graph/GraphPane.java

SpaceWindow.class: \
	SpaceWindow.java \
	../CanvasComponent.java \
	../CanvasPane.java \
	../JCanvas.java \
	../TransformContext.java \
	../event/EventAcceptor.java \
	../../gui/toolbox/JPanner.java

WarpImageFigure.class: \
	WarpImageFigure.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../Figure.java \
	../VisibleComponent.java \
	../../util/UserObjectContainer.java

ZoomDemo.class: \
	WarpImageFigure.java \
	ZoomDemo.java \
	../AbstractFigure.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../CanvasPane.java \
	../CanvasUtilities.java \
	../Figure.java \
	../FigureLayer.java \
	../GraphicsPane.java \
	../JCanvas.java \
	../PaneWrapper.java \
	../TransformContext.java \
	../VisibleComponent.java \
	../event/EventAcceptor.java \
	../event/EventLayer.java \
	../event/LayerMouseAdapter.java \
	../event/MouseFilter.java \
	../interactor/AbstractInteractor.java \
	../interactor/CompositeInteractor.java \
	../interactor/DragInteractor.java \
	../interactor/Interactor.java \
	../interactor/SelectionInteractor.java \
	../interactor/ShapedFigure.java \
	../toolbox/BasicController.java \
	../toolbox/BasicEllipse.java \
	../toolbox/BasicFigure.java \
	../toolbox/BasicRectangle.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../sketch/SketchController.java \
	../../sketch/SketchPane.java \
	../../util/UserObjectContainer.java \
	../../sketch/toolbox/PanZoomController.java


