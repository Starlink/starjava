#
# $Id: makefile,v 1.10 2000/07/17 21:30:46 neuendor Exp $
#
# Makefile for diva.util.java2d
#
DIVA  = ../..
include $(DIVA)/diva.mk

JSRCS = \
	AbstractPaintedGraphic.java \
	PaintedGraphic.java \
	PaintedList.java \
	PaintedObject.java \
	PaintedPath.java \
	PaintedShape.java \
	PaintedString.java \
	Polygon2D.java \
	PolygonIterator.java \
	Polyline2D.java \
	PolylineIterator.java \
	ShapeUtilities.java

# Include the shared compilation rules
include $(DIVA)/compile.mk


# A rule to run this as a program
ShapeUtilities: ShapeUtilities.class
	$(JAVA) -classpath $(CLASSPATH) diva.util.java2d.ShapeUtilities

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
PaintedList.class: \
	PaintedList.java \
	PaintedObject.java

PaintedObject.class: \
	PaintedObject.java

PaintedPath.class: \
	PaintedObject.java \
	PaintedPath.java

PaintedShape.class: \
	PaintedObject.java \
	PaintedPath.java \
	PaintedShape.java

PaintedString.class: \
	PaintedObject.java \
	PaintedString.java

Polygon2D.class: \
	Polygon2D.java \
	PolygonIterator.java

PolygonIterator.class: \
	Polygon2D.java \
	PolygonIterator.java

Polyline2D.class: \
	Polyline2D.java \
	PolylineIterator.java

PolylineIterator.class: \
	Polyline2D.java \
	PolylineIterator.java

ShapeUtilities.class: \
	Polygon2D.java \
	Polyline2D.java \
	ShapeUtilities.java


