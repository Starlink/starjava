#
# $Id: makefile,v 1.20 2000/11/13 08:09:13 hwawen Exp $
#
# Makefile for diva.whiteboard
#
DIVA  = ..
include $(DIVA)/diva.mk

SUBPACKAGES = \
	uml

# Java files in this directory
JSRCS = \
	CommandInterpreter.java \
	SketchDocument.java \
	SketchStoragePolicy.java \
	SketchTest1.java \
	SketchTest2.java \
	SketchTest3.java \
	Whiteboard.java \
	WhiteboardEdits.java \
	WhiteboardState.java \
	WhiteboardView.java \
	ThumbnailView.java 

# Include the shared compilation rules
include $(DIVA)/compile.mk

Whiteboard: Whiteboard.class
	$(JAVA) -classpath $(CLASSPATH) diva.whiteboard.Whiteboard 

SketchTest1: SketchTest1.class
	$(JAVA) -classpath $(CLASSPATH) diva.whiteboard.SketchTest1

SketchTest2: SketchTest2.class
	$(JAVA) -classpath $(CLASSPATH) diva.whiteboard.SketchTest2

SketchTest3: SketchTest3.class
	$(JAVA) -classpath $(CLASSPATH) diva.whiteboard.SketchTest3

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
