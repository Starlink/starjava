/*
 * $Id: CosFirstLastPtsFE.java,v 1.6 2000/05/10 18:54:53 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/*
 * CosFirstLastPtsFE computes the cosine of the angle between the
 * first and the last point of a stroke.  Return -1 if there's only one
 * data point.  One of Rubine's features.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 */
public class CosFirstLastPtsFE implements FeatureExtractor {   

    /**
     * Return the cosine of the angle between the first and last point
     * of the stroke.  Return -1 if there's only one data point.
     */
    public double apply(TimedStroke s) {
        int num = s.getVertexCount();
        if(num >= 2) {
            double f5 = FEUtilities.distance(s.getX(0), s.getY(0), s.getX(num-1), s.getY(num-1));
            if(f5 == 0) {
                // distance between start and end points
                // is very very close, about 0, use 0.1,
                // otherwise we'll run into divide by 0 error later.
                f5 = 0.1;
            }
            return ((double)((s.getX(num-1)-s.getX(0))/f5));
        }
        else {
            // cannot compute
            System.out.println("CosFirstLastPtsFE: not enough data points.");
            return -1;
        }
    }

    /**
     * Return the name of this feature extractor.
     */
    public String getName() {
        return "Cosine First Last Points";
    }

}

