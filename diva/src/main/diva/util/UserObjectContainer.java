/*
 * $Id: UserObjectContainer.java,v 1.4 2000/05/02 00:45:26 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;

/**
 * An object which is annotated with a single
 * "user object" which is its semantic equivalent
 * in an application.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public interface UserObjectContainer {
    /**
     * Return the user object.
     */
    public Object getUserObject();

    /**
     * Set the user object.
     */
    public void setUserObject(Object o);
}

