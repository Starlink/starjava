# ESO Archive
# 
# $Id: Makefile,v 1.10 2002/08/08 22:40:23 gillies Exp $
# 
# who             when        what
# --------------  ----------  ----------------------------------------
# Allan Brighton  1999/12/08  Created

# Source files to compile
SOURCES = \
	ImageChangeEvent.java \
	SimpleRenderedImage.java \
	EmptyRenderedImage.java \
	ImageColorITTs.java \
	ImageColorLUTs.java \
	ImageColormap.java \
	ImageLookup.java \
	MyTileCache.java \
	ImageUtil.java \
	ImageProcessor.java \
	GifEncoder.java \
	GreyscaleFilter.java \
	ImageEncoder.java \
	ImageHistogram.java \
	BasicImageReadableProcessor.java


# Java package name
PACKAGE_DIR = jsky/image

# Path to top level dir
TOP = ../../..

# include master makefile
include $(TOP)/conf/master.mk

clean:: resource_clean

# (see ./gui for tests)
test: all
	(cd gui; $(MAKE) test)

