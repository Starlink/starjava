# ESO Archive
# 
# $Id: Makefile,v 1.3 2002/08/08 22:19:20 gillies Exp $
# 
# who             when        what
# --------------  ----------  ----------------------------------------
# Allan Brighton  1999/12/10  Created

# Source files to compile
SOURCES = \
	FITSHDUChooser.java \
	FITSHDUChooserFrame.java \
	FITSHDUChooserInternalFrame.java \
	FITSKeywords.java \
	FITSKeywordsFrame.java \
	FITSKeywordsInternalFrame.java \


# Java package name
PACKAGE_DIR = jsky/image/fits/gui

# Path to top level dir
TOP = ../../../../..

# include master makefile
include $(TOP)/conf/master.mk

# tests
test: all
	(cd ../../gui; $(MAKE) test)
