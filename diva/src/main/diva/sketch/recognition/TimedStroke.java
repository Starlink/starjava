/*
 * $Id: TimedStroke.java,v 1.5 2002/02/06 03:27:45 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.recognition;
import diva.util.PropertyContainer;
import diva.util.BasicPropertyContainer;
import diva.util.java2d.Polyline2D;

import java.util.Iterator;

/**
 * TimedStroke is a collection of points taken in the duration of a
 * mouse pressed event and a mouse released event.  A TimedStroke
 * object contains basic stroke path information (such as the points
 * and the timestamps in the path) and can be annotated to contain
 * application-specific information, e.g.  properties used in the
 * recognition process.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 */
public class TimedStroke extends Polyline2D.Float implements PropertyContainer {
    /**
     *  An array storing the timestamps of the points
     *  in the stroke path.
     */
    private long _timestamps[];

    /**
     *  An array storing the timestamps of the points
     *  in the stroke path.
     */
    BasicPropertyContainer _properties = new BasicPropertyContainer();

    /**
     *  Construct a timed stroke object with an empty stroke path.
     */
    public TimedStroke() {
        this(4);
    }


    /** Copy constructor for efficient copying of TimedStroke
     */
    public TimedStroke(TimedStroke in) {
        super(in);
        int cnt = in.getVertexCount();
        _timestamps = new long[cnt];
        System.arraycopy(in._timestamps, 0, _timestamps, 0, cnt);
    }
    
    /**
     * Construct a timed stroke object with an empty stroke path
     * of the given initial size.
     */
    public TimedStroke(int initSize) {
        super(initSize);
        _timestamps = new long[initSize];
    }
	
	
    /**
     *  Add a pair of x, y coordinates in the stroke path and the
     *  corresponding timestamp.
     */
    public void addVertex(float x, float y, long timestamp) {
        lineTo(x,y);

        int cnt = getVertexCount();
        if(cnt >= _timestamps.length) {
            long[] temp = new long[cnt*2];
            System.arraycopy(_timestamps, 0, temp, 0, cnt);
            _timestamps = temp;
        }
        _timestamps[cnt-1] = timestamp;
    }

    /**
     * Return the property corresponding to the given key, or null if
     * no such property exists.
     */
    public Object getProperty(String key) {
        return _properties.getProperty(key);
    }

    /**
     *  Return the timestamp of the point at the given index.
     */
    public long getTimestamp(int i) {
        return _timestamps[i];
    }

    /** Return an iteration of the names of the properties
     */
    public Iterator propertyNames(){
        return _properties.propertyNames();
    }

    /**
     * Set the property corresponding to the given key.
     */
    public void setProperty(String key, Object value) {
        _properties.setProperty(key, value);
    }
}


