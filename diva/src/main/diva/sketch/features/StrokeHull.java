/*
 * $Id: StrokeHull.java,v 1.5 2000/05/10 18:54:54 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.features;
import diva.sketch.recognition.TimedStroke;
import java.util.Iterator;

/**
 * StrokeHull computes the convex hull of a stroke and stores the
 * result in the stroke's property table.
 *
 * @see ConvexHull
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 */
public class StrokeHull {
    /**
     * The key to a stroke's property table, to store or access
     * the cached convex hull of a stroke.
     */
    public static String PROPERTY_KEY = "StrokeHull";

    /**
     * Generate a convex hull object for the specified stroke.  The
     * result is stored in the stroke's property table so that it
     * won't need to be recalculated each time.
     * <p>
     *
     * First check to see if the stroke's property table contains a
     * convex hull.  If so, just return the cached hull.  This assumes
     * that the stroke does not change.
     *
     * FIXME - this caching is extremely dangerous for incremental
     *         recognition.  check number of points in stroke?
     *         have a force recompute function?
     */
    public static ConvexHull apply(TimedStroke s) {
        ConvexHull hull = (ConvexHull)s.getProperty(StrokeHull.PROPERTY_KEY);
        if(hull != null){
            return hull;
        }
        int numPoints = s.getVertexCount();
        double xpath[] = new double[numPoints];
        double ypath[] = new double[numPoints];
        int index = 0;
        for (int i=0; i<numPoints; i++) {
            xpath[index] = s.getX(i);
            ypath[index] = s.getY(i);
            index++;
        }
        
        hull = new ConvexHull();
        hull.quickHull(xpath, ypath);
        s.setProperty(StrokeHull.PROPERTY_KEY, hull);
        return hull;
    }
}

