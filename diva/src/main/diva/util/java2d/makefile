#
# $Id: makefile,v 1.14 2002/09/26 10:27:42 johnr Exp $
#
# Makefile for diva.util.java2d
#
DIVA  = ../..
include $(DIVA)/diva.mk

JSRCS = \
	Polygon2D.java \
	PolygonIterator.java \
	Polyline2D.java \
	PolylineIterator.java \
	ShapeUtilities.java \
\
	AbstractPaintedGraphic.java \
	PaintedGraphic.java \
	PaintedImage.java \
	PaintedList.java \
	PaintedObject.java \
	PaintedPath.java \
	PaintedShape.java \
	PaintedString.java


# Include the shared compilation rules
include $(DIVA)/compile.mk


# A rule to run this as a program
ShapeUtilities: ShapeUtilities.class
	$(JAVA) -classpath $(CLASSPATH) diva.util.java2d.ShapeUtilities

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
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

AbstractPaintedGraphic.class: \
	AbstractPaintedGraphic.java \
	PaintedGraphic.java \
	PaintedObject.java

PaintedGraphic.class: \
	PaintedGraphic.java \
	PaintedObject.java

PaintedImage.class: \
	PaintedImage.java \
	PaintedObject.java \
	../../canvas/CanvasUtilities.java

PaintedList.class: \
	PaintedList.java \
	PaintedObject.java

PaintedObject.class: \
	PaintedObject.java

PaintedPath.class: \
	AbstractPaintedGraphic.java \
	PaintedGraphic.java \
	PaintedObject.java \
	PaintedPath.java \
	ShapeUtilities.java

PaintedShape.class: \
	AbstractPaintedGraphic.java \
	PaintedGraphic.java \
	PaintedObject.java \
	PaintedPath.java \
	PaintedShape.java \
	ShapeUtilities.java

PaintedString.class: \
	PaintedObject.java \
	PaintedString.java


