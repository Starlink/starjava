#
# $Id: makefile,v 1.23 2000/06/05 22:40:29 neuendor Exp $
#
# Makefile for diva.canvas.tutorial
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	ArcTutorial.java \
	CompositeFigureTutorial.java \
	ConnectorTutorial.java \
	DragTutorial.java \
	FigureTutorial.java \
	LabelTutorial.java \
	SimpleTutorial.java \
	TerminalTutorial.java \
	IconTutorial.java \
	TransformedFigureTutorial.java

# Miscellaneous files that really should be here
MISC_FILES = demo.gif

# Include the shared compilation rules
include $(DIVA)/compile.mk

# Rules for running the examples. These are just there to save messing
# about with classpath
ArcTutorial: ArcTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.ArcTutorial

CompositeFigureTutorial: CompositeFigureTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.CompositeFigureTutorial

ConnectorTutorial: ConnectorTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.ConnectorTutorial

DragTutorial: DragTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.DragTutorial

#IconTutorial: IconTutorial.class
#	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.IconTutorial

LabelTutorial: LabelTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.LabelTutorial

FigureTutorial: FigureTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.FigureTutorial

SimpleTutorial: SimpleTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.SimpleTutorial

SwingTutorial: SwingTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.SwingTutorial

TerminalTutorial: TerminalTutorial.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.tutorial.TerminalTutorial

TransformedFigureTutorial: TransformedFigureTutorial.class
	$(JAVA) -classpath $(CLASSPATH) \
		diva.canvas.tutorial.TransformedFigureTutorial

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
