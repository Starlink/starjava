/*
 * $Id: SineInitAngleFE.java,v 1.9 2001/07/22 22:01:48 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/**
 * SineInitAngleFE computes the sine of the initial angle of a stroke.
 * The angle is determined by the first and the third mouse points.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.9 $
 * @rating Red
 */
public class SineInitAngleFE implements FeatureExtractor {
    /**
     * Return the sine of the initial angle of the stroke.  The angle
     * is determined by the first and the third mouse points.  Return
     * -1 if there's only one data point.
     */
    public double apply(TimedStroke s) {
        return sineInitAngle(s);
    }


    /**
     * Return the name of this feature extractor.
     */    
    public String getName() {
        return "Sine of Initial Angle";
    }


    /**
     * Return the sine of the initial angle of the stroke.  The angle
     * is determined by the first and the third mouse points.  Return
     * -1 if there's only one data point.
     */
    public static double sineInitAngle(TimedStroke s) {
        if(s.getVertexCount() >= 3) {
            double denom = FEUtilities.distance(s.getX(0), s.getY(0),
                    s.getX(2), s.getY(2));
            if(denom==0){
                // distance between start and end
                // points is very very close, about 0,
                // use 0.1 to avoid divide by 0 error.
                denom = 0.1;
            }
            return (double)(s.getY(2)-s.getY(0))/denom;
        }
        else if(s.getVertexCount() == 2) {
            double denom = FEUtilities.distance(s.getX(0), s.getY(0),
                    s.getX(1), s.getY(1));
            if(denom==0){
                // distance between start and end points
                // is very very close, about 0, use 0.1
                // to avoid divide by 0 error.
                denom = 0.1;
            }
            return (double)(s.getY(1)-s.getY(0))/denom;
        }
        else {
            //cannot compute
            System.out.println("SineInitAngleFE: not enough data points.");
            return -1;
        }
    }
}


