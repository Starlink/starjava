/*
 * $Id: SumOfAbsoluteAnglesFE.java,v 1.9 2001/07/22 22:01:48 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/**
 * SumOfAbsoluteAnglesFE computes the sum of the absolute values of
 * the angles along a stroke path.  This is done by calculating the
 * angles formed by every three consecutive data points in the path,
 * taking the absolute values and summing them up.  One of Rubine's
 * features.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.9 $
 * @rating Red
 */
public class SumOfAbsoluteAnglesFE implements FeatureExtractor {
    /**
     * Computes the sum of the absolute values of the angles along a
     * stroke path.  This is done by calculating the angles formed
     * by every three consecutive data points in the path, taking
     * the absolute values and summing them up.  Return -1 if there are
     * less than 3 points.
     */
    public double apply(TimedStroke s) {
        return sumOfAbsoluteAngles(s);
    }

    
    /**
     * Return the name of this feature extractor.
     */    
    public String getName() {
        return "Sum of Absolute Angles";
    }


    /**
     * Computes the sum of the absolute values of the angles along a
     * stroke path.  This is done by calculating the angles formed
     * by every three consecutive data points in the path, taking
     * the absolute values and summing them up.  Return -1 if there are
     * less than 3 points.
     */
    public static double sumOfAbsoluteAngles(TimedStroke s) {
        int num = s.getVertexCount();
        double sum = 0;
        double deltaX, deltaXp;
        double deltaY, deltaYp;
        double theta;
        if(num > 2) {
            for(int i = 1; i< num-1; i++) {
                deltaX = s.getX(i+1)-s.getX(i);
                deltaY = s.getY(i+1)-s.getY(i);
                deltaXp = s.getX(i)-s.getX(i-1);
                deltaYp = s.getY(i)-s.getY(i-1);
                double tmp = (double)((deltaX*deltaYp)-(deltaXp*deltaY)) /
                    (double)((deltaX*deltaXp)+(deltaY*deltaYp));
                theta = Math.atan(tmp);
                if(!Double.isNaN(theta)){
                    sum += Math.abs(theta);
                }
                else {
                    String err = "SumOfAbsoluteAnglesFE: invalid theta.";
                    System.out.println(err);
                }
            }
            return sum;
        }
        else {
            // cannot compute
            return -1;
        }
    }
}


