/*
 * $Id: SemanticObjectContainer.java,v 1.2 2000/05/02 00:45:25 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;

/**
 * An object which is annotated with a single
 * "semantic object" which is its semantic equivalent
 * in an application.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 */
public interface SemanticObjectContainer {
    /**
     * Return the semantic object.
     */
    public Object getSemanticObject();

    /**
     * Set the semantic object.
     */
    public void setSemanticObject(Object o);
}

