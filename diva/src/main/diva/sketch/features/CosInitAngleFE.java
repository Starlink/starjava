/*
 * $Id: CosInitAngleFE.java,v 1.8 2001/07/22 22:01:47 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/**
 * CosInitAngleFE computes the cosine of the initial angle of a
 * stroke.  The angle is determined from the first and third mouse
 * point.  Return -1 if there's only one data point.  One of Rubine's
 * features.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.8 $
 * @rating Red
 */
public class CosInitAngleFE implements FeatureExtractor {
    /**
     * Return the cosine of the initial angle of the stroke.  The
     * angle is determined from the first and third mouse point.
     * Return -1 if there's only one data point.
     */
    public double apply(TimedStroke s) {
        return cosInitAngle(s);
    }

    /**
     * Return the cosine of the initial angle of the stroke.  The
     * angle is determined from the first and third mouse point.
     * Return -1 if there's only one data point.
     */
    public static double cosInitAngle(TimedStroke s) {
        if(s.getVertexCount() >= 3) {
            double denom = FEUtilities.distance(s.getX(0), s.getY(0),
                    s.getX(2), s.getY(2));
            if(denom == 0){
                // distance between start and end points
                // is very very close, about 0, use 0.1,
                // otherwise we'll run into divide by 0
                // error later.
                denom = 0.1;
            }
            double value = (double)(s.getX(2)-s.getX(0))/denom;
            return value;
        }
        else if(s.getVertexCount() == 2) {
            double denom = FEUtilities.distance(s.getX(0), s.getY(0),
                    s.getX(1), s.getY(1));
            if(denom == 0){
                // distance between start and end points
                // is very very close, about 0, use 0.1.
                denom = 0.1;
            }
            double value = (s.getX(1)-s.getX(0))/denom;
            return value;
        }
        else {
            //cannot compute
            System.out.println("CosInitAngleFE: not enough data points.");
            return -1;
        }
    }

    /**
     * Return the name of this feature extractor.
     */
    public String getName() {
        return "Cosine of Initial Angle";
    }
}


