/*
 * $Id: AreaRatioFE.java,v 1.8 2001/07/22 22:01:46 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
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
 * @version $Revision: 1.8 $
 */
public class AreaRatioFE implements FeatureExtractor {
    /**
     * Return the ratio of the convex hull area and the
     * bounding box area of the stroke.
     */
    public double apply(TimedStroke s) {
        return areaRatio(s);
    }

    /**
     * Return the ratio of the convex hull area and the
     * bounding box area of the stroke.
     */
    public static double areaRatio(TimedStroke s) {
        ConvexHull hull = StrokeHull.apply(s);
        double hullArea = hull.getArea();
        Rectangle2D box = StrokeBBox.apply(s);
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


