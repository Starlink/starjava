#
# $Id: makefile,v 1.30 2002/05/16 20:37:31 johnr Exp $
#
# Makefile for diva.canvas.event
#
DIVA  = ../..
include $(DIVA)/diva.mk

JSRCS = \
	EventAcceptor.java \
	EventLayer.java \
	LayerAdapter.java \
	LayerConsumer.java \
	LayerEvent.java \
	LayerEventMulticaster.java \
	LayerListener.java \
	LayerMouseAdapter.java \
	LayerMotionAdapter.java \
	LayerMotionListener.java \
	MouseFilter.java

# Include the shared compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
EventAcceptor.class: \
	EventAcceptor.java \
	../CanvasComponent.java

EventLayer.class: \
	EventAcceptor.java \
	EventLayer.java \
	LayerEvent.java \
	LayerEventMulticaster.java \
	LayerListener.java \
	LayerMotionListener.java \
	../CanvasComponent.java \
	../CanvasLayer.java \
	../interactor/Interactor.java

LayerAdapter.class: \
	LayerAdapter.java \
	LayerEvent.java \
	LayerListener.java

LayerConsumer.class: \
	LayerConsumer.java \
	LayerEvent.java \
	LayerListener.java

LayerEvent.class: \
	LayerEvent.java \
	../CanvasLayer.java \
	../Figure.java

LayerEventMulticaster.class: \
	LayerEvent.java \
	LayerEventMulticaster.java \
	LayerListener.java \
	LayerMotionListener.java

LayerListener.class: \
	LayerEvent.java \
	LayerListener.java

LayerMouseAdapter.class: \
	LayerEvent.java \
	LayerListener.java \
	LayerMotionListener.java \
	LayerMouseAdapter.java \
	MouseFilter.java

LayerMotionAdapter.class: \
	LayerEvent.java \
	LayerMotionAdapter.java \
	LayerMotionListener.java

LayerMotionListener.class: \
	LayerEvent.java \
	LayerMotionListener.java

MouseFilter.class: \
	LayerEvent.java \
	MouseFilter.java


