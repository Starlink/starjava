# ESO Archive
# 
# $Id: Makefile,v 1.13 2002/08/08 22:40:23 gillies Exp $
# 
# who             when        what
# --------------  ----------  ----------------------------------------
# Allan Brighton  1999/12/08  Created

# Source files to compile
SOURCES = \
	BasicImageDisplay.java \
	ImageCoordinateConverter.java \
	GraphicsImageDisplay.java \
	DivaGraphicsImageDisplay.java \
	ImageDisplay.java \
	MainImageDisplay.java \
	ImageDisplayStatusPanel.java \
	DivaMainImageDisplay.java \
	ImagePanner.java \
	ImageZoom.java \
	ImageColorbar.java \
	ImageDisplayPanel.java \
	ImageColors.java \
	ImageColorsFrame.java \
	ImageColorsInternalFrame.java \
	ImageProperties.java \
	ImagePropertiesFrame.java \
	ImagePropertiesInternalFrame.java \
	ImageCutLevels.java \
	ImageCutLevelsFrame.java \
	ImageCutLevelsInternalFrame.java \
	ImageDisplayControl.java \
	ImageDisplayControlFrame.java \
	ImageDisplayControlInternalFrame.java \
	ImageDisplayMenuBar.java \
	ImageDisplayToolBar.java \
	ImageSaveDialog.java \
	ImagePrintDialog.java \
	ImageHistoryItem.java \
	PickObjectGUI.java \
	PickObject.java \
	PickObjectFrame.java \
	PickObjectInternalFrame.java \
	PickObjectStatistics.java \
	ImageGraphicsHandler.java

# Java package name
PACKAGE_DIR = jsky/image/gui

# Resources to be installed in the package directory
PACKAGE_RESOURCES = \
	i18n/gui.properties \
	i18n/gui_gb.properties \
	i18n/jb.properties \
	i18n/jb_gb.properties

# Path to top level dir
TOP = ../../../..

# include master makefile
include $(TOP)/conf/master.mk

clean:: resource_clean

# image file to test with
IMAGE = test/ngc1275.fits

# tests
test: test2

test0 ImageDisplay: all
	java $(JVMFLAGS) jsky.image.gui.ImageDisplay $(IMAGE)

test2 ImageDisplayControlFrame: all
	java $(JVMFLAGS) jsky.image.gui.ImageDisplayControlFrame $(IMAGE)

test2c: all
	java $(JVMFLAGS) jsky.image.gui.ImageDisplayControlFrame

test2d: all
	java $(JVMFLAGS) jsky.image.gui.ImageDisplayControlFrame "http://archive.eso.org/dss/dss?ra=13:29:51.700&dec=+47:11:57.00&mime-type=application/x-fits&x=5&y=5"

test4 DivaGraphicsImageDisplay: all
	java $(JVMFLAGS) jsky.image.gui.DivaGraphicsImageDisplay $(IMAGE)

