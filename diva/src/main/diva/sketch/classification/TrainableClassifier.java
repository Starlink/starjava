/*
 * $Id: TrainableClassifier.java,v 1.4 2001/07/22 22:01:44 johnr Exp $
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
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public interface TrainableClassifier extends Classifier {
    /**
     * Train the classifier with a given training set.  This method
     * will throw a ClassifierException if the training set is not
     * self consisistent, i.e. the feature sets that it contains do
     * not have the same number of features in them.
     */
    public void train(TrainingSet s) throws ClassifierException;

    /**
     * Return whether this classifier is incremental, i.e. whether
     * this classifier can support multiple calls to "train".
     */
    public boolean isIncremental();

    /**
     * Clear all results of previous trainings (presumably so that
     * this classifier can be trained again from scratch).
     */
    public void clear();
}


