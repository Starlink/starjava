/*
 * $Id: BBoxDiagonalLengthFE.java,v 1.6 2000/05/10 18:54:53 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
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
 * @version $Revision: 1.6 $
 */
public class BBoxDiagonalLengthFE implements FeatureExtractor {
    /**
     * StrokeBbox computes the bounding box of a stroke.
     */
    private StrokeBBox _strokeBBox = new StrokeBBox();

    /**
     * Return the length of the diagonal of the stroke's bounding box.
     */
    public double apply(TimedStroke s) {
        Rectangle2D r = _strokeBBox.apply(s);
        return (Math.sqrt(Math.pow(r.getWidth(),2)+Math.pow(r.getHeight(),2)));
    }

    /**
     * Return the name of the feature extractor.
     */
    public String getName() {
        return "BBox Diagonal Length";
    }
}

