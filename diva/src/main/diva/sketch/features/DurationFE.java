/*
 * $Id: DurationFE.java,v 1.6 2000/05/10 18:54:53 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;

/**
 * DurationFE computes the amount of time taken to draw a stroke.
 * Returns 0 if there's only one data point.  One of Rubine's
 * features.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 */
public class DurationFE implements FeatureExtractor {

    /**
     * Return the duration of the specified stroke.  Returns 0 if
     * there's only one data point.
     */
    public double apply(TimedStroke s) {
        int num = s.getVertexCount();
        if(num >= 2) {
            double val = (double)(s.getTimestamp(num-1)-s.getTimestamp(0));
            return val;
        }
        else {
            return 0;
        }
    }

    /**
     * Return the name of this feature extractor.
     */
    public String getName() {
        return "Duration";
    }

}

