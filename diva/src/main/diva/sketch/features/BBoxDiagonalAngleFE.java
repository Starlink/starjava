/*
 * $Id: BBoxDiagonalAngleFE.java,v 1.8 2001/07/22 22:01:46 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;
import java.awt.geom.Rectangle2D;

/**
 * BBoxDiagonalAngleFE computes the angle between the diagonal and the
 * base of a stroke's bounding box.  One of Rubine's features.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.8 $
 * @rating Red
 */
public class BBoxDiagonalAngleFE implements FeatureExtractor {
    /**
     * Return the angle between the diagonal and the base of the
     * stroke's bounding box.
     */
    public double apply(TimedStroke s) {
        return bboxDiagonalAngle(s);
    }

    /**
     * Return the angle between the diagonal and the base of the
     * stroke's bounding box.
     */
    public static double bboxDiagonalAngle(TimedStroke s) {
        Rectangle2D r = StrokeBBox.apply(s);
        return (Math.atan(((double)r.getHeight()/(double)r.getWidth())));
    }

    /**
     * Return the name of the feature extractor.
     */
    public String getName() {
        return "BBox Diagonal Angle";
    }
}


