/*
 * $Id: FocusMouseListener.java,v 1.1 2000/07/02 19:56:57 neuendor Exp $
 *
 * Copyright (c) 2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.gui.toolbox;

import java.awt.Component;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

/** 
 * A mouse listener that requests focus for the source of any
 * mouse event it receives.  Yes, I Know this is simple, but I think it 
 * is useful.  Attach it to a component and the component gets when pressed.
 *
 * @author Steve Neuendorffer (neuendor@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 */
public class FocusMouseListener implements MouseListener {      
    public void mouseReleased(MouseEvent event) {
    }
    
    public void mouseEntered(MouseEvent event) {
    }
    
    public void mouseExited(MouseEvent event) {
    }
    
    /**
     * Grab the keyboard focus when the component that this listener is
     * attached to is clicked on.
     */
    public void mousePressed(MouseEvent event) {
	Component component = event.getComponent();
	if (!component.hasFocus()) {
	    component.requestFocus();
	}
    }
    
    public void mouseClicked(MouseEvent event) {
    }
}

