/*
 * $Id: SumOfAbsDeltaRatioFE.java,v 1.6 2000/05/10 18:54:54 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/**
 * SumOfAbsDeltaRatioFE computes the ratio of the sum of the absolute
 * values of the delta y's and the sum of the absolute values of the
 * delta x's (sum of |delta y|)/(sum of |delta x|).
 *
 *  @author Heloise Hse (hwawen@eecs.berkeley.edu)
 *  @version $Revision: 1.6 $
 */
public class SumOfAbsDeltaRatioFE implements FeatureExtractor {

    /**
     * Return the ratio of the sum of the absolute values of the delta
     * y's and the sum of the absolute values of the delta x's.
     * (sum of |delta y|)/(sum of |delta x|).
     */
    public double apply(TimedStroke s) {
        double sumDx = 0;
        double sumDy = 0;

        double x0, y0, x1, y1;
        x0 = s.getX(0);
        y0 = s.getY(0);
        for(int i=1; i < s.getVertexCount(); i++) {
            x1 = s.getX(i);
            y1 = s.getY(i);
            sumDx += Math.abs(x1-x0);
            sumDy += Math.abs(y1-y0);
            x0 = x1;
            y0 = y1;
        }
        return (sumDy/sumDx);
    }

    /**
     * Return the name of this feature extractor.
     */    
    public String getName() {
        return "Sum of Absolute Delta Ratio";
    }

}

