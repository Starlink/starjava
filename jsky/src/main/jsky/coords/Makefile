# ESO Archive
# 
# $Id: Makefile,v 1.5 2002/08/08 22:19:19 gillies Exp $
# 
# who             when        what
# --------------  ----------  ----------------------------------------
# Allan Brighton  1999/12/08  Created

# Source files to compile
SOURCES = \
	WorldCoordinateConverter.java \
	CoordinateConverter.java \
	HMS.java \
	DMS.java \
	Trigod.java \
	Cotr.java \
	JPrec.java \
	Coordinates.java \
	ImageCoords.java \
	CoordinateRadius.java \
	WorldCoordinates.java \
	WorldCoords.java \
	platepos.java \
	slasubs.java \
	wcscon.java \
	worldpos.java \
	WCSTransform.java \
	NamedCoordinates.java \
	WCSKeywordProvider.java \


# Java package name
PACKAGE_DIR = jsky/coords

# Path to top level dir
TOP = ../../..

# include master makefile
include $(TOP)/conf/master.mk

# tests
test: 

test1 HMS: all
	java $(JVMFLAGS) jsky.coords.HMS

test2 DMS: all
	java $(JVMFLAGS) jsky.coords.DMS

test3 WorldCoords: all
	java $(JVMFLAGS) jsky.coords.WorldCoords

