#
# $Id: makefile,v 1.44 2002/05/19 22:05:11 johnr Exp $
#
# Makefile for diva.util
#
DIVA  = ..
include $(DIVA)/diva.mk

# Subpackage directories
SUBPACKAGES = \
	aelfred \
	gui \
	java2d \
	jester \
	xml \
	$(OPTUTILPKG)

# Java files in this directory
JSRCS = \
	ArrayIterator.java \
	BasicPropertyContainer.java \
	CompoundIterator.java \
	Filter.java \
	FilteredIterator.java \
	FilteredArrayIterator.java \
	IteratorAdapter.java \
	IteratorIterator.java \
	IteratorUtilities.java \
	LoggableOp.java \
	ModelParser.java \
	ModelWriter.java \
	NullArrayIterator.java \
	NullIterator.java \
	PropertyChangeMulticaster.java \
	PropertyContainer.java \
	ProxyIterator.java \
	ReverseIterator.java \
	SemanticObjectContainer.java \
	UnitIterator.java \
	UserObjectContainer.java 

#	PropertyIndexSet.java \
#	VisualObjectContainer.java
#	NotFilter.java \
#	OrFilter.java \
#	AndFilter.java \
#	UserObjectChangeEvent.java \
#	UserObjectChangeListener.java \
#	SemanticObjectChangeEvent.java \
#	SemanticObjectChangeListener.java \

# Include the shared compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
AndFilter.class: \
	AndFilter.java \
	Filter.java

ArrayIterator.class: \
	ArrayIterator.java \
	IteratorAdapter.java

BasicPropertyContainer.class: \
	BasicPropertyContainer.java \
	PropertyContainer.java

CompoundIterator.class: \
	CompoundIterator.java

Filter.class: \
	Filter.java

FilteredIterator.class: \
	Filter.java \
	FilteredIterator.java \
	IteratorAdapter.java

FilteredArrayIterator.class: \
	FilteredArrayIterator.java \
	IteratorAdapter.java \
	NullArrayIterator.java

IteratorAdapter.class: \
	IteratorAdapter.java

IteratorIterator.class: \
	IteratorAdapter.java \
	IteratorIterator.java

IteratorUtilities.class: \
	Filter.java \
	IteratorUtilities.java

LoggableOp.class: \
	LoggableOp.java \
	aelfred/XmlParser.java

ModelParser.class: \
	ModelParser.java

ModelWriter.class: \
	ModelWriter.java

NotFilter.class: \
	Filter.java \
	NotFilter.java

NullIterator.class: \
	IteratorAdapter.java \
	NullIterator.java

NullArrayIterator.class: \
	IteratorAdapter.java \
	NullArrayIterator.java

OrFilter.class: \
	Filter.java \
	OrFilter.java

PropertyChangeMulticaster.class: \
	PropertyChangeMulticaster.java

PropertyContainer.class: \
	PropertyContainer.java

PropertyIndexSet.class: \
	PropertyIndexSet.java

ProxyIterator.class: \
	IteratorAdapter.java \
	ProxyIterator.java

ReverseIterator.class: \
	IteratorAdapter.java \
	ReverseIterator.java

SemanticObjectChangeEvent.class: \
	SemanticObjectChangeEvent.java

SemanticObjectChangeListener.class: \
	SemanticObjectChangeEvent.java \
	SemanticObjectChangeListener.java

SemanticObjectContainer.class: \
	SemanticObjectContainer.java

UnitIterator.class: \
	IteratorAdapter.java \
	UnitIterator.java

UserObjectChangeEvent.class: \
	UserObjectChangeEvent.java

UserObjectChangeListener.class: \
	UserObjectChangeEvent.java \
	UserObjectChangeListener.java

UserObjectContainer.class: \
	UserObjectContainer.java

VisualObjectContainer.class: \
	VisualObjectContainer.java


