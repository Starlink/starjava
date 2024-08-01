package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * ZoomRegion for zooming in two dimensions (X-Y).
 * A drag in both positive graphics coordinate directions
 * (down and right) is a zoom in (magnifying),
 * and a drag in both negative directions (up and left) is a zoom out.
 *
 * @author   Mark Taylor
 * @since    28 Mar 2006
 * @see      Zoomer
 */
public abstract class XYZoomRegion extends ZoomRegion {

    private static final int BOX = 32;

    /**
     * Constructs a new XY zoom region.
     *
     * @param  display   rectangle defining the region for both dragging
     *         and visual feedback
     */
    @SuppressWarnings("this-escape")
    public XYZoomRegion( Rectangle display ) {
        setDisplay( display );
        setTarget( display );

        /* I'm in two minds about whether to change the cursor for this 
         * region.  Probably not, since the mouse may be doing other things
         * here (e.g. point selection) and it could be confusing. */
        if ( false ) {
            setCursor( Cursor.getPredefinedCursor( Cursor.SE_RESIZE_CURSOR ) );
        }
    }

    public ZoomDrag createDrag( Component comp, Point start ) {
        return new XyDrag( comp, start );
    }

    /**
     * Implements ZoomDrag for an XY zoom.
     */
    private class XyDrag implements ZoomDrag {

        final Graphics g_;
        final Point start_;
        final Point origin_;
        Boolean sense_;   // true for zoom in, false for zoom out
        Point last_;

        /**
         * Constructor.
         *
         * @param   comp  component on which drag will take place
         * @param   start  start of drag region
         */
        XyDrag( Component comp, Point start ) {
            g_ = comp.getGraphics();
            start_ = start;
            origin_ = comp.getLocation();
            g_.setXORMode( Color.YELLOW );
        }

        public void dragTo( Point point ) {
            Boolean dSense;
            if ( point.x > start_.x && point.y > start_.y ) {
                dSense = Boolean.TRUE;
            }
            else if ( point.x < start_.x && point.y < start_.y ) {
                dSense = Boolean.FALSE;
            }
            else {
                dSense = null;
            }
            if ( ! ( sense_ == null && dSense == null ) ) {
                if ( sense_ == null ) {
                    sense_ = dSense;
                }
                else {
                    assert last_ != null;
                    xorRegion( start_, last_ );
                }
                assert sense_ != null;
                last_ = sense_.equals( dSense ) ? point : start_;
                xorRegion( start_, last_ );
            }
        }

        public double[][] boundsAt( Point end ) {
            if ( sense_ != null ) {
                Rectangle zone = new Rectangle( getDisplay() );
                zone.translate( - origin_.x, -origin_.y );
                xorRegion( start_, last_ );
                if ( sense_.booleanValue() ) {
                    double x0 = ( start_.x - zone.x ) / (double) zone.width;
                    double y0 = ( start_.y - zone.y ) / (double) zone.height;
                    double x1 = ( end.x - zone.x ) / (double) zone.width;
                    double y1 = ( end.y - zone.y ) / (double) zone.height;
                    return x1 > x0 && y1 > y0 
                         ? new double[][] { { x0, x1 }, { y0, y1 } }
                         : null;
                }
                else {
                    int dx = start_.x - end.x;
                    int dy = start_.y - end.y;
                    if ( dx >= 0 && dy > 0 || dx > 0 && dy >= 0 ) {
                        double xrange = ( 2 * dx + BOX ) / (double) BOX;
                        double yrange = ( 2 * dy + BOX ) / (double) BOX;
                        double cx =
                            ( start_.x - zone.x ) / (double) zone.width;
                        double cy =
                            ( start_.y - zone.y ) / (double) zone.height;
                        double x0 = cx - xrange * 0.5;
                        double y0 = cy - yrange * 0.5;
                        double x1 = cx + xrange * 0.5;
                        double y1 = cy + yrange * 0.5;
                        return new double[][] { { x0, x1 }, { y0, y1 } };
                    }
                    else {
                        return null;
                    }
                }
            }
            else {
                return null;
            }
        }

        /**
         * Draws an XORed box on the display region to represent a zoom
         * between the two given points.  Because it uses XOR graphics,
         * drawing the same thing a second time erases it.
         *
         * @param  p0  first point
         * @param  p1  second point
         */
        private void xorRegion( Point p0, Point p1 ) {
            if ( sense_.booleanValue() ) {
                g_.drawRect( Math.min( p0.x, p1.x ), Math.min( p0.y, p1.y ),
                             Math.abs( p1.x - p0.x ), Math.abs( p1.y - p0.y ) );
            }
            else {
                int x0 = p0.x;
                int y0 = p0.y;
                int xrange = Math.abs( p1.x - p0.x );
                int yrange = Math.abs( p1.y - p0.y );
                g_.drawRect( x0 - BOX / 2, y0 - BOX / 2, BOX, BOX );
                g_.drawRect( x0 - BOX / 2 - xrange, y0 - BOX / 2 - yrange,
                             BOX + xrange * 2, BOX + yrange * 2 );
            }
        }
    }
}
