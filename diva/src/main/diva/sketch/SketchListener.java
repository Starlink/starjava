/*
 * $Id: SketchListener.java,v 1.4 2001/07/22 22:01:41 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;

/**
 * A listener for modifications to a sketch model,
 * which is updated by SketchEvent when symbols
 * in the model are added, deleted or modified.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public interface SketchListener {
    /**
     * Invoked when a symbol has been added to a sketch model.
     */
    public void symbolAdded(SketchEvent e);

    /**
     * Invoked when a symbol has been removed from a sketch model.
     */
    public void symbolRemoved(SketchEvent e);

    /**
     * Invoked when a symbol has been modified.
     */
    public void symbolModified(SketchEvent e);
}


