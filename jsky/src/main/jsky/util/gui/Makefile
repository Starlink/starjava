# ESO Archive
# 
# $Id: Makefile,v 1.30 2002/08/08 22:40:23 gillies Exp $
# 
# who             when        what
# --------------  ----------  ----------------------------------------
# Allan Brighton  1999/12/08  Created

# Source files to compile
SOURCES = \
	ArrayTableCellRenderer.java \
	BasicRangeSliderUI.java \
	BasicWindowMonitor.java \
	BusyWin.java \
	ClipboardHelper.java \
	CoordinatesOffsetPanel.java \
	CoordinatesPanel.java \
	DesktopUtil.java \
	DialogUtil.java \
	DraggableTree.java \
	ExampleFileFilter.java \
	GenericToolBar.java \
	GenericToolBarTarget.java \
	GridBagUtil.java \
	JTextFieldDouble.java \
	JTextFieldDoubleTableCellRenderer.java \
	LabelEntry.java \
	LabelEntrySlider.java \
	LabelJPanel.java \
	LabelValue.java \
	LabelWidget.java \
	LookAndFeelMenu.java \
	MetalRangeSliderUI.java \
	MultiRangeSliderUI.java \
	NumberEntry.java \
	PassbandPanel.java \
	PrintPreview.java \
	PrintableJTable.java \
	ProgressBarFilterInputStream.java \
	ProgressBarUtil.java \
	ProgressException.java \
	ProgressPanel.java \
	ProgressPanelDialog.java \
	ProperMotionPanel.java \
	ProxyServerDialog.java \
	QuantityPanel.java \
	QuantitySlider.java \
	RangeSlider.java \
	RangeSliderUI.java \
	SexagesimalTableCellEditor.java \
	SexagesimalTableCellRenderer.java \
	SortedJTable.java \
	StatusPanel.java \
	SwingUtil.java \
	TabbedPanel.java \
	TabbedPanelFrame.java \
	TabbedPanelInternalFrame.java \
	TableUtil.java \
	ToggleButtonPanel.java \
	VRangeSlider.java \
	Wavelength1DTableModel.java \
	XYDisplay.java \
	ZoomControl.java \



# Java package name
PACKAGE_DIR = jsky/util/gui

# Resources to be installed in the package directory
PACKAGE_RESOURCES = \
	i18n/gui.properties

# Path to top level dir
TOP = ../../../..

# include master makefile
include $(TOP)/conf/master.mk

clean:: resource_clean

# tests
test: 

test1 LabelEntrySlider: all
	java $(JVMFLAGS) jsky.util.gui.LabelEntrySlider

test2 LabelEntry: all
	java $(JVMFLAGS) jsky.util.gui.LabelEntry

test3 LabelValue: all
	java $(JVMFLAGS) jsky.util.gui.LabelValue

test5 ProgressBarUtil: all
	java $(JVMFLAGS) jsky.util.gui.ProgressBarUtil

test6 ToggleButtonPanel: all
	java $(JVMFLAGS) jsky.util.gui.ToggleButtonPanel

test7 LabelJPanel: all
	java $(JVMFLAGS) jsky.util.gui.LabelJPanel

test9 XYDisplay: all
	java $(JVMFLAGS) jsky.util.gui.XYDisplay

test10 ZoomControl: all
	java $(JVMFLAGS) jsky.util.gui.ZoomControl

test11 SortedJTable: all
	java $(JVMFLAGS) jsky.util.gui.SortedJTable

test12 ProgressBarFilterInputStream: all
	java $(JVMFLAGS) jsky.util.gui.ProgressBarFilterInputStream

test13 JTextFieldDouble: all
	java $(JVMFLAGS) jsky.util.gui.JTextFieldDouble

test14 RangeSlider: all
	java $(JVMFLAGS) jsky.util.gui.RangeSlider

test15 VRangeSlider: all
	java $(JVMFLAGS) jsky.util.gui.VRangeSlider

test16 ProgressPanelDialog: all
	java $(JVMFLAGS) jsky.util.gui.ProgressPanelDialog 

test17 TabbedPanelFrame: all
	java $(JVMFLAGS) jsky.util.gui.TabbedPanelFrame 

test18 ProxyServerDialog: all
	java $(JVMFLAGS) jsky.util.gui.ProxyServerDialog

