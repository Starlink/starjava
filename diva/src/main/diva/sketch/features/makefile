#
# $Id: makefile,v 1.19 2002/05/16 20:37:39 johnr Exp $
#
# Makefile for diva.sketch.features
#
DIVA  = ../..
include $(DIVA)/diva.mk

SUBPACKAGES = 

# Java files in this directory
JSRCS = \
	AreaRatioFE.java \
	AspectRatioFE.java \
	BBoxDiagonalAngleFE.java \
	BBoxDiagonalLengthFE.java \
	ConvexHull.java \
	CornerFE.java \
	CosFirstLastPtsFE.java \
	CosInitAngleFE.java \
	DistanceStartEndPtsFE.java \
	DurationFE.java \
	FeatureExtractor.java \
	FEUtilities.java \
	MaxSpeedFE.java \
	PathLengthFE.java \
	PathRatioFE.java \
	SineFirstLastPtsFE.java \
	SineInitAngleFE.java \
	StrokeBBox.java \
	StrokeHull.java \
	SumOfAbsDeltaRatioFE.java \
	SumOfAbsoluteAnglesFE.java \
	SumOfAnglesFE.java \
	SumOfSquaredAnglesFE.java 

JAVAC_FLAGS = -deprecation

# Include the shared compilation rules
include $(DIVA)/compile.mk


### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
AreaRatioFE.class: \
	AreaRatioFE.java \
	ConvexHull.java \
	FeatureExtractor.java \
	StrokeBBox.java \
	StrokeHull.java \
	../recognition/TimedStroke.java

AspectRatioFE.class: \
	AspectRatioFE.java \
	FeatureExtractor.java \
	StrokeBBox.java \
	../recognition/TimedStroke.java

BBoxDiagonalAngleFE.class: \
	BBoxDiagonalAngleFE.java \
	FeatureExtractor.java \
	StrokeBBox.java \
	../recognition/TimedStroke.java

BBoxDiagonalLengthFE.class: \
	BBoxDiagonalLengthFE.java \
	FeatureExtractor.java \
	StrokeBBox.java \
	../recognition/TimedStroke.java

ConvexHull.class: \
	ConvexHull.java \
	FEUtilities.java

CornerFE.class: \
	CornerFE.java \
	FEUtilities.java \
	FeatureExtractor.java \
	PathLengthFE.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

CosFirstLastPtsFE.class: \
	CosFirstLastPtsFE.java \
	FEUtilities.java \
	FeatureExtractor.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

CosInitAngleFE.class: \
	CosInitAngleFE.java \
	FEUtilities.java \
	FeatureExtractor.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

DistanceStartEndPtsFE.class: \
	DistanceStartEndPtsFE.java \
	FEUtilities.java \
	FeatureExtractor.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

DurationFE.class: \
	DurationFE.java \
	FeatureExtractor.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

FeatureExtractor.class: \
	FeatureExtractor.java \
	../recognition/TimedStroke.java

FEUtilities.class: \
	FEUtilities.java

MaxSpeedFE.class: \
	FeatureExtractor.java \
	MaxSpeedFE.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

PathLengthFE.class: \
	FEUtilities.java \
	FeatureExtractor.java \
	PathLengthFE.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

PathRatioFE.class: \
	ConvexHull.java \
	FeatureExtractor.java \
	PathLengthFE.java \
	PathRatioFE.java \
	StrokeHull.java \
	../recognition/TimedStroke.java

SineFirstLastPtsFE.class: \
	FEUtilities.java \
	FeatureExtractor.java \
	SineFirstLastPtsFE.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

SineInitAngleFE.class: \
	FEUtilities.java \
	FeatureExtractor.java \
	SineInitAngleFE.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

StrokeBBox.class: \
	StrokeBBox.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

StrokeHull.class: \
	ConvexHull.java \
	StrokeHull.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

SumOfAbsDeltaRatioFE.class: \
	FeatureExtractor.java \
	SumOfAbsDeltaRatioFE.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

SumOfAbsoluteAnglesFE.class: \
	FeatureExtractor.java \
	SumOfAbsoluteAnglesFE.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

SumOfAnglesFE.class: \
	FeatureExtractor.java \
	SumOfAnglesFE.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java

SumOfSquaredAnglesFE.class: \
	FeatureExtractor.java \
	SumOfSquaredAnglesFE.java \
	../recognition/TimedStroke.java \
	../../util/java2d/Polyline2D.java


