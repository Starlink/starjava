/*
 * $Id: SelectionListener.java,v 1.4 2001/07/22 22:00:39 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.canvas.interactor;

/**
 * A model for graph selections which can be listened to.
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.4 $
 */
public interface SelectionListener extends java.util.EventListener {
    /**
     * Called when the selection model has changed.
     */
    public void selectionChanged(SelectionEvent e);
}


