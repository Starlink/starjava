/*
 * $Id: BBoxDiagonalAngleFE.java,v 1.6 2000/05/10 18:54:52 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
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
 * @version $Revision: 1.6 $
 */
public class BBoxDiagonalAngleFE implements FeatureExtractor {
    /**
     * StrokeBbox computes the bounding box of a stroke.
     */
    private StrokeBBox _strokeBBox = new StrokeBBox();

    /**
     * Return the angle between the diagonal and the base of the
     * stroke's bounding box.
     */
    public double apply(TimedStroke s) {
        Rectangle2D r = _strokeBBox.apply(s);
        return (Math.atan(((double)r.getHeight()/(double)r.getWidth())));
    }

    /**
     * Return the name of the feature extractor.
     */
    public String getName() {
        return "BBox Diagonal Angle";
    }
}

