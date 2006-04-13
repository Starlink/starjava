package uk.ac.starlink.topcat.plot;

import java.awt.Point;

/**
 * Defines a currently active zoom drag gesture.
 *
 * @author   Mark Taylor
 * @since    28 Mar 2006
 * @see  Zoomer
 */
public interface ZoomDrag {

    /**
     * Invoked when the mouse is dragged to a new point <code>p</code>.
     *
     * @param  p  current mouse position
     */
    void dragTo( Point p );

    /** 
     * Returns the bounds defined by this drag at a current position 
     * <code>p</code>.
     * Elements of the result are two-element arrays giving 
     * (lower, upper) bounds in one or more dimensions, according 
     * to the type of drag.
     * The units should normally be dimensionless: a range of (0,1)
     * indicates the same range as is currently contained by the
     * display region.
     * Bounds may be larger or smaller than the (1,0) interval.
     *
     * <p>A null return indicates that no legal zoom is represented.
     *
     * @param   p  current point
     * @return  bounds defined by a drag ending at point <code>p</code>
     */
    double[][] boundsAt( Point p );
}
