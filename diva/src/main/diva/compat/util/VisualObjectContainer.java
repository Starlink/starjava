/*
 * $Id: VisualObjectContainer.java,v 1.1 2002/05/19 22:03:45 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.compat.util;

/**
 * An object which is visually represented by a 
 * "visual object" which is its visible representation within
 * a visualization or editing application.
 *
 * @author John Reekie (johnr@eecs.berkeley.edu)
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
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


