/*
 * $Id: StrokeFilter.java,v 1.4 2000/05/10 21:55:47 hwawen Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.toolbox;
import diva.sketch.recognition.TimedStroke;

/**
 * An object which filters a pen stroke in order to reduce the
 * complexity in the raw data points.  Examples of filtering are point
 * reduction, dehooking, etc.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public abstract class StrokeFilter {
    /**
     * The key into the property table to store or to access the
     * cached filtered stroke.
     */
    public static String PROPERTY_KEY = "StrokeFilter";

    /**
     * Apply a filtering algorithm on the specified pen stroke and
     * return the filtered stroke.
     */
    public abstract TimedStroke apply(TimedStroke s);
}


