/*
 * $Id: MaxSpeedFE.java,v 1.9 2001/07/22 22:01:48 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/**
 * MaxSpeedFE computes the maximum speed (squared) along the path of a
 * stroke.  This is done by taking every two data points in the path
 * and calculates ((delta X)^2 + (delta Y)^2)/(delta time)^2.  At
 * least 2 data points must exist in the path, otherwise 0 is
 * returned.  One of Rubine's features.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.9 $
 * @rating Red
 */
public class MaxSpeedFE implements FeatureExtractor {
    /**
     * Return the maximum speed (squared) of the specified stroke.
     * This is done by taking every two data points in the path and
     * calculates ((delta X)^2 + (delta Y)^2)/(delta time)^2.  At
     * least 2 data points must exist in the path, otherwise 0 is
     * returned.
     */
    public double apply(TimedStroke s) {
        return maxSpeed(s);
    }

    
    /**
     * Return the name of this feature extractor.
     */
    public String getName() {
        return "Maximum Speed";
    }


    /**
     * Return the maximum speed (squared) of the specified stroke.
     * This is done by taking every two data points in the path and
     * calculates ((delta X)^2 + (delta Y)^2)/(delta time)^2.  At
     * least 2 data points must exist in the path, otherwise 0 is
     * returned.
     */
    public static double maxSpeed(TimedStroke s) {
        int num = s.getVertexCount();
        double sum = 0;
        double deltaX, deltaY, val;
        long deltaT;
        if(num >= 2) {
            double maximum = 0;
            for(int i = 0; i <= (num-2); i++) {
                deltaX = s.getX(i+1)-s.getX(i);
                deltaY = s.getY(i+1)-s.getY(i);
                deltaT = s.getTimestamp(i+1)-s.getTimestamp(i);
                if(deltaT != 0){
                    val = (deltaX*deltaX + deltaY*deltaY)/
                        (deltaT*deltaT);
                    maximum = Math.max(maximum, val);
                }
            }
            return maximum;
        }
        else {
            return 0;
        }
    }
}


