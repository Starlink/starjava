/*
 * $Id: Classifier.java,v 1.4 2001/07/22 22:01:43 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.classification;

/**
 * A Classifier performs generic classification on feature sets, the
 * semantics of which it knows nothing about.  It assumes that the
 * feature set it is given is consistent with the feature sets that it
 * was trained on, i.e. the same features are in the same places.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public interface Classifier {
    /**
     * Return a classification for the specified feature set, or throw
     * a runtime exception if the feature set that is given does not
     * have the same number of features as the training set that it
     * was trained on.
     */
    public Classification classify(FeatureSet s) throws ClassifierException;
}


