#
# $Id: makefile,v 1.21 2002/05/16 20:37:38 johnr Exp $
#
# Makefile for diva.sketch.demo
#
DIVA  = ../..
include $(DIVA)/diva.mk

SUBPACKAGES = 

# Java files in this directory
JSRCS = LineTest.java \
	RegionLabeler.java \
	SketchDemo.java \
	SceneTest.java \
	SessionTest.java \
	SketchDemoApplet.java

SketchDemo:	SketchDemo.class
	$(JAVA) -classpath $(CLASSPATH) diva.sketch.demo.SketchDemo

SessionTest:	SessionTest.class
	$(JAVA) -classpath $(CLASSPATH) diva.sketch.demo.SessionTest

RegionLabeler:	RegionLabeler.class
	$(JAVA) -classpath $(CLASSPATH) diva.sketch.demo.RegionLabeler text graphics

LineTest:	LineTest.class
	$(JAVA) -classpath $(CLASSPATH) diva.sketch.demo.LineTest

# Include the shared compilation rules
include $(DIVA)/compile.mk


### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
LineTest.class: \
	LineTest.java \
	../BasicInterpreter.java \
	../JSketch.java \
	../SketchController.java \
	../SketchPane.java \
	../recognition/Recognition.java \
	../recognition/RecognitionSet.java \
	../recognition/Type.java \
	../toolbox/LineRecognizer.java \
	../../canvas/CanvasPane.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../gui/AppContext.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../canvas/event/LayerEvent.java \
	../../canvas/event/LayerListener.java \
	../../canvas/event/LayerMotionListener.java \
	../../canvas/interactor/AbstractInteractor.java \
	../../canvas/interactor/Interactor.java

RegionLabeler.class: \
	RegionLabeler.java \
	../BasicInterpreter.java \
	../JSketch.java \
	../LassoSelectionAction.java \
	../SketchController.java \
	../SketchModel.java \
	../SketchPane.java \
	../StrokeSymbol.java \
	../Symbol.java \
	../features/FEUtilities.java \
	../features/PathLengthFE.java \
	../recognition/BasicScene.java \
	../recognition/CompositeElement.java \
	../recognition/Scene.java \
	../recognition/SceneElement.java \
	../recognition/SceneRecognizer.java \
	../recognition/SceneWriter.java \
	../recognition/SimpleData.java \
	../recognition/StrokeElement.java \
	../recognition/TypedData.java \
	../../canvas/CanvasComponent.java \
	../../canvas/CanvasPane.java \
	../../canvas/Figure.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../canvas/VisibleComponent.java \
	../../gui/AppContext.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../gui/ExtensionFileFilter.java \
	../../util/UserObjectContainer.java \
	../../whiteboard/SketchStoragePolicy.java \
	../../canvas/event/LayerEvent.java \
	../../canvas/event/LayerListener.java \
	../../canvas/event/LayerMotionListener.java \
	../../canvas/interactor/AbstractInteractor.java \
	../../canvas/interactor/Interactor.java \
	../../canvas/interactor/SelectionModel.java \
	../../util/xml/AbstractXmlBuilder.java

SketchDemo.class: \
	SketchDemo.java \
	../JSketch.java \
	../SketchController.java \
	../SketchModel.java \
	../SketchPane.java \
	../SketchParser.java \
	../SketchWriter.java \
	../../canvas/CanvasPane.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../gui/AppContext.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../gui/ExtensionFileFilter.java \
	../../util/aelfred/HandlerBase.java

SceneTest.class: \
	SceneTest.java \
	../JSketch.java \
	../SketchModel.java \
	../SketchPane.java \
	../SketchParser.java \
	../SketchWriter.java \
	../StrokeSymbol.java \
	../classification/Classifier.java \
	../recognition/BasicScene.java \
	../recognition/BasicStrokeRecognizer.java \
	../recognition/Scene.java \
	../recognition/SceneElement.java \
	../recognition/SceneParser.java \
	../recognition/SceneRecognizer.java \
	../recognition/SceneWriter.java \
	../recognition/StrokeElement.java \
	../recognition/StrokeRecognizer.java \
	../recognition/StrokeSceneRecognizer.java \
	../recognition/TimedStroke.java \
	../toolbox/LLRSceneMetric.java \
	../toolbox/LLRSceneRecognizer.java \
	../../gui/AppContext.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java \
	../../gui/ExtensionFileFilter.java

SessionTest.class: \
	SessionTest.java \
	../JSketch.java \
	../SketchController.java \
	../SketchModel.java \
	../SketchPane.java \
	../StrokeSymbol.java \
	../Symbol.java \
	../classification/Classifier.java \
	../recognition/BasicScene.java \
	../recognition/BasicStrokeRecognizer.java \
	../recognition/Scene.java \
	../recognition/SceneRecognizer.java \
	../recognition/StrokeElement.java \
	../recognition/StrokeRecognizer.java \
	../recognition/StrokeSceneRecognizer.java \
	../recognition/TimedStroke.java \
	../toolbox/DashedPathRecognizer.java \
	../../canvas/CanvasPane.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../gui/AppContext.java \
	../../gui/ApplicationContext.java \
	../../gui/BasicFrame.java

SketchDemoApplet.class: \
	SketchDemo.java \
	SketchDemoApplet.java \
	../../gui/AppContext.java \
	../../gui/AppletContext.java


