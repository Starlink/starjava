# ESO Archive
# 
# $Id: Makefile,v 1.6 2002/08/08 22:40:23 gillies Exp $
# 
# who             when        what
# --------------  ----------  ----------------------------------------
# Allan Brighton  1999/12/08  Created

# Source files to compile
SOURCES = \
	FITSGraphics.java \
	CanvasDraw.java \
	ImageGraphicsMenu.java


# Java package name
PACKAGE_DIR = jsky/image/graphics/gui

# Resources to be installed in the package directory
PACKAGE_RESOURCES = \
	i18n/gui.properties

# Path to top level dir
TOP = ../../../../..

# include master makefile
include $(TOP)/conf/master.mk

clean:: resource_clean


# image file to test with
IMAGE = ../../gui/test/ngc1275.fits

# tests
test: test1

test1 ImageGraphicsMenu: all
	java $(JVMFLAGS) jsky.image.graphics.gui.ImageGraphicsMenu $(IMAGE)



