/*
 * $Id: UserObjectChangeListener.java,v 1.5 2000/05/02 00:45:26 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;

/**
 * A listener for changes in the user object of
 * a UserObjectContainer.
 * 
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 */
public interface UserObjectChangeListener extends java.util.EventListener {
    /**
     * The user object has changed.
     */
    public void objectChange(UserObjectChangeEvent e);
}

