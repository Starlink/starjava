# ESO Archive
# 
# $Id: Makefile,v 1.12 2002/08/08 22:19:20 gillies Exp $
# 
# who             when        what
# --------------  ----------  ----------------------------------------
# Allan Brighton  1999/12/08  Created

# Source files to compile
SOURCES = \
	NavigatorQueryTool.java \
	NavigatorPane.java \
	SymbolLayer.java \
	NavigatorCatalogMenu.java \
	NavigatorMenuBar.java \
	NavigatorToolBar.java \
	NavigatorFrame.java \
	NavigatorInternalFrame.java \
	NavigatorImageDisplay.java \
	NavigatorImageDisplayControl.java \
	NavigatorImageDisplayMenuBar.java \
	NavigatorImageDisplayToolBar.java \
	NavigatorImageDisplayFrame.java \
	NavigatorImageDisplayInternalFrame.java \
	NavigatorFITSTable.java \
	Navigator.java \

# Java package name
PACKAGE_DIR = jsky/navigator

# Resources to be installed in the package directory
PACKAGE_RESOURCES = \
	i18n/gui.properties \
	i18n/gui_gb.properties

# Path to top level dir
TOP = ../../..

# include master makefile
include $(TOP)/conf/master.mk

# tests
test: test1

test1 NavigatorFrame: all
	java $(JVMFLAGS) jsky.navigator.NavigatorFrame

test1a: all
	java $(JVMFLAGS) jsky.navigator.NavigatorFrame ${HOME}/.skycat/skycat.cfg

test1b: all
	java $(JVMFLAGS) jsky.navigator.NavigatorFrame http://archive.eso.org/skycat/skycat2.0.cfg

test1c: all
	java $(JVMFLAGS) jsky.navigator.NavigatorFrame http://archive.eso.org/ASTROM/src/xml/META.xml

