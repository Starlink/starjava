/*
 * $Id: VisualObjectContainer.java,v 1.2 2000/05/02 00:45:26 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;

/**
 * An object which is visually represented by a 
 * "visual object" which is its visible representation within
 * a visualization or editing application.
 *
 * @author John Reekie (johnr@eecs.berkeley.edu)
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 */
public interface VisualObjectContainer {
    /**
     * Return the visual object.
     */
    public Object getVisualObject();

    /**
     * Set the visual object.
     */
    public void setVisualObject(Object o);
}

