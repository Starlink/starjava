# ESO Archive
# 
# $Id: Makefile,v 1.10 2002/08/08 22:40:23 gillies Exp $
# 
# who             when        what
# --------------  ----------  ----------------------------------------
# Allan Brighton  1999/12/08  Created

# Source files to compile
SOURCES = \
	BasicTablePlotter.java \
	CatalogHistoryItem.java \
	CatalogNavigator.java \
	CatalogNavigatorMenuBar.java \
	CatalogNavigatorOpener.java \
	CatalogQueryPanel.java \
	CatalogQueryTool.java \
	CatalogTree.java \
	CatalogTreeCellRenderer.java \
	CatalogUIHandler.java \
	HyperlinkTableCellEditor.java \
	HyperlinkTableCellRenderer.java \
	QueryResultDisplay.java \
	SymbolSelectionEvent.java \
	SymbolSelectionListener.java \
	TableColumnConfigPanel.java \
	TableDisplay.java \
	TableDisplayTool.java \
	TablePlotter.java \
	TableSelectionEvent.java \
	TableSelectionListener.java \
	TableSymbolConfig.java \
	TableSymbolConfigGUI.java \


# Java package name
PACKAGE_DIR = jsky/catalog/gui

# Path to top level dir
TOP = ../../../..

# Resources to be installed in the package directory
PACKAGE_RESOURCES = \
	i18n/jb.properties \
	i18n/gui.properties \
	i18n/gui_gb.properties

# include master makefile
include $(TOP)/conf/master.mk

# tests: 
#
# Note: See one of the implementing packages, such as catalog/skycat,
# for test cases (You need a concrete catalog implementation to be able to test).
test: test1

test1 TableDisplay: all
	java $(JVMFLAGS) jsky.catalog.gui.TableDisplay

test2 CatalogTree: all
	java $(JVMFLAGS) jsky.catalog.gui.CatalogTree

