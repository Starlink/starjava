#
# $Id: makefile,v 1.10 2002/05/16 20:37:40 johnr Exp $
#
# Makefile for diva.sketch.recognition
#
DIVA  = ../..
include $(DIVA)/diva.mk

SUBPACKAGES = 

# Java files in this directory
JSRCS = \
	BasicStrokeRecognizer.java \
	BasicScene.java \
	ChoiceElement.java \
	CompositeElement.java \
	MSTrainingModel.java \
	MSTrainingParser.java \
	MSTrainingWriter.java \
	MultiStrokeRecognizer.java \
	Recognition.java \
	RecognitionSet.java \
	Scene.java \
	SceneDelta.java \
	SceneDeltaSet.java \
	SceneElement.java \
	SceneParser.java \
	SceneRecognizer.java \
	SceneWriter.java \
	SceneBuilder.java \
	SimpleData.java \
	SSTrainingModel.java \
	SSTrainingParser.java \
	SSTrainingWriter.java \
	StrokeElement.java \
	StrokeRecognizer.java \
	StrokeSceneRecognizer.java \
	TimedStroke.java \
	Type.java \
	TypedData.java \
	VotingSceneRecognizer.java \
	VotingStrokeRecognizer.java 

# Include the shared compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
BasicStrokeRecognizer.class: \
	BasicStrokeRecognizer.java \
	Recognition.java \
	RecognitionSet.java \
	SSTrainingModel.java \
	SSTrainingParser.java \
	SimpleData.java \
	StrokeRecognizer.java \
	TimedStroke.java \
	TypedData.java \
	../classification/AbstractClassifier.java \
	../classification/Classification.java \
	../classification/Classifier.java \
	../classification/ClassifierException.java \
	../classification/FeatureSet.java \
	../classification/TrainableClassifier.java \
	../classification/TrainingSet.java \
	../classification/WeightedEuclideanClassifier.java \
	../features/AreaRatioFE.java \
	../features/AspectRatioFE.java \
	../features/BBoxDiagonalAngleFE.java \
	../features/CornerFE.java \
	../features/CosFirstLastPtsFE.java \
	../features/CosInitAngleFE.java \
	../features/DistanceStartEndPtsFE.java \
	../features/FeatureExtractor.java \
	../features/SineFirstLastPtsFE.java \
	../features/SineInitAngleFE.java \
	../features/SumOfAbsoluteAnglesFE.java \
	../features/SumOfAnglesFE.java \
	../features/SumOfSquaredAnglesFE.java \
	../toolbox/ApproximateStrokeFilter.java \
	../toolbox/StrokeFilter.java \
	../../util/aelfred/HandlerBase.java \
	../../util/java2d/Polyline2D.java \
	../../util/xml/AbstractXmlBuilder.java

BasicScene.class: \
	BasicScene.java \
	ChoiceElement.java \
	CompositeElement.java \
	Scene.java \
	SceneElement.java \
	SimpleData.java \
	StrokeElement.java \
	TimedStroke.java \
	Type.java \
	TypedData.java \
	../../util/ArrayIterator.java \
	../../util/IteratorIterator.java \
	../../util/NullIterator.java \
	../../util/java2d/Polyline2D.java \
	../../util/xml/XmlBuilder.java

ChoiceElement.class: \
	ChoiceElement.java \
	CompositeElement.java \
	SceneElement.java \
	Type.java

CompositeElement.class: \
	CompositeElement.java \
	SceneElement.java \
	TypedData.java

MSTrainingModel.class: \
	MSTrainingModel.java \
	TimedStroke.java \
	../../util/IteratorAdapter.java \
	../../util/NullIterator.java

MSTrainingParser.class: \
	MSTrainingModel.java \
	MSTrainingParser.java \
	SSTrainingParser.java \
	TimedStroke.java \
	../../util/ModelParser.java \
	../../util/aelfred/HandlerBase.java \
	../../util/aelfred/XmlHandler.java \
	../../util/aelfred/XmlParser.java

MSTrainingWriter.class: \
	MSTrainingModel.java \
	MSTrainingParser.java \
	MSTrainingWriter.java \
	SSTrainingWriter.java \
	TimedStroke.java \
	../../util/ModelWriter.java

MultiStrokeRecognizer.class: \
	MultiStrokeRecognizer.java \
	RecognitionSet.java \
	StrokeRecognizer.java \
	TimedStroke.java

Recognition.class: \
	Recognition.java \
	Type.java \
	TypedData.java \
	../../util/xml/XmlBuilder.java

RecognitionSet.class: \
	Recognition.java \
	RecognitionSet.java \
	Type.java \
	TypedData.java \
	../../util/IteratorAdapter.java \
	../../util/NullIterator.java \
	../../util/xml/XmlBuilder.java

Scene.class: \
	CompositeElement.java \
	Scene.java \
	SceneElement.java \
	SimpleData.java \
	StrokeElement.java \
	TimedStroke.java \
	Type.java \
	TypedData.java

SceneDelta.class: \
	CompositeElement.java \
	Scene.java \
	SceneDelta.java \
	SceneElement.java \
	TypedData.java \
	../../util/ArrayIterator.java \
	../../util/IteratorAdapter.java \
	../../util/NullIterator.java

SceneDeltaSet.class: \
	SceneDelta.java \
	SceneDeltaSet.java \
	../../util/IteratorAdapter.java \
	../../util/NullIterator.java

SceneElement.class: \
	SceneElement.java

SceneParser.class: \
	SceneBuilder.java \
	SceneParser.java \
	../../resource/DefaultBundle.java \
	../../resource/RelativeBundle.java \
	../../util/LoggableOp.java \
	../../util/ModelParser.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/CompositeBuilder.java \
	../../util/xml/XmlDocument.java \
	../../util/xml/XmlElement.java \
	../../util/xml/XmlReader.java

SceneRecognizer.class: \
	Scene.java \
	SceneDeltaSet.java \
	SceneRecognizer.java \
	StrokeElement.java

SceneWriter.class: \
	Scene.java \
	SceneBuilder.java \
	SceneElement.java \
	SceneWriter.java \
	../../resource/DefaultBundle.java \
	../../resource/RelativeBundle.java \
	../../util/LoggableOp.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/CompositeBuilder.java \
	../../util/xml/XmlDocument.java \
	../../util/xml/XmlElement.java \
	../../util/xml/XmlWriter.java

SceneBuilder.class: \
	BasicScene.java \
	CompositeElement.java \
	SSTrainingParser.java \
	SSTrainingWriter.java \
	Scene.java \
	SceneBuilder.java \
	SceneElement.java \
	StrokeElement.java \
	TimedStroke.java \
	TypedData.java \
	../../util/aelfred/HandlerBase.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java \
	../../util/xml/XmlElement.java

SimpleData.class: \
	SimpleData.java \
	Type.java \
	TypedData.java \
	../../util/xml/AbstractXmlBuilder.java \
	../../util/xml/XmlBuilder.java \
	../../util/xml/XmlElement.java

SSTrainingModel.class: \
	SSTrainingModel.java \
	TimedStroke.java \
	../../util/IteratorAdapter.java \
	../../util/NullIterator.java \
	../../util/java2d/Polyline2D.java

SSTrainingParser.class: \
	SSTrainingModel.java \
	SSTrainingParser.java \
	TimedStroke.java \
	../../util/ModelParser.java \
	../../util/aelfred/HandlerBase.java \
	../../util/aelfred/XmlHandler.java \
	../../util/aelfred/XmlParser.java \
	../../util/java2d/Polyline2D.java

SSTrainingWriter.class: \
	SSTrainingModel.java \
	SSTrainingParser.java \
	SSTrainingWriter.java \
	TimedStroke.java \
	../../util/ModelWriter.java \
	../../util/java2d/Polyline2D.java

StrokeElement.class: \
	SceneElement.java \
	StrokeElement.java \
	TimedStroke.java

StrokeRecognizer.class: \
	RecognitionSet.java \
	StrokeRecognizer.java \
	TimedStroke.java

StrokeSceneRecognizer.class: \
	CompositeElement.java \
	Recognition.java \
	RecognitionSet.java \
	Scene.java \
	SceneDelta.java \
	SceneDeltaSet.java \
	SceneElement.java \
	SceneRecognizer.java \
	StrokeElement.java \
	StrokeRecognizer.java \
	StrokeSceneRecognizer.java \
	Type.java \
	TypedData.java \
	../../util/xml/XmlBuilder.java

TimedStroke.class: \
	TimedStroke.java \
	../../util/BasicPropertyContainer.java \
	../../util/PropertyContainer.java \
	../../util/java2d/Polyline2D.java

Type.class: \
	SimpleData.java \
	Type.java

TypedData.class: \
	Type.java \
	TypedData.java \
	../../util/xml/XmlBuilder.java

VotingSceneRecognizer.class: \
	CompositeElement.java \
	Scene.java \
	SceneDelta.java \
	SceneDeltaSet.java \
	SceneElement.java \
	SceneRecognizer.java \
	StrokeElement.java \
	Type.java \
	TypedData.java \
	VotingSceneRecognizer.java \
	../../util/xml/XmlBuilder.java

VotingStrokeRecognizer.class: \
	Recognition.java \
	RecognitionSet.java \
	StrokeRecognizer.java \
	TimedStroke.java \
	Type.java \
	VotingStrokeRecognizer.java


