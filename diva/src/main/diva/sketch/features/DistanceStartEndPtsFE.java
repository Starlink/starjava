/*
 * $Id: DistanceStartEndPtsFE.java,v 1.8 2001/07/22 22:01:47 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/**
 * DistanceStartEndPtsFE computes the distance between the first and
 * the last point of a stroke.  Returns 0 if there's only one data
 * point.  One of Rubine's features.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.8 $
 * @rating Red
 */
public class DistanceStartEndPtsFE implements FeatureExtractor {
    /**
     * Return the distance between the first and the last point of the
     * stroke.  Returns 0 if there's only one data point.
     */
    public double apply(TimedStroke s) {
        return distanceStartEnd(s);
    }

    /**
     * Return the distance between the first and the last point of the
     * stroke.  Returns 0 if there's only one data point.
     */
    public static double distanceStartEnd(TimedStroke s) {
        int num = s.getVertexCount();
        if(num >= 2) {
            return FEUtilities.distance(s.getX(0), s.getY(0),
                    s.getX(num-1), s.getY(num-1));
        }
        else {
            return 0;
        }
    }

    /**
     * Return the name of this feature extractor.
     */
    public String getName() {
        return "Distance between First and Last Points";
    }

}


