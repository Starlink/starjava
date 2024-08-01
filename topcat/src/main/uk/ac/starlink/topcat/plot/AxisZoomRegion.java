package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * ZoomRegion for zooming in one dimension (X or Y).
 * A drag in the positive graphics coordinate direction (left-to-right
 * or up-to-down) is a zoom in (magnifying) and a drag in the
 * opposite direction is a zoom out (er, parvifying?).
 *
 * @author   Mark Taylor
 * @since    28 Mar 2006
 * @see     Zoomer
 */
public abstract class AxisZoomRegion extends ZoomRegion {

    private final boolean isX_;
    private static final int BOX = 32;

    /**
     * Constructs a new axis zoom region with no target and display regions.
     *
     * @param  isX  true for X axis zooming, false for Y axis zooming
     */
    @SuppressWarnings("this-escape")
    public AxisZoomRegion( boolean isX ) {
        isX_ = isX;
        setCursor( Cursor.getPredefinedCursor( isX ? Cursor.W_RESIZE_CURSOR
                                                   : Cursor.N_RESIZE_CURSOR ) );
    }

    /**
     * Constructs a new axis zoom region with defined target and 
     * display regions.
     *
     * @param  isX  true for X axis zooming, false for Y axis zooming
     * @param  target   target region (region in which cursor is dragged)
     * @param  display  display region (region in which zoom box is shown)
     */
    @SuppressWarnings("this-escape")
    public AxisZoomRegion( boolean isX, Rectangle target, Rectangle display ) {
        this( isX );
        setTarget( target );
        setDisplay( display );
    }

    public ZoomDrag createDrag( Component comp, Point start ) {
        return new AxisDrag( comp, start );
    }

    /**
     * Implements ZoomDrag for an Axis zoom.
     */
    private class AxisDrag implements ZoomDrag {
        final Point start_;
        final Point origin_;
        final Graphics g_;
        Point last_;
        Boolean sense_;   // true for zoom in, false for zoom out
        int lastDist_;

        /**
         * Constructor.
         *
         * @param  comp  component on which drag will take place
         * @param  start  start of drag region
         */
        AxisDrag( Component comp, Point start ) {
            g_ = comp.getGraphics();
            start_ = start;
            origin_ = comp.getLocation();
            g_.setXORMode( Color.YELLOW );
        }

        public void dragTo( Point point ) {
            int dist = isX_ ? point.x - start_.x
                            : point.y - start_.y;
            if ( lastDist_ < 0 || dist != lastDist_ ) {
                lastDist_ = dist;
                if ( ! ( sense_ == null && dist == 0 ) ) {
                    if ( sense_ == null ) {
                        assert dist != 0;
                        sense_ = Boolean.valueOf( dist > 0 );
                    }
                    else {
                        assert last_ != null;
                        xorRegion( start_, last_ );
                    }
                    assert sense_ != null;
                    last_ = sense_.booleanValue() == dist > 0 ? point : start_;
                    xorRegion( start_, last_ );
                }
            }
        }

        public double[][] boundsAt( Point end ) {
            if ( sense_ != null ) {
                xorRegion( start_, last_ );
                Rectangle target = getTarget();
                if ( sense_.booleanValue() ) {
                    double v0 = isX_
                        ? ( start_.x - target.x ) / (double) target.width
                        : ( start_.y - target.y ) / (double) target.height;
                    double v1 = isX_
                        ? ( end.x - target.x ) / (double) target.width
                        : ( end.y - target.y ) / (double) target.height;
                    return v1 > v0 ? new double[][] { { v0, v1 } }
                                   : null;
                }
                else {
                    int dist = isX_ ? start_.x - end.x
                                    : start_.y - end.y;
                    if ( dist > 0 ) {
                        double range = ( 2 * dist + BOX ) / (double) BOX;
                        double centre = isX_
                            ? ( start_.x - target.x ) / (double) target.width
                            : ( start_.y - target.y ) / (double) target.height;
                        double v0 = centre - range * 0.5;
                        double v1 = centre + range * 0.5;
                        return new double[][] { { v0, v1 } };
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
            int v0 = isX_ ? p0.x : p0.y;
            int v1 = isX_ ? p1.x : p1.y;
            int[] range = v0 <= v1 ? new int[] { v0, v1 }
                                   : new int[] { v1, v0 };
            Rectangle rect = new Rectangle( getDisplay() );
            rect.translate( - origin_.x, - origin_.y );
            if ( isX_ ) {
                rect.x = range[ 0 ];
                rect.width = range[ 1 ] - range[ 0 ];
                rect.y += 1;
                rect.height -= 2;
            }
            else {
                rect.y = range[ 0 ];
                rect.height = range[ 1 ] - range[ 0 ];
                rect.x += 1;
                rect.width -= 2;
            }
            if ( sense_.booleanValue() ) {
                g_.drawRect( rect.x, rect.y, rect.width, rect.height );
            }
            else {
                Rectangle r1 = new Rectangle( rect );
                Rectangle r2 = new Rectangle( rect );
                if ( isX_ ) {
                    r1.x += BOX / 2 + ( range[ 1 ] - range[ 0 ] );
                    r2.x -= BOX / 2;
                }
                else {
                    r1.y += BOX / 2 + ( range[ 1 ] - range[ 0 ] );
                    r2.y -= BOX / 2;
                }
                g_.drawRect( r1.x, r1.y, r1.width, r1.height );
                g_.drawRect( r2.x, r2.y, r2.width, r2.height );
            }
        }
    }
}
