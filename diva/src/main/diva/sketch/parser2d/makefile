#
# $Id: makefile,v 1.24 2002/05/16 20:37:39 johnr Exp $
#
# Makefile for diva.sketch
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	AbstractRule.java \
	AndConstraint.java \
	AngleRelation.java \
	AreaRatioRelation.java \
	BasicConstraint.java \
	DeltaXRelation.java \
	DeltaYRelation.java \
	DistanceRelation.java \
	Grammar2D.java \
	HeightRatioRelation.java \
	NativeResults.java \
	OrConstraint.java \
	OverlapRelation.java \
	ParsingSceneRecognizer.java \
	Relation.java \
	RelationConstraint.java \
	RelationUtilities.java \
	Rule.java \
	SingleRule.java \
	WidthRatioRelation.java 

# Include the shared compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
AbstractRule.class: \
	AbstractRule.java \
	Rule.java \
	../recognition/CompositeElement.java \
	../recognition/Scene.java \
	../recognition/SceneElement.java \
	../recognition/Type.java

AndConstraint.class: \
	AndConstraint.java \
	RelationConstraint.java \
	../recognition/SceneElement.java

AngleRelation.class: \
	AngleRelation.java \
	Relation.java \
	RelationUtilities.java \
	../recognition/SceneElement.java

AreaRatioRelation.class: \
	AreaRatioRelation.java \
	Relation.java \
	../recognition/SceneElement.java

BasicConstraint.class: \
	BasicConstraint.java \
	Relation.java \
	RelationConstraint.java \
	../recognition/SceneElement.java

DeltaXRelation.class: \
	DeltaXRelation.java \
	Relation.java \
	RelationUtilities.java \
	../recognition/SceneElement.java

DeltaYRelation.class: \
	DeltaYRelation.java \
	Relation.java \
	RelationUtilities.java \
	../recognition/SceneElement.java

DistanceRelation.class: \
	DistanceRelation.java \
	Relation.java \
	RelationUtilities.java \
	../recognition/SceneElement.java

Grammar2D.class: \
	Grammar2D.java \
	Rule.java \
	../recognition/Scene.java \
	../recognition/SceneElement.java \
	../recognition/Type.java

HeightRatioRelation.class: \
	HeightRatioRelation.java \
	Relation.java \
	../recognition/SceneElement.java

NativeResults.class: \
	NativeResults.java \
	../recognition/TypedData.java

OrConstraint.class: \
	OrConstraint.java \
	RelationConstraint.java \
	../recognition/SceneElement.java

OverlapRelation.class: \
	OverlapRelation.java \
	Relation.java \
	../recognition/SceneElement.java

ParsingSceneRecognizer.class: \
	Grammar2D.java \
	ParsingSceneRecognizer.java \
	Rule.java \
	../recognition/CompositeElement.java \
	../recognition/Scene.java \
	../recognition/SceneDelta.java \
	../recognition/SceneDeltaSet.java \
	../recognition/SceneElement.java \
	../recognition/SceneRecognizer.java \
	../recognition/StrokeElement.java \
	../recognition/Type.java \
	../recognition/TypedData.java \
	../../util/ArrayIterator.java \
	../../util/Filter.java \
	../../util/FilteredIterator.java \
	../../util/xml/XmlBuilder.java

Relation.class: \
	Relation.java \
	../recognition/SceneElement.java

RelationConstraint.class: \
	RelationConstraint.java \
	../recognition/SceneElement.java

RelationUtilities.class: \
	AngleRelation.java \
	AreaRatioRelation.java \
	DeltaXRelation.java \
	DeltaYRelation.java \
	DistanceRelation.java \
	HeightRatioRelation.java \
	OverlapRelation.java \
	Relation.java \
	RelationUtilities.java \
	WidthRatioRelation.java

Rule.class: \
	Rule.java \
	../recognition/CompositeElement.java \
	../recognition/Scene.java \
	../recognition/SceneElement.java \
	../recognition/Type.java

SingleRule.class: \
	Rule.java \
	SingleRule.java \
	../recognition/CompositeElement.java \
	../recognition/Scene.java \
	../recognition/SceneElement.java \
	../recognition/Type.java

WidthRatioRelation.class: \
	Relation.java \
	WidthRatioRelation.java \
	../recognition/SceneElement.java


