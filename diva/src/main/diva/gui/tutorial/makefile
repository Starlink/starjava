#
# $Id: makefile,v 1.4 2002/05/16 20:37:36 johnr Exp $
#
# Makefile for diva.gui.tutorial
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	AppletTutorial.java \
	ApplicationTutorial.java \
	MDIApplicationTutorial.java \
	TextDocument.java \
	TextView.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

# The tutorials
AppletTutorial:: classes
	appletviewer AppletTutorial.htm

ApplicationTutorial:: classes
	$(JAVA) -classpath $(CLASSPATH) diva.gui.tutorial.ApplicationTutorial

MDIApplicationTutorial:: classes
	$(JAVA) -classpath $(CLASSPATH) diva.gui.tutorial.MDIApplicationTutorial

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
AppletTutorial.class: \
	AppletTutorial.java \
	ApplicationTutorial.java \
	../AbstractApplication.java \
	../AppContext.java \
	../AppletContext.java

ApplicationTutorial.class: \
	ApplicationTutorial.java \
	TextDocument.java \
	TextView.java \
	../AbstractApplication.java \
	../AbstractDocument.java \
	../AbstractStoragePolicy.java \
	../AbstractView.java \
	../AppContext.java \
	../Application.java \
	../ApplicationContext.java \
	../DefaultActions.java \
	../DefaultStoragePolicy.java \
	../Document.java \
	../DocumentFactory.java \
	../GUIUtilities.java \
	../StoragePolicy.java \
	../View.java \
	../toolbox/ListDataModel.java \
	../../resource/DefaultBundle.java \
	../../resource/RelativeBundle.java

MDIApplicationTutorial.class: \
	MDIApplicationTutorial.java \
	TextDocument.java \
	TextView.java \
	../AbstractApplication.java \
	../AbstractDocument.java \
	../AbstractStoragePolicy.java \
	../AbstractView.java \
	../AppContext.java \
	../AppContextDelegate.java \
	../Application.java \
	../ApplicationContext.java \
	../DefaultActions.java \
	../DefaultStoragePolicy.java \
	../DesktopContext.java \
	../Document.java \
	../DocumentFactory.java \
	../GUIUtilities.java \
	../MDIApplication.java \
	../MDIContext.java \
	../StoragePolicy.java \
	../View.java \
	../toolbox/FocusMouseListener.java \
	../toolbox/ListDataModel.java \
	../../resource/DefaultBundle.java \
	../../resource/RelativeBundle.java

TextDocument.class: \
	TextDocument.java \
	../AbstractDocument.java \
	../Application.java \
	../Document.java \
	../DocumentFactory.java

TextView.class: \
	TextDocument.java \
	TextView.java \
	../AbstractDocument.java \
	../AbstractView.java \
	../Document.java \
	../View.java \
	../toolbox/FocusMouseListener.java \
	../toolbox/ListDataModel.java \
	../../resource/DefaultBundle.java \
	../../resource/RelativeBundle.java


