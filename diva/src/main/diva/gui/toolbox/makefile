#
# $Id: makefile,v 1.20 2000/08/16 20:24:34 neuendor Exp $
#
# Makefile for diva.gui.toolbox
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	FigureIcon.java \
	FocusMouseListener.java \
	JCanvasPalette.java \
	JContextMenu.java \
	JPseudoFrame.java \
	JPalette.java \
	JPanner.java \
	JShadePane.java \
	JStatusBar.java \
	JTreePane.java \
	ListDataModel.java \
	MenuCreator.java \
	MenuFactory.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

# Test shade pane
shade:: classes
	$(JAVA) -classpath $(CLASSPATH) diva.gui.toolbox.JShadePane


### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
