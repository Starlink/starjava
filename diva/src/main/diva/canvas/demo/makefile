#
# $Id: makefile,v 1.46 2000/05/22 17:13:59 neuendor Exp $
#
# Makefile for diva.canvas.demo
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	FigureDemo.java \
	MainWindow.java \
	RootPane.java \
	SelectionPane.java \
	SimplePane.java \
	SpaceDemo.java \
	SpaceWindow.java \
	WarpImageFigure.java \
	ZoomDemo.java \

# Miscellaneous files that really should be here
MISC_FILES = demo.gif

# Include the shared compilation rules
include $(DIVA)/compile.mk

# This is just a handy way of getting the classpath
FigureDemo: classes
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.demo.FigureDemo

ZoomDemo: ZoomDemo.class WarpImageFigure.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.demo.ZoomDemo

SpaceDemo: classes
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.demo.SpaceDemo

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
