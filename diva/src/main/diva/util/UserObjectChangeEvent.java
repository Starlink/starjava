/*
 * $Id: UserObjectChangeEvent.java,v 1.5 2000/05/02 00:45:26 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;

/**
 * An event that can be used to communicate when the user object
 * changes in a UserObjectContainer.
 *
 * @see UserObjectContainer
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 */
public class UserObjectChangeEvent extends java.util.EventObject {
    /**
     * @see #getOldValue()
     * @serial
     */
    private Object _oldValue;

    /**
     * Construct a GraphEvent with the given
     * source and previous value.
     */
    public UserObjectChangeEvent(Object source, Object oldValue) {
        super(source);
        _oldValue = oldValue;
    }

    /**
     * Return the old value of the user object.
     */
    public Object getOldValue() {
        return _oldValue;
    }
}

