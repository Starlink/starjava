# ESO Archive
# 
# $Id: Makefile,v 1.30 2002/08/08 22:28:45 gillies Exp $
# 
# who             when        what
# --------------  ----------  ----------------------------------------
# Allan Brighton  1999/12/08  Created

# Source files to compile
SOURCES = \
	ConnectionUtil.java \
	Convert.java \
	FileUtil.java \
	Format.java \
	FormatUtilities.java \
	I18N.java \
	IntHashtable.java \
	Interruptable.java \
	JarManager.java \
	JavaExpr.java \
	ListenerHandler.java \
	Logger.java \
	NameValue.java \
	Preferences.java \
	PrintableWithDialog.java \
	ProxyServerUtil.java \
	QuickSort.java \
	ReplaceablePropertyChangeListener.java \
	ReplaceablePropertyVetoException.java \
	ReplacementEvent.java \
	ReplacementVetoException.java \
	ResourceMap.java \
	Resources.java \
	Saveable.java \
	SaveableAsHTML.java \
	SaxParserUtil.java \
	Sortable.java \
	StatusLogger.java \
	StringTokenizerUtil.java \
	StringUtil.java \
	SwingWorker.java \
	TclUtil.java \
	TransferableList.java \
	IApplyCancel.java \


# Java package name
PACKAGE_DIR = jsky/util

# Resources to be installed in the package directory
PACKAGE_RESOURCES = \
	i18n/gui.properties \
	i18n/gui_gb.properties


# Path to top level dir
TOP = ../../..

# include master makefile
include $(TOP)/conf/master.mk

# tests
test: 

test1 StringTokenizerUtil: all
	java $(JVMFLAGS) jsky.util.StringTokenizerUtil

test2 StringUtil: all
	java $(JVMFLAGS) jsky.util.StringUtil

test3 FileUtil: all
	java $(JVMFLAGS) jsky.util.FileUtil

test4 TclUtil: all
	java $(JVMFLAGS) jsky.util.TclUtil

test5 JavaExpr: all
	java $(JVMFLAGS) jsky.util.JavaExpr

test6 Logger: all
	java $(JVMFLAGS) jsky.util.Logger

test6a: all
	java $(JVMFLAGS) jsky.util.Logger logtest.prop

test7 JarManager: all
	java $(JVMFLAGS) jsky.util.JarManager


test8 I18N: all
	java $(JVMFLAGS) jsky.util.I18N

test8a: all
	java $(JVMFLAGS) -Duser.language=de jsky.util.I18N

#test8b: all
#	java $(JVMFLAGS) -Duser.language=fr jsky.util.I18N

