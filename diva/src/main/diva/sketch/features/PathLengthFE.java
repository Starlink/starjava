/*
 * $Id: PathLengthFE.java,v 1.3 2001/07/22 22:01:48 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/**
 * PathLengthFE computes the path length of a stroke by
 * enumerating over the points in the stroke and summing up the
 * distances between every two consecutive points.  One of Rubine's
 * features.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class PathLengthFE implements FeatureExtractor {
    /**
     * Return the path length of a stroke by enumerating over the
     * points in the stroke and summing up the distances between every
     * two consecutive points.
     */
    public double apply(TimedStroke s) {
        return pathLength(s);
    }


    /**
     * Return the name of this feature extractor.
     */    
    public String getName() {
        return "Stroke Path Length";
    }


    /**
     * Return the path length of a stroke by enumerating over the
     * points in the stroke and summing up the distances between every
     * two consecutive points.
     */
    public static final double pathLength(TimedStroke s) {
        double len = 0;
        int num = s.getVertexCount();
        if(num>1){
            double p1x = s.getX(0);
            double p1y = s.getY(0);
            double p2x, p2y;
            for(int i = 1; i< num; i++){
                p2x = s.getX(i);
                p2y = s.getY(i);
                len += FEUtilities.distance(p1x, p1y, p2x, p2y);
                p1x = p2x;
                p1y = p2y;
            }
        }
        return len;
    }        
}


