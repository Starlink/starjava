/*
 * $Id: MenuCreator.java,v 1.6 2001/07/22 22:01:33 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.gui.toolbox;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import diva.canvas.*;
import diva.canvas.interactor.*;
import diva.canvas.event.*;

/**
 * This interactor creates a menu when it is activated.  By default, this 
 * interactor is associated with the right mouse button.  This class is
 * commonly used to create context sensitive menus for figures in a canvas.
 * 
 *
 * @author Stephen Neuendorffer (neuendor@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 */
public class MenuCreator extends AbstractInteractor {
    /** The menu factory.
     */
    MenuFactory _factory;

    /** Return the menu factory.
     */
    public MenuFactory getMenuFactory() {
        return _factory;
    }
    
    /**
     * Construct a new interactor with a right button mouse filter.
     * Set the menu factory to the given factory.
     */
    public MenuCreator(MenuFactory factory) {
	MouseFilter filter = new MouseFilter (
            InputEvent.BUTTON3_MASK);
	setMouseFilter(filter);
	setMenuFactory(factory);
    }
  
    /** 
     * When a mouse press happens, ask the factory to create a menu and show
     * it on the screen.  Consume the mouse event.  If the factory is set to 
     * null, then ignore the event and do not consume it.
     */  
    public void mouseReleased(LayerEvent e) {
	if(_factory != null) {
            Figure source = e.getFigureSource();
            JPopupMenu menu = _factory.create(source);
	    if(menu == null) return;
            menu.show(e.getComponent(), e.getX(), e.getY());
            e.consume();
        }
    }

    /** Set the menu factory.
     */
    public void setMenuFactory(MenuFactory factory) {
        _factory = factory;
    }
}


