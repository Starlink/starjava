/*
 * $Id: PathRatioFE.java,v 1.6 2000/05/10 18:54:53 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/**
 * PathRatioFE computes the ratio between a stroke's convex hull path
 * length and its actual path length, (convex_hull/pathlen).
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 */
public class PathRatioFE implements FeatureExtractor {
    /**
     * Stroke hull computes the convex hull of a stroke using
     * quick hull algorithm.
     */
    private StrokeHull _strokeHull = new StrokeHull();

    /**
     * StrokePathLengthFE computes the length in a stroke path by
     * taking the sum of distances between every consecutive data
     * points.
     */
    private StrokePathLengthFE _strokePathLengthFE = new StrokePathLengthFE();

    /**
     * Return the ratio between the stroke's convex hull path length
     * and the its path length.
     */
    public double apply(TimedStroke s) {
        ConvexHull hull = _strokeHull.apply(s);
        double hullplen = hull.getPathLength();
        double rawplen = _strokePathLengthFE.apply(s);
        double ratio = rawplen/hullplen;
        //        System.out.println("raw: " + rawplen + ", hull: " +
        //                hullplen + ", path ratio = " + ratio);
        return ratio;
    }

    /**
     * Return the name of this feature extractor.
     */
    public String getName() {
        return "Path Ratio";
    }

}

