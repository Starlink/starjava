# ESO Archive
# 
# $Id: Makefile,v 1.4 2002/08/08 22:19:19 gillies Exp $
# 
# who             when        what
# --------------  ----------  ----------------------------------------
# Allan Brighton  1999/12/08  Created

# Source files to compile
SOURCES = \
	AbstractCatalogDirectory.java \
	BasicQueryArgs.java \
	Catalog.java \
	PlotableCatalog.java \
	CatalogDirectory.java \
	CatalogFactory.java \
	FieldDesc.java \
	FieldDescAdapter.java \
	FieldFormat.java \
	HTMLQueryResultHandler.java \
	MemoryCatalog.java \
	QueryArgs.java \
	QueryResult.java \
	QueryResultHandler.java \
	RowCoordinates.java \
	SearchCondition.java \
	TablePlotSymbol.java \
	TableQueryResult.java \
	TestCatalog.java \
	TestTableQueryResult.java \
	URLQueryResult.java \
	ValueRange.java \


# Java package name
PACKAGE_DIR = jsky/catalog

# Path to top level dir
TOP = ../../..

# include master makefile
include $(TOP)/conf/master.mk

# tests
test: 

test1 SearchCondition: all
	java $(JVMFLAGS) jsky.catalog.SearchCondition

test2 MemoryCatalog: all
	java $(JVMFLAGS) jsky.catalog.MemoryCatalog
