package uk.ac.starlink.splat.plot;

/**
 *  Interface to mouse motion tracking in Plot.
 *
 *  @author Peter W. Draper
 *  @version $Id$
 *  @since $Date$
 *  @since 12-JUN-2000, original version.
 *  @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 */

interface MouseMotionTracker 
{
    /**
     *  Update readout/display of current coordinates under the mouse
     *  during a motion event.
     *
     *  @param xcoord value of xcoordinate given graphics coordinate
     *  @param ycoord value of ycoordinate given graphics coordinate
     *
     */
    public void updateCoords( String xcoord, String ycoord);
}
