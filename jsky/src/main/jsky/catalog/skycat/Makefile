# Makefile
#
# Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
# Observatory Control System, Gemini Telescopes Project.
#
# $Id: Makefile,v 1.8 2002/08/08 22:40:23 gillies Exp $

# Source files to compile
SOURCES = \
	SkycatConfigEntry.java \
	SkycatTable.java \
	SkycatCatalog.java \
	SkycatConfigFile.java \
	TestCatalog.java \
	SkycatPlotSymbol.java



# Java package name
PACKAGE_DIR = jsky/catalog/skycat

# Path to top level dir
TOP = ../../../..

# include master makefile
include $(TOP)/conf/master.mk

clean:: resource_clean

# Name of a catalog from the skycat config file, for testing
CATALOG_NAME = Guide Star Catalog at ESO

# Name of a local catalog file, for testing
LOCAL_CATALOG_NAME = $(TOP)/$(PACKAGE_DIR)/test/ngc1275.table


# tests
test: test2

test1 SkycatConfigFile: all
	java $(JVMFLAGS) jsky.catalog.skycat.SkycatConfigFile  "$(CATALOG_NAME)"

test2 SkycatCatalog: all
	java $(JVMFLAGS) jsky.catalog.skycat.SkycatCatalog "$(CATALOG_NAME)"

test3 SkycatTable: all
	java $(JVMFLAGS) jsky.catalog.skycat.SkycatTable $(LOCAL_CATALOG_NAME)
