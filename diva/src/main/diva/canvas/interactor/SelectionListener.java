/*
 * $Id: SelectionListener.java,v 1.3 2000/05/02 00:43:31 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.canvas.interactor;

/**
 * A model for graph selections which can be listened to.
 *
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.3 $
 */
public interface SelectionListener extends java.util.EventListener {
    /**
     * Called when the selection model has changed.
     */
    public void selectionChanged(SelectionEvent e);
}

