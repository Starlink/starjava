/*
 * $Id: SineFirstLastPtsFE.java,v 1.9 2001/07/22 22:01:48 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/**
 * SineFirstLastPtsFE computes the sine of the angle between the first
 * and the last points of a stroke.  One of Rubine's features.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.9 $
 * @rating Red
 */
public class SineFirstLastPtsFE implements FeatureExtractor {
    /**
     * Return the sine of the angle between the first and the last
     * points of the stroke.  Returns -1 if there's only one data
     * point.
     */
    public double apply(TimedStroke s) {
        return sineFirstLast(s);
    }

    
    /**
     * Return the name of this feature extractor.
     */    
    public String getName() {
        return "Sine First Last Points";
    }


    /**
     * Return the sine of the angle between the first and the last
     * points of the stroke.  Returns -1 if there's only one data
     * point.
     */
    public static double sineFirstLast(TimedStroke s) {
        int num = s.getVertexCount();
        if(num >= 2) {
            double f5 = FEUtilities.distance(s.getX(0), s.getY(0),
                    s.getX(num-1), s.getY(num-1));
            if(f5==0) {
                // distance between start and end points
                // is very very close, about 0, use 0.1
                // to avoid divide by 0 error.
                f5 = 0.1;
            }
            return ((double)((s.getY(num-1)-s.getY(0))/f5));
        }
        else {
            //cannot compute
            System.out.println("SineFirstLastPtsFE: not enough data points.");
            return -1;
        }
    }
}


