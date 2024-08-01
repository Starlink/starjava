package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * ZoomRegion implementation for a zoom around the centre of the 
 * display region.
 *
 * @author   Mark Taylor
 * @since    13 Apr 2006
 * @see      Zoomer
 */
public abstract class CentreZoomRegion extends ZoomRegion {

    private final boolean isX_;
    private static final int BOX = 32;

    /**
     * Constructor.
     *
     * @param   isX  true if the target region is horizontal, 
     *               false for vertical
     */
    @SuppressWarnings("this-escape")
    public CentreZoomRegion( boolean isX ) {
        isX_ = isX;
        setCursor( Cursor.getPredefinedCursor( isX ? Cursor.W_RESIZE_CURSOR
                                                   : Cursor.N_RESIZE_CURSOR ) );
    }

    public ZoomDrag createDrag( Component comp, Point start ) {
        return new CentreDrag( comp, start );
    }

    public abstract Rectangle getTarget();

    public abstract Rectangle getDisplay();

    /**
     * Drag object implementation for CentreZoomRegion.
     */
    private class CentreDrag implements ZoomDrag {
        final Point start_;
        final Graphics g_;
        final Rectangle target_;
        final Rectangle display_;
        final Point centre_;
        final double aspect_;
        Point last_;
        Boolean sense_;  // true for zoom in, false for zoom out
        int lastDist_;

        /**
         * Constructor.
         *
         * @param  comp  component on which drag will take place
         * @param  start  start of drag in target region
         */
        CentreDrag( Component comp, Point start ) {
            g_ = comp.getGraphics();
            start_ = start;
            g_.setXORMode( Color.YELLOW );
            Point origin = comp.getLocation();
            target_ = new Rectangle( getTarget() );
            display_ = new Rectangle( getDisplay() );
            target_.translate( - origin.x, - origin.y );
            display_.translate( - origin.x, - origin.y );
            centre_ = new Point( display_.x + display_.width / 2,
                                 display_.y + display_.height / 2 );
            aspect_ = (double) display_.height / (double) display_.width;
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

        /**
         * Returns a single-element double[][] array which gives a
         * value saying how much the zoom should be.  Greater than one is
         * a zoom in, less than one is a zoom out.
         */
        public double[][] boundsAt( Point end ) {
            if ( sense_ != null ) {
                xorRegion( start_, last_ );
                int range = Math.abs( isX_ ? end.x - start_.x
                                           : end.y - start_.y );
                if ( range > 3 ) {
                    if ( sense_.booleanValue() ) {
                        double scale = 
                            2.0 * range / (double) ( isX_ ? target_.width
                                                          : target_.height );
                        return new double[][] { { scale } };
                    }
                    else {
                        double scale = 1.0 + 2.0 * range / (double) BOX;
                        return new double[][] { { scale } };
                    }
                }
                else {
                    return null;
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
            int range = Math.abs( isX_ ? p0.x - p1.x
                                       : p0.y - p1.y );
            int xrange = isX_ ? range : (int) ( range / aspect_ );
            int yrange = isX_ ? (int) ( range * aspect_ ) : range;
            if ( sense_.booleanValue() ) {
                g_.drawRect( centre_.x - xrange, centre_.y - yrange,
                             2 * xrange, 2 * yrange );
            }
            else {
                int xbox = isX_ ? BOX : (int) ( BOX / aspect_ );
                int ybox = isX_ ? (int) ( BOX * aspect_ ) : BOX;
                g_.drawRect( centre_.x - xbox / 2, centre_.y - ybox / 2,
                             xbox, ybox );
                g_.drawRect( centre_.x - xbox / 2 - range,
                             centre_.y - ybox / 2 - range,
                             xbox + 2 * range, ybox + 2 * range );
            }
        }
    }
}
