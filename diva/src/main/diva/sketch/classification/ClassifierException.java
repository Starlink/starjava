/*
 * $Id: ClassifierException.java,v 1.4 2001/07/22 22:01:44 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.classification;

/**
 * Thrown when there is some internal error in the training
 * or classification process.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
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


