#
# $Id: makefile,v 1.23 2002/05/16 20:37:36 johnr Exp $
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
	JCanvasPanner.java \
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
FigureIcon.class: \
	FigureIcon.java \
	../../canvas/CanvasComponent.java \
	../../canvas/CanvasUtilities.java \
	../../canvas/Figure.java \
	../../canvas/VisibleComponent.java \
	../../util/UserObjectContainer.java

FocusMouseListener.class: \
	FocusMouseListener.java

JCanvasPalette.class: \
	JCanvasPalette.java \
	../../canvas/CanvasPane.java \
	../../canvas/FigureLayer.java \
	../../canvas/JCanvas.java

JCanvasPanner.class: \
	JCanvasPanner.java \
	../../canvas/CanvasComponent.java \
	../../canvas/CanvasLayer.java \
	../../canvas/CanvasPane.java \
	../../canvas/CanvasUtilities.java \
	../../canvas/GraphicsPane.java \
	../../canvas/JCanvas.java \
	../../canvas/TransformContext.java \
	../../canvas/demo/SimplePane.java \
	../../canvas/event/EventAcceptor.java \
	../../util/java2d/ShapeUtilities.java

JContextMenu.class: \
	JContextMenu.java

JPseudoFrame.class: \
	JPseudoFrame.java

JPalette.class: \
	JPalette.java

JPanner.class: \
	JPanner.java \
	../../canvas/CanvasComponent.java \
	../../canvas/CanvasPane.java \
	../../canvas/CanvasUtilities.java \
	../../canvas/JCanvas.java \
	../../canvas/TransformContext.java \
	../../canvas/event/EventAcceptor.java \
	../../util/java2d/ShapeUtilities.java

JShadePane.class: \
	JShadePane.java \
	../ApplicationContext.java \
	../BasicFrame.java

JStatusBar.class: \
	JStatusBar.java

JTreePane.class: \
	JTreePane.java \
	../ApplicationContext.java \
	../BasicFrame.java

ListDataModel.class: \
	ListDataModel.java \
	../../util/IteratorAdapter.java

MenuCreator.class: \
	JContextMenu.java \
	MenuCreator.java \
	MenuFactory.java \
	../../canvas/Figure.java \
	../../canvas/event/LayerEvent.java \
	../../canvas/event/LayerListener.java \
	../../canvas/event/LayerMotionListener.java \
	../../canvas/event/MouseFilter.java \
	../../canvas/interactor/AbstractInteractor.java \
	../../canvas/interactor/Interactor.java

MenuFactory.class: \
	JContextMenu.java \
	MenuFactory.java \
	../../canvas/Figure.java


