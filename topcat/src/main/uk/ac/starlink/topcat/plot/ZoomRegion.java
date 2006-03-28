package uk.ac.starlink.topcat.plot;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Defines a region for use with a Zoomer object.
 *
 * @author   Mark Taylor
 * @since    28 Mar 2006
 * @see  Zoomer
 */
public abstract class ZoomRegion {

    private Rectangle target_;
    private Rectangle display_;
    private Cursor cursor_;

    /**
     * Sets the target region.
     * This is the region within which the mouse must be 
     * clicked and dragged in order to generate a zoom event.
     *
     * @param  target  target region
     */
    public void setTarget( Rectangle target ) {
        target_ = target;
    }

    /**
     * Returns the target region.
     * This is the region within which the mouse must be 
     * clicked and dragged in order to generate a zoom event.
     *
     * @return  target region
     */
    public Rectangle getTarget() {
        return target_;
    }

    /**
     * Sets the display region.
     * This is the region used to provide visual feedback to the user 
     * during a drag gesture.
     *
     * @param  display  display region
     */
    public void setDisplay( Rectangle display ) {
        display_ = display;
    }

    /**
     * Returns the display region.
     * This is the region used to display visual feedback to the user
     * during a drag gesture.
     *
     * @return  display region
     */
    public Rectangle getDisplay() {
        return display_;
    }

    /** 
     * Sets a custom cursor for use in the target region.
     *
     * @param  custom cursor
     */
    public void setCursor( Cursor cursor ) {
        cursor_ = cursor;
    }

    /**
     * Returns the custom cursor for use in the target region.
     *
     * @return  custom cursor
     */
    public Cursor getCursor() {
        return cursor_;
    }

    /**
     * Returns a new ZoomDrag object appropriate for this region.
     *
     * @param  comp  component on which the drag is taking place
     * @param  start  start point for the drag
     * @return   new drag object
     */
    public abstract ZoomDrag createDrag( Component comp, Point start );

    /**
     * Callback which will be invoked when a zoom invoked on this region
     * has been completed successfully.  The parameter is a two-element
     * array giving X (lower, upper) bounds followed by Y (lower, upper) 
     * bounds.  Either of the X or Y bound arrays may be null to indicate
     * no zooming in that direction. 
     * The units are dimensionless: a range of (0,1) indicates the same
     * range as is currently contained by the display region.
     * Bounds may be larger or smaller than the (1,0) interval.
     *
     * @param   bounds   2x2 array of zoom bounds { {xlo, xhi}, {ylo, yhi} }
     */
    public abstract void zoomed( double[][] bounds );
}
