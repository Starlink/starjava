/*
 * $Id: AspectRatioFE.java,v 1.6 2000/05/10 18:54:52 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;
import java.awt.geom.Rectangle2D;

/**
 * AspectRatioFE calculates the ratio of the width and the height of a
 * stroke, (width/height).
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 */
public class AspectRatioFE implements FeatureExtractor {
    /**
     * StrokeBbox computes the bounding box of a stroke.
     */
    private StrokeBBox _strokeBbox = new StrokeBBox();   

    /**
     * Return the ratio of the width and height of the stroke.
     */
    public double apply(TimedStroke s) {
        Rectangle2D box = _strokeBbox.apply(s);
        return ((double)box.getWidth()/(double)box.getHeight());
    }

    /**
     * Return the name of the feature extractor.
     */    
    public String getName() {
        return "Aspect Ratio";
    }

}

