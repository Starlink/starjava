# ESO Archive
# 
# $Id: Makefile,v 1.6 2002/08/08 22:40:23 gillies Exp $
# 
# who             when        what
# --------------  ----------  ----------------------------------------
# Allan Brighton  1999/12/08  Created

# Source files to compile
SOURCES = \
	ImageFigure.java \
	LabeledImageFigure.java \
	DivaImageGraphics.java \
	ImageLayer.java \
	ImageLabel.java \
	ImageFigureGroup.java \
	SelectionDragger.java \
	ShapeUtil.java \
	MeasureBand.java \


# Java package name
PACKAGE_DIR = jsky/image/graphics

# Path to top level dir
TOP = ../../../..

# include master makefile
include $(TOP)/conf/master.mk


# image file to test with
IMAGE = ../gui/test/ngc1275.fits

# tests
test: test1

test1 DivaImageGraphics: all
	java $(JVMFLAGS) jsky.image.graphics.DivaImageGraphics $(IMAGE)

test2 ShapeUtil: all
	java $(JVMFLAGS) jsky.image.graphics.ShapeUtil

test3 MeasureBand: all
	java $(JVMFLAGS) jsky.image.graphics.MeasureBand $(IMAGE)

test4: all
	(cd ../../app/jskycat; $(MAKE) test)
