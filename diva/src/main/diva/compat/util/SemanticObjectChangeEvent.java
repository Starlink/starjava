/*
 * $Id: SemanticObjectChangeEvent.java,v 1.1 2002/05/19 22:03:44 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.compat.util;

/**
 * An event that can be used to communicate when the semantic object
 * changes in a SemanticObjectContainer.
 *
 * @see SemanticObjectContainer
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 */
public class SemanticObjectChangeEvent extends java.util.EventObject {
    /**
     * @see #getOldValue()
     * @serial
     */
    private Object _oldValue;

    /**
     * Construct a GraphEvent with the given
     * source and previous value.
     */
    public SemanticObjectChangeEvent(Object source, Object oldValue) {
        super(source);
        _oldValue = oldValue;
    }

    /**
     * Return the old value of the semantic object.
     */
    public Object getOldValue() {
        return _oldValue;
    }
}


