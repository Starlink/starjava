/*
 * $Id: FeatureExtractor.java,v 1.7 2001/07/22 22:01:47 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/**
 * A feature extractor performs computation on a stroke and outputs a
 * value that represents the stroke's feature.  Example of features
 * are the number of corners, aspect ratio, etc.
 *
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.7 $
 * @rating Red
 */
public interface FeatureExtractor {
    /**
     * Return the value of the feature extracted from the stroke.
     */
    public double apply(TimedStroke s);

    /**
     * Return the name of the feature extractor.
     */
    public String getName();
}


