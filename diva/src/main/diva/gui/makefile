#
# $Id: makefile,v 1.41 2002/05/16 20:37:35 johnr Exp $
#
# Makefile for diva.gui
#

DIVA  = ..
include $(DIVA)/diva.mk

SUBPACKAGES = toolbox tutorial

MISC_FILES = 

# Java files in this directory
JSRCS = \
	AbstractApplication.java \
	AbstractDocument.java \
	AbstractStoragePolicy.java \
	AbstractView.java \
	AppContext.java \
	AppContextDelegate.java \
	AppletContext.java \
	Application.java \
	ApplicationContext.java \
	ApplicationExceptionHandler.java \
	ApplicationResources.java \
	BasicFrame.java \
	BasicPage.java \
	BasicStoragePolicy.java \
	ContextView.java \
	DefaultActions.java \
	DefaultStoragePolicy.java \
	DesktopContext.java \
	Document.java \
	DocumentFactory.java \
	ExtensionFileFilter.java \
	GUIUtilities.java \
	MDIApplication.java \
	MDIContext.java \
	MultipageDocument.java \
	MultipageModel.java \
	MultipageParser.java \
	MultipageWriter.java \
	MultiWindowApplication.java \
	Page.java \
	StoragePolicy.java \
	View.java \
	ViewAdapter.java \
	ViewEvent.java \
	ViewListener.java

# Include the shared compilation rules
include $(DIVA)/compile.mk


### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
AbstractApplication.class: \
	AbstractApplication.java \
	AppContext.java \
	Application.java \
	Document.java \
	DocumentFactory.java \
	GUIUtilities.java \
	StoragePolicy.java \
	View.java \
	toolbox/ListDataModel.java \
	../resource/DefaultBundle.java \
	../resource/RelativeBundle.java

AbstractDocument.class: \
	AbstractDocument.java \
	Application.java \
	Document.java \
	toolbox/ListDataModel.java

AbstractStoragePolicy.class: \
	AbstractStoragePolicy.java \
	Application.java \
	Document.java \
	StoragePolicy.java

AbstractView.class: \
	AbstractView.java \
	Document.java \
	View.java

AppContext.class: \
	AppContext.java

AppContextDelegate.class: \
	AppContext.java \
	AppContextDelegate.java

AppletContext.class: \
	AppContext.java \
	AppletContext.java \
	Application.java

Application.class: \
	AppContext.java \
	Application.java \
	Document.java \
	DocumentFactory.java \
	StoragePolicy.java \
	View.java

ApplicationContext.class: \
	AppContext.java \
	Application.java \
	ApplicationContext.java

ApplicationExceptionHandler.class: \
	Application.java \
	ApplicationExceptionHandler.java

ApplicationResources.class: \
	ApplicationResources.java

BasicFrame.class: \
	AppContext.java \
	ApplicationContext.java \
	BasicFrame.java

BasicPage.class: \
	BasicPage.java \
	MultipageModel.java \
	Page.java

BasicStoragePolicy.class: \
	AbstractStoragePolicy.java \
	AppContext.java \
	Application.java \
	BasicStoragePolicy.java \
	Document.java \
	DocumentFactory.java \
	StoragePolicy.java

ContextView.class: \
	AppContext.java \
	ContextView.java

DefaultActions.class: \
	Application.java \
	DefaultActions.java \
	Document.java \
	DocumentFactory.java \
	StoragePolicy.java \
	View.java

DefaultStoragePolicy.class: \
	AbstractStoragePolicy.java \
	AppContext.java \
	Application.java \
	DefaultStoragePolicy.java \
	Document.java \
	DocumentFactory.java \
	StoragePolicy.java \
	toolbox/JStatusBar.java

DesktopContext.class: \
	AppContext.java \
	AppContextDelegate.java \
	DesktopContext.java \
	MDIContext.java \
	ViewEvent.java \
	ViewListener.java \
	toolbox/JPseudoFrame.java \
	toolbox/JShadePane.java \
	toolbox/JStatusBar.java

Document.class: \
	Application.java \
	Document.java

DocumentFactory.class: \
	Application.java \
	Document.java \
	DocumentFactory.java

ExtensionFileFilter.class: \
	ExtensionFileFilter.java

GUIUtilities.class: \
	GUIUtilities.java

MDIApplication.class: \
	AbstractApplication.java \
	AppContext.java \
	Application.java \
	Document.java \
	MDIApplication.java \
	MDIContext.java \
	View.java \
	ViewAdapter.java \
	ViewEvent.java \
	ViewListener.java

MDIContext.class: \
	AppContext.java \
	MDIContext.java \
	ViewListener.java

MultipageDocument.class: \
	AbstractDocument.java \
	Application.java \
	Document.java \
	MultipageDocument.java \
	MultipageModel.java \
	MultipageParser.java \
	MultipageWriter.java \
	toolbox/ListDataModel.java \
	../util/ModelParser.java \
	../util/ModelWriter.java \
	../util/aelfred/HandlerBase.java

MultipageModel.class: \
	MultipageModel.java \
	Page.java \
	toolbox/ListDataModel.java

MultipageParser.class: \
	BasicPage.java \
	MultipageDocument.java \
	MultipageModel.java \
	MultipageParser.java \
	Page.java \
	../util/ModelParser.java \
	../util/aelfred/HandlerBase.java \
	../util/aelfred/XmlHandler.java \
	../util/aelfred/XmlParser.java

MultipageWriter.class: \
	BasicPage.java \
	MultipageDocument.java \
	MultipageModel.java \
	MultipageParser.java \
	MultipageWriter.java \
	Page.java \
	../util/ModelWriter.java

MultiWindowApplication.class: \
	AbstractApplication.java \
	AppContext.java \
	Application.java \
	ContextView.java \
	Document.java \
	MultiWindowApplication.java \
	View.java \
	toolbox/ListDataModel.java \
	../resource/DefaultBundle.java \
	../resource/RelativeBundle.java

Page.class: \
	Page.java

StoragePolicy.class: \
	Application.java \
	Document.java \
	StoragePolicy.java

View.class: \
	Document.java \
	View.java

ViewAdapter.class: \
	ViewAdapter.java \
	ViewEvent.java \
	ViewListener.java

ViewEvent.class: \
	ViewEvent.java

ViewListener.class: \
	ViewEvent.java \
	ViewListener.java


