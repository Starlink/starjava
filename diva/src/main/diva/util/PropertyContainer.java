/*
 * $Id: PropertyContainer.java,v 1.4 2000/05/02 00:45:25 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;

/**
 * An object that can be annotated with arbitrary
 * objects whose keys are strings.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public interface PropertyContainer {
    /**
     * Return the property corresponding to
     * the given key, or null if no such property
     * exists.
     */
    public Object getProperty(String key);

    /**
     * Set the property corresponding to
     * the given key.
     */
    public void setProperty(String key, Object value);

    // XXX removeProperty
}

