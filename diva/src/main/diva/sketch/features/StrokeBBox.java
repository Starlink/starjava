/*
 * $Id: StrokeBBox.java,v 1.8 2001/07/22 22:01:48 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;

/**
 * StrokeBBox computes the bounding box of a stroke and stores
 * the result in the stroke's property table.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.8 $
 */
public class StrokeBBox {
    /**
     * The key to a property table, to store or access the cached
     * bounding box of a stroke.
     */
    public static String PROPERTY_KEY = "StrokeBBox";

    /**
     * Compute the stroke's bounding box using the minimum and maximum
     * x, y values in the path.  If the specified stroke already has a
     * bounding box in its property table, just return the cached box.
     * This assumes that the stroke does not change.  Otherwise,
     * calculate the bounding box and stores it in the stroke's property
     * table so that it won't need to be recalculated.
     *
     * <P>
     * FIXME - this caching is extremely dangerous for incremental
     *         recognition.  check number of points in stroke?
     *         have a force recompute function?
     */
    public static Rectangle2D apply(TimedStroke s) {
        Rectangle2D rect = (Rectangle2D)s.getProperty(PROPERTY_KEY);
        if(rect != null){
            return rect;
        }
        rect = bboxNoCache(s);
        s.setProperty(PROPERTY_KEY, rect);
        return rect;
    }
        

    /** Return the bounding box, but do not cache the
     * results in the stroke's property table.
     */
    public static Rectangle2D bboxNoCache(TimedStroke s) {
        int num = s.getVertexCount();
        if(num > 0){
            double xmin = s.getX(0);
            double ymin = s.getY(0);
            double xmax = s.getX(0);
            double ymax = s.getY(0);
            for(int i = 1; i < num; i++) {
                xmin = Math.min(s.getX(i), xmin);
                ymin = Math.min(s.getY(i), ymin);
                xmax = Math.max(s.getX(i), xmax);
                ymax = Math.max(s.getY(i), ymax);
            }
            
            Rectangle2D r = new Rectangle2D.Double();
            r.setFrame(xmin-1, ymin-1, xmax-xmin+2, ymax-ymin+2);
            return r;
        }
        else {
            String err = "No data points in stroke";
            throw new IllegalArgumentException(err);
        }
    }
}



