#
# $Id: makefile,v 1.24 2002/05/16 20:37:46 johnr Exp $
#
# Makefile for diva.whiteboard
#
DIVA  = ..
include $(DIVA)/diva.mk

SUBPACKAGES =

# Java files in this directory
JSRCS = \
	CommandInterpreter.java \
	HighlightInterpreter.java \
	SketchDocument.java \
	SketchStoragePolicy.java \
	SketchTest1.java \
	SketchTest2.java \
	SketchTest3.java \
	Whiteboard.java \
	WhiteboardEdits.java \
	WhiteboardState.java \
	WhiteboardView.java \
	ThumbnailView.java 

# Include the shared compilation rules
include $(DIVA)/compile.mk

Whiteboard: Whiteboard.class
	$(JAVA) -classpath $(CLASSPATH) diva.whiteboard.Whiteboard 

SketchTest1: SketchTest1.class
	$(JAVA) -classpath $(CLASSPATH) diva.whiteboard.SketchTest1

SketchTest2: SketchTest2.class
	$(JAVA) -classpath $(CLASSPATH) diva.whiteboard.SketchTest2

SketchTest3: SketchTest3.class
	$(JAVA) -classpath $(CLASSPATH) diva.whiteboard.SketchTest3

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
CommandInterpreter.class: \
	CommandInterpreter.java \
	WhiteboardState.java \
	../canvas/CanvasPane.java \
	../canvas/Figure.java \
	../canvas/FigureDecorator.java \
	../canvas/FigureLayer.java \
	../canvas/GraphicsPane.java \
	../gui/MultipageDocument.java \
	../resource/DefaultBundle.java \
	../resource/RelativeBundle.java \
	../sketch/BasicInterpreter.java \
	../sketch/CheckSelectionAction.java \
	../sketch/DeletionAction.java \
	../sketch/LassoSelectionAction.java \
	../sketch/MultiStateInterpreter.java \
	../sketch/SketchController.java \
	../sketch/SketchModel.java \
	../sketch/StrokeSymbol.java \
	../sketch/Symbol.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/LayerListener.java \
	../canvas/event/LayerMotionListener.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/Interactor.java \
	../canvas/interactor/SelectionModel.java \
	../sketch/classification/Classifier.java \
	../sketch/features/FEUtilities.java \
	../sketch/features/PathLengthFE.java \
	../sketch/recognition/BasicStrokeRecognizer.java \
	../sketch/recognition/Recognition.java \
	../sketch/recognition/RecognitionSet.java \
	../sketch/recognition/StrokeRecognizer.java \
	../sketch/recognition/TimedStroke.java \
	../sketch/recognition/Type.java \
	../util/java2d/Polygon2D.java \
	../util/java2d/Polyline2D.java

HighlightInterpreter.class: \
	HighlightInterpreter.java \
	../canvas/GraphicsPane.java \
	../sketch/BasicInterpreter.java \
	../sketch/SketchController.java \
	../sketch/SketchModel.java \
	../sketch/StrokeSymbol.java \
	../sketch/Symbol.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/LayerListener.java \
	../canvas/event/LayerMotionListener.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/Interactor.java \
	../canvas/toolbox/BasicFigure.java \
	../sketch/recognition/TimedStroke.java \
	../util/java2d/Polyline2D.java

SketchDocument.class: \
	SketchDocument.java \
	../gui/AbstractDocument.java \
	../gui/Application.java \
	../gui/BasicPage.java \
	../gui/Document.java \
	../gui/DocumentFactory.java \
	../gui/MultipageDocument.java \
	../gui/MultipageModel.java \
	../gui/MultipageParser.java \
	../gui/MultipageWriter.java \
	../gui/Page.java \
	../sketch/JSketch.java \
	../sketch/SketchModel.java \
	../sketch/SketchPane.java \
	../sketch/SketchParser.java \
	../sketch/SketchWriter.java \
	../sketch/Symbol.java \
	../util/ModelParser.java \
	../util/ModelWriter.java \
	../util/aelfred/HandlerBase.java

SketchStoragePolicy.class: \
	SketchStoragePolicy.java \
	../gui/AbstractStoragePolicy.java \
	../gui/AppContext.java \
	../gui/Application.java \
	../gui/DefaultStoragePolicy.java \
	../gui/DesktopContext.java \
	../gui/Document.java \
	../gui/ExtensionFileFilter.java \
	../gui/GUIUtilities.java \
	../gui/StoragePolicy.java \
	../gui/toolbox/JStatusBar.java

SketchTest1.class: \
	SketchTest1.java

SketchTest2.class: \
	SketchTest2.java \
	../canvas/AbstractFigure.java \
	../canvas/CanvasComponent.java \
	../canvas/CanvasLayer.java \
	../canvas/CanvasPane.java \
	../canvas/Figure.java \
	../canvas/FigureLayer.java \
	../canvas/GraphicsPane.java \
	../canvas/JCanvas.java \
	../canvas/OverlayLayer.java \
	../sketch/SketchLayer.java \
	../util/PropertyContainer.java \
	../canvas/event/EventAcceptor.java \
	../canvas/event/EventLayer.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/LayerListener.java \
	../canvas/event/LayerMotionListener.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/Interactor.java \
	../canvas/interactor/ShapedFigure.java \
	../canvas/toolbox/BasicFigure.java \
	../sketch/recognition/TimedStroke.java \
	../util/java2d/Polyline2D.java

SketchTest3.class: \
	SketchTest3.java \
	../canvas/AbstractFigure.java \
	../canvas/CanvasComponent.java \
	../canvas/CanvasLayer.java \
	../canvas/CanvasPane.java \
	../canvas/Figure.java \
	../canvas/FigureLayer.java \
	../canvas/GraphicsPane.java \
	../canvas/JCanvas.java \
	../canvas/OverlayLayer.java \
	../sketch/SketchLayer.java \
	../util/PropertyContainer.java \
	../canvas/event/EventAcceptor.java \
	../canvas/event/EventLayer.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/LayerListener.java \
	../canvas/event/LayerMotionListener.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/Interactor.java \
	../canvas/interactor/ShapedFigure.java \
	../canvas/toolbox/BasicFigure.java \
	../sketch/recognition/TimedStroke.java \
	../util/java2d/Polyline2D.java

Whiteboard.class: \
	SketchDocument.java \
	SketchStoragePolicy.java \
	ThumbnailView.java \
	Whiteboard.java \
	WhiteboardEdits.java \
	WhiteboardState.java \
	WhiteboardView.java \
	../gui/AbstractApplication.java \
	../gui/AbstractDocument.java \
	../gui/AbstractStoragePolicy.java \
	../gui/AbstractView.java \
	../gui/AppContext.java \
	../gui/AppContextDelegate.java \
	../gui/Application.java \
	../gui/ApplicationContext.java \
	../gui/BasicPage.java \
	../gui/DefaultActions.java \
	../gui/DefaultStoragePolicy.java \
	../gui/DesktopContext.java \
	../gui/Document.java \
	../gui/DocumentFactory.java \
	../gui/ExtensionFileFilter.java \
	../gui/GUIUtilities.java \
	../gui/MDIApplication.java \
	../gui/MDIContext.java \
	../gui/MultipageDocument.java \
	../gui/MultipageModel.java \
	../gui/Page.java \
	../gui/StoragePolicy.java \
	../gui/View.java \
	../gui/ViewAdapter.java \
	../gui/ViewEvent.java \
	../resource/RelativeBundle.java \
	../sketch/JSketch.java \
	../sketch/SketchEvent.java \
	../sketch/SketchListener.java \
	../sketch/SketchModel.java \
	../sketch/SketchPane.java \
	../sketch/SketchParser.java \
	../sketch/SketchWriter.java \
	../gui/toolbox/JPalette.java \
	../gui/toolbox/JShadePane.java \
	../gui/toolbox/ListDataModel.java \
	../util/aelfred/HandlerBase.java

WhiteboardEdits.class: \
	WhiteboardEdits.java \
	../gui/MultipageModel.java \
	../gui/Page.java \
	../sketch/SketchListener.java \
	../sketch/SketchModel.java \
	../sketch/Symbol.java

WhiteboardState.class: \
	WhiteboardState.java \
	../sketch/SketchController.java

WhiteboardView.class: \
	CommandInterpreter.java \
	HighlightInterpreter.java \
	Whiteboard.java \
	WhiteboardEdits.java \
	WhiteboardState.java \
	WhiteboardView.java \
	../canvas/CanvasComponent.java \
	../canvas/CanvasPane.java \
	../canvas/CanvasUtilities.java \
	../canvas/Figure.java \
	../canvas/GraphicsPane.java \
	../canvas/JCanvas.java \
	../canvas/VisibleComponent.java \
	../gui/AbstractApplication.java \
	../gui/AbstractDocument.java \
	../gui/AbstractView.java \
	../gui/AppContextDelegate.java \
	../gui/Application.java \
	../gui/BasicPage.java \
	../gui/DesktopContext.java \
	../gui/Document.java \
	../gui/MDIApplication.java \
	../gui/MultipageDocument.java \
	../gui/MultipageModel.java \
	../gui/Page.java \
	../gui/View.java \
	../sketch/BasicInterpreter.java \
	../sketch/CompositeSymbol.java \
	../sketch/JSketch.java \
	../sketch/MultiStateInterpreter.java \
	../sketch/SketchController.java \
	../sketch/SketchEvent.java \
	../sketch/SketchListener.java \
	../sketch/SketchModel.java \
	../sketch/SketchPane.java \
	../sketch/SketchParser.java \
	../sketch/SketchWriter.java \
	../sketch/StrokeSymbol.java \
	../sketch/Symbol.java \
	../util/UserObjectContainer.java \
	../canvas/event/LayerEvent.java \
	../canvas/event/LayerListener.java \
	../canvas/event/LayerMotionListener.java \
	../canvas/interactor/AbstractInteractor.java \
	../canvas/interactor/CompositeInteractor.java \
	../canvas/interactor/Interactor.java \
	../canvas/interactor/SelectionInteractor.java \
	../canvas/interactor/SelectionModel.java \
	../gui/toolbox/ListDataModel.java \
	../sketch/recognition/TimedStroke.java \
	../util/aelfred/HandlerBase.java \
	../util/java2d/Polyline2D.java \
	../util/java2d/ShapeUtilities.java

ThumbnailView.class: \
	ThumbnailView.java \
	WhiteboardEdits.java \
	WhiteboardView.java \
	../canvas/Figure.java \
	../gui/AbstractDocument.java \
	../gui/AbstractView.java \
	../gui/Application.java \
	../gui/BasicPage.java \
	../gui/DesktopContext.java \
	../gui/Document.java \
	../gui/MultipageDocument.java \
	../gui/MultipageModel.java \
	../gui/Page.java \
	../gui/View.java \
	../gui/ViewAdapter.java \
	../gui/ViewEvent.java \
	../sketch/JSketch.java \
	../sketch/SketchEvent.java \
	../sketch/SketchListener.java \
	../sketch/SketchModel.java \
	../sketch/SketchPane.java \
	../sketch/SketchParser.java \
	../sketch/SketchWriter.java \
	../sketch/Symbol.java \
	../canvas/interactor/SelectionModel.java \
	../gui/toolbox/ListDataModel.java \
	../util/java2d/AbstractPaintedGraphic.java \
	../util/java2d/PaintedPath.java


