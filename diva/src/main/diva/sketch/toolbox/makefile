#
# $Id: makefile,v 1.36 2002/07/14 00:05:02 hwawen Exp $
#
# Makefile for diva.sketch.toolbox
#
DIVA  = ../..
include $(DIVA)/diva.mk

SUBPACKAGES = 

# Java files in this directory
JSRCS = \
	ApproximateStrokeFilter.java \
	AveragingStrokeFilter.java \
	BulletedData.java \
	BulletedItem.java \
	BulletedItems.java \
	BulletedLine.java \
	BulletedList.java \
	CachingStrokeRecognizer.java \
	CompositeTransducer.java \
	DashedPathData.java \
	DashedPathRecognizer.java \
	DehookingStrokeFilter.java \
	HMMTrainer.java \
	ImageTransducer.java \
	InterpolateStrokeFilter.java \
	LineRecognizer.java \
	LLRSceneMetric.java \
	LLRSceneRecognizer.java \
	ModedIncrRecognizer.java \
	NormalizeLengthStrokeFilter.java \
	NormalizeTimeStrokeFilter.java \
	SceneMetric.java \
	PanRecognizer.java \
	PanZoomController.java \
	PanZoomInterpreter.java \
	RemoveDupPtsStrokeFilter.java \
	RetypingRecognizer.java \
	ScribbleRecognizer.java \
	SceneTranslator.java \
	SketchTransducer.java \
	StrokeFilter.java \
	TableTransducer.java \
	TextBlock.java \
	TextData.java \
	TextLine.java \
	TextAnnotations.java \
	TrainingToMatlab.java \
	Transducer.java \
	ZoomRecognizer.java 

# Include the shared compilation rules
include $(DIVA)/compile.mk


### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
ApproximateStrokeFilter.class: \
	ApproximateStrokeFilter.java \
	StrokeFilter.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

BulletedData.class: \
	BulletedData.java \
	TextAnnotations.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java

BulletedItem.class: \
	BulletedItem.java \
	TextAnnotations.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java

BulletedItems.class: \
	BulletedItems.java \
	TextAnnotations.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java

BulletedLine.class: \
	BulletedLine.java \
	TextAnnotations.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java

BulletedList.class: \
	BulletedList.java \
	TextAnnotations.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java

CachingStrokeRecognizer.class: \
	CachingStrokeRecognizer.java \
	../recognition/RecognitionSet.java \
	../recognition/StrokeRecognizer.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

CompositeTransducer.class: \
	CompositeTransducer.java \
	Transducer.java \
	../SketchModel.java

DashedPathData.class: \
	DashedPathData.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../../util/java2d/Polyline2D.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java \
	../../util/xml/XmlElement.java

DashedPathRecognizer.class: \
	ApproximateStrokeFilter.java \
	DashedPathData.java \
	DashedPathRecognizer.java \
	StrokeFilter.java \
	../recognition/CompositeElement.java \
	../recognition/Scene.java \
	../recognition/SceneDelta.java \
	../recognition/SceneDeltaSet.java \
	../recognition/SceneElement.java \
	../recognition/SceneRecognizer.java \
	../recognition/SimpleData.java \
	../recognition/StrokeElement.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java \
	../../util/xml/AbstractXmlBuilder.java

DehookingStrokeFilter.class: \
	DehookingStrokeFilter.java \
	StrokeFilter.java \
	../features/FEUtilities.java \
	../features/PathLengthFE.java \
	../recognition/MSTrainingModel.java \
	../recognition/MSTrainingParser.java \
	../recognition/TimedStroke.java \
	../../util/aelfred/HandlerBase.java \
	../../util/java2d/Polyline2D.java

HMMTrainer.class: \
	HMMTrainer.java \
	NormalizeTimeStrokeFilter.java \
	StrokeFilter.java \
	../recognition/SSTrainingModel.java \
	../recognition/SSTrainingParser.java \
	../recognition/TimedStroke.java \
	../../util/aelfred/HandlerBase.java \
	../../util/java2d/Polyline2D.java

ImageTransducer.class: \
	ImageTransducer.java \
	Transducer.java \
	../JSketch.java \
	../SketchController.java \
	../SketchModel.java \
	../SketchPane.java \
	../StrokeSymbol.java \
	../Symbol.java \
	../recognition/TimedStroke.java \
	../../canvas/CanvasPane.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../util/java2d/Polyline2D.java

LineRecognizer.class: \
	ApproximateStrokeFilter.java \
	LineRecognizer.java \
	StrokeFilter.java \
	../recognition/Recognition.java \
	../recognition/RecognitionSet.java \
	../recognition/SimpleData.java \
	../recognition/StrokeRecognizer.java \
	../recognition/TimedStroke.java \
	../recognition/TypedData.java \
	../../util/java2d/Polyline2D.java \
	../../util/xml/AbstractXmlBuilder.java

LLRSceneMetric.class: \
	LLRSceneMetric.java \
	SceneMetric.java \
	../recognition/ChoiceElement.java \
	../recognition/CompositeElement.java \
	../recognition/Scene.java \
	../recognition/SceneElement.java \
	../recognition/StrokeElement.java \
	../recognition/TypedData.java \
	../../util/xml/XmlBuilder.java

LLRSceneRecognizer.class: \
	LLRSceneRecognizer.java \
	../recognition/CompositeElement.java \
	../recognition/Scene.java \
	../recognition/SceneDelta.java \
	../recognition/SceneDeltaSet.java \
	../recognition/SceneElement.java \
	../recognition/SceneRecognizer.java \
	../recognition/SimpleData.java \
	../recognition/StrokeElement.java \
	../recognition/StrokeSceneRecognizer.java \
	../recognition/TypedData.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java

ModedIncrRecognizer.class: \
	ModedIncrRecognizer.java \
	../recognition/RecognitionSet.java \
	../recognition/StrokeRecognizer.java \
	../recognition/TimedStroke.java

NormalizeLengthStrokeFilter.class: \
	NormalizeLengthStrokeFilter.java \
	StrokeFilter.java \
	../features/FEUtilities.java \
	../features/PathLengthFE.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

NormalizeTimeStrokeFilter.class: \
	NormalizeTimeStrokeFilter.java \
	StrokeFilter.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

SceneMetric.class: \
	SceneMetric.java \
	../recognition/Scene.java \
	../recognition/StrokeRecognizer.java \
	../recognition/TimedStroke.java

PanRecognizer.class: \
	ModedIncrRecognizer.java \
	PanRecognizer.java \
	../recognition/Recognition.java \
	../recognition/RecognitionSet.java \
	../recognition/StrokeRecognizer.java \
	../recognition/TimedStroke.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../../util/java2d/Polyline2D.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java \
	../../util/xml/XmlElement.java

PanZoomController.class: \
	PanZoomController.java \
	PanZoomInterpreter.java \
	../SketchController.java \
	../recognition/TimedStroke.java \
	../../canvas/CanvasLayer.java \
	../../canvas/CanvasPane.java \
	../../canvas/FigureDecorator.java \
	../../canvas/GraphicsPane.java \
	../../canvas/TransformContext.java \
	../../canvas/event/EventLayer.java \
	../../canvas/interactor/AbstractInteractor.java \
	../../canvas/interactor/CompositeInteractor.java \
	../../canvas/interactor/DragInteractor.java \
	../../canvas/interactor/Interactor.java \
	../../canvas/interactor/Manipulator.java \
	../../canvas/interactor/SelectionDragger.java \
	../../canvas/interactor/SelectionInteractor.java \
	../../canvas/interactor/SelectionRenderer.java \
	../../util/java2d/Polyline2D.java

PanZoomInterpreter.class: \
	CachingStrokeRecognizer.java \
	ModedIncrRecognizer.java \
	PanRecognizer.java \
	PanZoomController.java \
	PanZoomInterpreter.java \
	ZoomRecognizer.java \
	../SketchController.java \
	../StrokeSymbol.java \
	../classification/Classifier.java \
	../recognition/BasicStrokeRecognizer.java \
	../recognition/Recognition.java \
	../recognition/RecognitionSet.java \
	../recognition/StrokeRecognizer.java \
	../recognition/TimedStroke.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../recognition/VotingStrokeRecognizer.java \
	../../canvas/AbstractFigure.java \
	../../canvas/CanvasLayer.java \
	../../canvas/CanvasPane.java \
	../../canvas/Figure.java \
	../../canvas/FigureLayer.java \
	../../canvas/GraphicsPane.java \
	../../util/PropertyContainer.java \
	../../canvas/event/LayerEvent.java \
	../../canvas/event/LayerListener.java \
	../../canvas/event/LayerMotionListener.java \
	../../canvas/interactor/AbstractInteractor.java \
	../../canvas/interactor/Interactor.java \
	../../canvas/interactor/ShapedFigure.java \
	../../canvas/toolbox/BasicFigure.java \
	../../util/java2d/Polyline2D.java \
	../../util/xml/AbstractXmlBuilder.java

RemoveDupPtsStrokeFilter.class: \
	RemoveDupPtsStrokeFilter.java \
	StrokeFilter.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

RetypingRecognizer.class: \
	RetypingRecognizer.java \
	../recognition/Recognition.java \
	../recognition/RecognitionSet.java \
	../recognition/SimpleData.java \
	../recognition/StrokeRecognizer.java \
	../recognition/TimedStroke.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../../util/xml/AbstractXmlBuilder.java

ScribbleRecognizer.class: \
	ApproximateStrokeFilter.java \
	ScribbleRecognizer.java \
	StrokeFilter.java \
	../features/AspectRatioFE.java \
	../features/CornerFE.java \
	../features/StrokeBBox.java \
	../features/SumOfAbsDeltaRatioFE.java \
	../recognition/Recognition.java \
	../recognition/RecognitionSet.java \
	../recognition/SimpleData.java \
	../recognition/StrokeRecognizer.java \
	../recognition/TimedStroke.java \
	../recognition/TypedData.java \
	../../util/java2d/Polyline2D.java \
	../../util/xml/AbstractXmlBuilder.java

SceneTranslator.class: \
	SceneTranslator.java \
	../recognition/BasicScene.java \
	../recognition/CompositeElement.java \
	../recognition/SSTrainingParser.java \
	../recognition/Scene.java \
	../recognition/SceneElement.java \
	../recognition/SceneParser.java \
	../recognition/SceneWriter.java \
	../recognition/SimpleData.java \
	../recognition/StrokeElement.java \
	../recognition/TimedStroke.java \
	../recognition/TypedData.java \
	../../util/LoggableOp.java \
	../../util/ModelParser.java \
	../../util/aelfred/HandlerBase.java \
	../../util/java2d/Polyline2D.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlDocument.java \
	../../util/xml/XmlElement.java \
	../../util/xml/XmlReader.java

SketchTransducer.class: \
	SketchTransducer.java \
	Transducer.java \
	../SketchModel.java \
	../SketchWriter.java

StrokeFilter.class: \
	StrokeFilter.java \
	../recognition/TimedStroke.java

TableTransducer.class: \
	TableTransducer.java \
	Transducer.java \
	../SketchModel.java

TextBlock.class: \
	TextAnnotations.java \
	TextBlock.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java

TextData.class: \
	TextData.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java \
	../../util/xml/XmlElement.java

TextLine.class: \
	TextAnnotations.java \
	TextLine.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java

TextAnnotations.class: \
	TextAnnotations.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java \
	../../util/xml/XmlElement.java

TrainingToMatlab.class: \
	TrainingToMatlab.java \
	../recognition/SSTrainingModel.java \
	../recognition/SSTrainingParser.java \
	../recognition/TimedStroke.java \
	../../util/aelfred/HandlerBase.java \
	../../util/java2d/Polyline2D.java

Transducer.class: \
	Transducer.java \
	../SketchModel.java

ZoomRecognizer.class: \
	ModedIncrRecognizer.java \
	ZoomRecognizer.java \
	../recognition/Recognition.java \
	../recognition/RecognitionSet.java \
	../recognition/StrokeRecognizer.java \
	../recognition/TimedStroke.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../../util/java2d/Polyline2D.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java \
	../../util/xml/XmlElement.java


