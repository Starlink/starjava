/*
 * $Id: LayerMotionListener.java,v 1.5 2001/07/22 22:00:35 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.event;

/** The interface for listeners that respond to motion of the mouse over a
 * figure. 
 *
 * @version	$Revision: 1.5 $
 * @author 	John Reekie
 */
public interface LayerMotionListener extends java.util.EventListener {

    /** Invoked when the mouse enters a layer or figure.
     */
    public void mouseEntered (LayerEvent e);

    /** Invoked when the mouse exits a layer or figure.
     */
    public void mouseExited (LayerEvent e);
 
    /** Invoked when the mouse moves while over a layer or figure.
     */
    public void mouseMoved (LayerEvent e);
}



