# Makefile
#
# Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
# Observatory Control System, Gemini Telescopes Project.
#
# $Id: Makefile,v 1.6 2002/08/08 22:40:23 gillies Exp $

# Source files to compile
SOURCES = \
	HTMLViewer.java \
	HTMLViewerHistoryItem.java \
	HTMLViewerMenuBar.java \
	HTMLViewerFrame.java \
	HTMLViewerInternalFrame.java


# Java package name
PACKAGE_DIR = jsky/html

# Path to top level dir
TOP = ../../..

# include master makefile
include $(TOP)/conf/master.mk

#clean:: resource_clean

# test URL
URL = http://www.eso.org/

# tests
test: test1

test1 HTMLViewer: all
	java $(JVMFLAGS) jsky.html.HTMLViewerFrame $(URL)

