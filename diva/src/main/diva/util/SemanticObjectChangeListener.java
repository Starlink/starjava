/*
 * $Id: SemanticObjectChangeListener.java,v 1.2 2000/05/02 00:45:25 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util;

/**
 * A listener for changes in the semantic object of
 * a SemanticObjectContainer.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 */
public interface SemanticObjectChangeListener extends java.util.EventListener {
    /**
     * The semantic object has changed.
     */
    public void objectChange(SemanticObjectChangeEvent e);
}

