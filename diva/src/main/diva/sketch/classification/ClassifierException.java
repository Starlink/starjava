/*
 * $Id: ClassifierException.java,v 1.3 2000/05/02 00:44:47 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.classification;

/**
 * Thrown when there is some internal error in the training
 * or classification process.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 */
public class ClassifierException extends Exception {
    /**
     * Construct a ClassifierException with no
     * error message.
     */
    public ClassifierException() {
        super();
    }
    
    /**
     * Construct a ClassifierException with the specified
     * error message.
     */
    public ClassifierException(String s) {
        super(s);
    }
}

