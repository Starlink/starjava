# Makefile
#
# Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
# Observatory Control System, Gemini Telescopes Project.
#
# $Id: Makefile,v 1.3 2002/08/08 22:36:54 gillies Exp $

# Source files to compile
SOURCES = \
	IRSACatalog.java \
	IRSAQueryArgs.java \
	IRSAFieldDesc.java \
	IRSACatalogQueryPanel.java \
	IRSACatalogQueryTool.java \
	IRSAConfig.java \
	IRSADataDictXML.java \
	IRSATable.java \
	IRSAXML.java \


# Java package name
PACKAGE_DIR = jsky/catalog/irsa

# Path to top level dir
TOP = ../../../..

# include master makefile
include $(TOP)/conf/master.mk

clean:: resource_clean

# Name of a local catalog file, for testing
LOCAL_CATALOG_NAME = $(TOP)/$(PACKAGE_DIR)/test/oasis.table


# tests
test: test2

test1 IRSAXML: all
	java $(JVMFLAGS) jsky.catalog.irsa.IRSAXML

test2 IRSADataDictXML: all
	java $(JVMFLAGS) jsky.catalog.irsa.IRSADataDictXML

test3 IRSATable: all
	java $(JVMFLAGS) jsky.catalog.irsa.IRSATable $(LOCAL_CATALOG_NAME)

test4 IRSAConfig: all
	java $(JVMFLAGS) jsky.catalog.irsa.IRSAConfig

test5 IRSACatalog: all
	java $(JVMFLAGS) jsky.catalog.irsa.IRSACatalog

