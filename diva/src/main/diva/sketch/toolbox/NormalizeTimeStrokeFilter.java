/*
 * $Id: NormalizeTimeStrokeFilter.java,v 1.2 2001/07/22 22:01:58 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.TimedStroke;

/**
 * Interpolate a timed stroke so that its points are
 * spaced evenly over time.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class NormalizeTimeStrokeFilter extends StrokeFilter {
    /** The default timestep, 10ms.
     */
    public static int DEFAULT_TIMESTEP = 10;
    
    /** The timestep to interpolate strokes to.
     */
    private int _timestep;

    /** Interpolate to the given time step (in milliseconds)
     */
    public NormalizeTimeStrokeFilter(int timestep) {
        _timestep = timestep;
    }

    /** Interpolate to the default timestep.
     */
    public NormalizeTimeStrokeFilter() {
        this(DEFAULT_TIMESTEP);
    }
    
    /** Return the timestep that the filter interpolates to in millisecs.
     */
    public int getTimestep() {
        return _timestep;
    }

    /** Set the timestep (milliseconds) that the filter interpolates to.
     */
    public void setTimestep(int timestep) {
        _timestep = timestep;
    }
    
    /**
     * Interpolate a timed stroke so that it contains
     * a specified number of points.
     */
    public TimedStroke apply(TimedStroke s){
        return interpolate(s, _timestep);
    }

    /** Normalize an individual stroke to time increments according
     * to DEFAULT_TIMESTEP.
     */
    public static TimedStroke interpolate(TimedStroke s) {
        return interpolate(s, DEFAULT_TIMESTEP);
    }

    /** Normalize an individual stroke to time increments given
     * by timestep.
     */
    public static TimedStroke interpolate(TimedStroke s, int timestep) {
        long t0 = s.getTimestamp(0);
        long tN = s.getTimestamp(s.getVertexCount()-1);
        int nsteps = (int)((tN-t0)/timestep);
        TimedStroke out = new TimedStroke();
        long tcur = t0;
        int j = 0;
        for(int i = 0; i < nsteps; i++) {
            while(j < s.getVertexCount() && s.getTimestamp(j) <= tcur) {
                j++;
            }
            if(j >= s.getVertexCount()) {
                throw new RuntimeException("not enough points!");
            }
            else {
                long tplus = s.getTimestamp(j);
                long tminus = s.getTimestamp(j-1);

                double xcur = (tplus-tcur)*s.getX(j-1) +
                    (tcur-tminus)*s.getX(j);
                xcur = xcur / (tplus-tminus);
                
                double ycur = (tplus-tcur)*s.getY(j-1) +
                    (tcur-tminus)*s.getY(j);
                ycur = ycur / (tplus-tminus);

                out.addVertex((float)xcur, (float)ycur, tcur);
                tcur += timestep;
            }
        }
        return out;
    }
}

