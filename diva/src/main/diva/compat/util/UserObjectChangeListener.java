/*
 * $Id: UserObjectChangeListener.java,v 1.1 2002/05/19 22:03:44 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.compat.util;

/**
 * A listener for changes in the user object of
 * a UserObjectContainer.
 * 
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 */
public interface UserObjectChangeListener extends java.util.EventListener {
    /**
     * The user object has changed.
     */
    public void objectChange(UserObjectChangeEvent e);
}


