/*
 * $Id: SumOfSquaredAnglesFE.java,v 1.9 2001/07/22 22:01:48 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/**
 * SumOfSquaredAnglesFE computes the sum of squared angle values along
 * a stroke path.  This is done by calculating the angles formed by
 * every three consecutive points in the path, squaring and summing
 * them up.  One of Rubine's features.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.9 $
 * @rating Red
 */
public class SumOfSquaredAnglesFE implements FeatureExtractor {
    /**
     * Compute the sum of the squared angle values along the stroke
     * path.  This is done by calculating the angles formed by every
     * three consecutive points in the path, squaring and summing
     * them up.  Return -1 is there are less than 3 points.
     */
    public double apply(TimedStroke s) {
        return sumOfSquaredAngles(s);
    }


    /**
     * Return the name of this feature extractor.
     */    
    public String getName() {
        return "Sum of Squared Angles";
    }


    /**
     * Compute the sum of the squared angle values along the stroke
     * path.  This is done by calculating the angles formed by every
     * three consecutive points in the path, squaring and summing
     * them up.  Return -1 is there are less than 3 points.
     */
    public static double sumOfSquaredAngles(TimedStroke s) {
        int num = s.getVertexCount();

        if(num > 2) {
            double sum = 0;
            double deltaX, deltaXp;
            double deltaY, deltaYp;
            double theta;
            for(int i = 1; i< num-1; i++) {
                deltaX = s.getX(i+1)-s.getX(i);
                deltaY = s.getY(i+1)-s.getY(i);
                deltaXp = s.getX(i)-s.getX(i-1);
                deltaYp = s.getY(i)-s.getY(i-1);
                double tmp = (double)((deltaX*deltaYp)-(deltaXp*deltaY))/(double)((deltaX*deltaXp)+(deltaY*deltaYp));
                theta = Math.atan(tmp);
                if(!Double.isNaN(theta)){
                    sum += Math.pow(theta,2);
                }
                else {
                    System.out.println("SumOfSquaredAnglesFE: invalid theta.");
                }
            }
            return sum;
        }
        else {
            //cannot compute
            //System.out.println("SumOfSquaredAnglesFE: not enough data points.");
            return -1;
        }
    }
}


