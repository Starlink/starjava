#
# $Id: makefile,v 1.10 2002/05/16 20:37:38 johnr Exp $
#
# Makefile for diva.sketch.classification
#
DIVA  = ../..
include $(DIVA)/diva.mk

SUBPACKAGES = 

# Java files in this directory
JSRCS = \
	AbstractClassifier.java \
	BayesClassifier.java \
	Classification.java \
	Classifier.java \
	ClassifierException.java \
	CrossValidation.java \
	FeatureSet.java \
	GaussianWeightSet.java \
	KNNClassifier.java \
	RubineClassifier.java \
	TrainableClassifier.java \
	TrainingSet.java \
	WeightedEuclideanClassifier.java \
	WeightSet.java 

# Include the shared compilation rules
include $(DIVA)/compile.mk


### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
AbstractClassifier.class: \
	AbstractClassifier.java \
	Classification.java \
	Classifier.java \
	ClassifierException.java \
	FeatureSet.java \
	GaussianWeightSet.java \
	TrainableClassifier.java \
	TrainingSet.java \
	WeightSet.java

BayesClassifier.class: \
	AbstractClassifier.java \
	BayesClassifier.java \
	Classification.java \
	Classifier.java \
	ClassifierException.java \
	FeatureSet.java \
	TrainableClassifier.java \
	TrainingSet.java \
	WeightSet.java

Classification.class: \
	Classification.java

Classifier.class: \
	Classification.java \
	Classifier.java \
	ClassifierException.java \
	FeatureSet.java

ClassifierException.class: \
	ClassifierException.java

CrossValidation.class: \
	Classification.java \
	Classifier.java \
	ClassifierException.java \
	CrossValidation.java \
	FeatureSet.java \
	TrainableClassifier.java \
	TrainingSet.java

FeatureSet.class: \
	FeatureSet.java

GaussianWeightSet.class: \
	ClassifierException.java \
	FeatureSet.java \
	GaussianWeightSet.java \
	WeightSet.java

KNNClassifier.class: \
	Classification.java \
	Classifier.java \
	ClassifierException.java \
	FeatureSet.java \
	KNNClassifier.java \
	TrainableClassifier.java \
	TrainingSet.java

RubineClassifier.class: \
	Classification.java \
	Classifier.java \
	ClassifierException.java \
	FeatureSet.java \
	RubineClassifier.java \
	TrainableClassifier.java \
	TrainingSet.java

TrainableClassifier.class: \
	Classifier.java \
	ClassifierException.java \
	TrainableClassifier.java \
	TrainingSet.java

TrainingSet.class: \
	FeatureSet.java \
	TrainingSet.java \
	../../util/IteratorAdapter.java \
	../../util/NullIterator.java

WeightedEuclideanClassifier.class: \
	AbstractClassifier.java \
	Classification.java \
	Classifier.java \
	ClassifierException.java \
	FeatureSet.java \
	TrainableClassifier.java \
	WeightSet.java \
	WeightedEuclideanClassifier.java

WeightSet.class: \
	ClassifierException.java \
	FeatureSet.java \
	WeightSet.java \
	../../util/IteratorAdapter.java \
	../../util/NullIterator.java


