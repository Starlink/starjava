# Makefile
#
# Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
# Observatory Control System, Gemini Telescopes Project.
#
# $Id: Makefile,v 1.3 2002/08/08 22:34:06 gillies Exp $

# Source files to compile
SOURCES = \
	AstroCatConfig.java \
	AstroCatTable.java \
	AstroCatXML.java \
	AstroCatalog.java \


# Java package name
PACKAGE_DIR = jsky/catalog/astrocat

# Path to top level dir
TOP = ../../../..

# include master makefile
include $(TOP)/conf/master.mk

clean:: resource_clean

# Name of a local catalog file, for testing
LOCAL_CATALOG_NAME = $(TOP)/$(PACKAGE_DIR)/test/ngc1275.table


# tests
test: test2

test1 AstroCatXML: all
	java $(JVMFLAGS) jsky.catalog.astrocat.AstroCatXML

test2 AstroCatTable: all
	java $(JVMFLAGS) jsky.catalog.astrocat.AstroCatTable $(LOCAL_CATALOG_NAME)

test3 AstroCatConfig: all
	java $(JVMFLAGS) jsky.catalog.astrocat.AstroCatConfig

test4 AstroCatalog: all
	java $(JVMFLAGS) jsky.catalog.astrocat.AstroCatalog

