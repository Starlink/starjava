/*
 * $Id: AspectRatioFE.java,v 1.9 2001/07/22 22:01:46 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;
import java.awt.geom.Rectangle2D;

/**
 * AspectRatioFE calculates the ratio of the width and the height from
 * a stroke's bounding box, (width/height).
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.9 $
 * @rating Red
 */
public class AspectRatioFE implements FeatureExtractor {
    /**
     * Return the ratio of the width and height of the stroke's
     * bounding box.
     */
    public double apply(TimedStroke s) {
        return aspectRatio(s);
    }

    /**
     * Return the ratio of the width and height of the stroke's
     * bounding box.
     */
    public static double aspectRatio(TimedStroke s) {
        Rectangle2D box = StrokeBBox.apply(s);
        return ((double)box.getWidth()/(double)box.getHeight());
    }

    /**
     * Return the name of the feature extractor.
     */    
    public String getName() {
        return "Aspect Ratio";
    }

}


