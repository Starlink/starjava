/*
 * $Id: BBoxDiagonalLengthFE.java,v 1.8 2001/07/22 22:01:47 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;
import java.awt.geom.Rectangle2D;

/**
 * BBoxDiagonalLengthFE computes the diagonal length of a stroke's
 * bounding box.  One of Rubine's features.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.8 $
 * @rating Red
 */
public class BBoxDiagonalLengthFE implements FeatureExtractor {
    /**
     * Return the length of the diagonal of the stroke's bounding box.
     */
    public double apply(TimedStroke s) {
        return bboxDiagonalLength(s);
    }
    
    /**
     * Return the length of the diagonal of the stroke's bounding box.
     */
    public static double bboxDiagonalLength(TimedStroke s) {
        Rectangle2D r = StrokeBBox.apply(s);
        return (Math.sqrt(Math.pow(r.getWidth(),2)+Math.pow(r.getHeight(),2)));
    }

    /**
     * Return the name of the feature extractor.
     */
    public String getName() {
        return "BBox Diagonal Length";
    }
}


