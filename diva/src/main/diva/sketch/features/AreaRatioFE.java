/*
 * $Id: AreaRatioFE.java,v 1.6 2000/05/10 18:54:52 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;
import java.awt.geom.Rectangle2D;

/**
 * AreaRatioFE calculates the ratio of the convex hull area and the
 * bounding box area of a stroke, (convex_hull/bbox).
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 */
public class AreaRatioFE implements FeatureExtractor {
    /**
     * Stroke hull computes the convex hull of a stroke using
     * quick hull algorithm.
     */
    private StrokeHull _strokeHull = new StrokeHull();

    /**
     * StrokeBbox computes the bounding box of a stroke.
     */
    private StrokeBBox _strokeBbox = new StrokeBBox();

    /**
     * Return the ratio of the convex hull area and the
     * bounding box area of the stroke.
     */
    public double apply(TimedStroke s) {
        ConvexHull hull = _strokeHull.apply(s);
        double hullArea = hull.getArea();
        Rectangle2D box = _strokeBbox.apply(s);
        double boundArea = box.getWidth()*box.getHeight();
        double arearatio = hullArea/boundArea;
        return arearatio;
    }

    /**
     * Return the name of the feature extractor.
     */
    public String getName() {
        return "Area Ratio";
    }

}

