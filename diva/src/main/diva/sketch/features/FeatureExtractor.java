/*
 * $Id: FeatureExtractor.java,v 1.5 2000/05/10 18:54:53 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/**
 *  A feature extractor performs computation on a stroke and outputs a
 *  value that represents the stroke's feature.  Example of features
 *  are the number of corners, aspect ratio, etc.
 *
 *  @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 *  @version $Revision: 1.5 $
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

