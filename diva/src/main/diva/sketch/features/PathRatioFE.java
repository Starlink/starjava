/*
 * $Id: PathRatioFE.java,v 1.10 2001/07/22 22:01:48 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/**
 * PathRatioFE computes the ratio between a stroke's convex hull path
 * length and its actual path length, (convex_hull/pathlen).
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.10 $
 * @rating Red
 */
public class PathRatioFE implements FeatureExtractor {
    /**
     * Return the ratio between the stroke's convex hull path length
     * and the its path length.
     */
    public double apply(TimedStroke s) {
        return pathRatio(s);
    }


    /**
     * Return the name of this feature extractor.
     */
    public String getName() {
        return "Path Ratio";
    }


    /**
     * Return the ratio between the stroke's convex hull path length
     * and the its path length.
     */
    public static double pathRatio(TimedStroke s) {
        ConvexHull hull = StrokeHull.apply(s);
        double hullLen = hull.getPerimeter();
        double rawLen = PathLengthFE.pathLength(s);
        double ratio = rawLen/hullLen;
        return ratio;
    }
}


