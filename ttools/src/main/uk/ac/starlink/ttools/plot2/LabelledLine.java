package uk.ac.starlink.ttools.plot2;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * Aggregates a line in graphics coordinates and its annotation.
 * The annotation is intended for human consumption.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2019
 */
public class LabelledLine {

    private final Point2D[] gps_;
    private final String label_;

    /**
     * Constructs a straight labelled line between two points.
     *
     * @param  gp0  start point in graphics space
     * @param  gp1  end point in graphics space
     * @param  label  human-readable annotation for line (may be null)
     */
    public LabelledLine( Point2D gp0, Point2D gp1, String label ) {
        this( new Point2D[] { gp0, gp1 }, label );
    }

    /**
     * Constructs a labelled line with an arbitrary number of points.
     * Null elements may appear in the points array indicating a break
     * in the line.  There must be at least two array elements, and the
     * first and last elements must both be non-null.
     *
     * @param  gps  array of points in graphics space defining the line
     * @param  label  human-readable annotation for line (may be null)
     */
    public LabelledLine( Point2D[] gps, String label ) {
        if ( gps.length < 2 ) {
            throw new IllegalArgumentException( "Not enough points" );
        }
        gps_ = gps;
        label_ = label;
    }

    /**
     * Returns the array of points defining this line.
     *
     * @return  array of (at least 2) points
     */
    public Point2D[] getPoints() {
        return gps_;
    }

    /**
     * Returns the annotation.
     *
     * @return  human-readable label for line
     */
    public String getLabel() {
        return label_;
    }

    /**
     * Draws the line to a given graphics context.
     * The current settings of the graphics context are used.
     *
     * @param  g2  graphics context
     */
    public void drawLine( Graphics2D g2 ) {
        g2.draw( getPath() );
    }

    /**
     * Returns a path object corresponding to this line.
     *
     * @return  path
     */
    public Path2D getPath() {
        int np = gps_.length;
        Path2D.Double path = new Path2D.Double( Path2D.WIND_NON_ZERO, np );
        boolean brk = true;
        for ( Point2D gp : gps_ ) {
            if ( gp != null ) {
                double gx = gp.getX();
                double gy = gp.getY();
                if ( brk ) {
                    path.moveTo( gx, gy );
                }
                else {
                    path.lineTo( gx, gy );
                }
            }
            brk = gp == null;
        }
        return path;
    }

    /**
     * Draws the label in the middle of the line to a given graphics context.
     * The current settings of the graphics context are used.
     * If a non-null background colour is supplied, a rectangle
     * corresponding to the label bounds is plotted underneath
     * the label itself.
     *
     * @param  g2  graphics context
     * @param  bg  background colour, or null
     */
    public void drawLabel( Graphics2D g2, Color bg ) {
        if ( label_ == null ) {
            return;
        }
        int np = gps_.length;

        /* Short cut for the common case of a straight line. */
        if ( np == 2 ) {
            drawLabel( g2, bg, gps_[ 0 ], gps_[ 1 ], 0.5 );
        }

        /* General case of multi-point line. */
        else {

            /* Prepare an array of cumulative line length per input point. */
            double[] dists = new double[ np ];
            for ( int ip = 1; ip < np; ip++ ) {
                Point2D gpA = gps_[ ip - 1 ];
                Point2D gpB = gps_[ ip - 0 ];
                double d = gpA == null || gpB == null
                         ? 0
                         :  Math.hypot( gpB.getX() - gpA.getX(),
                                        gpB.getY() - gpA.getY() );
                dists[ ip ] = dists[ ip - 1 ] + d;
            }

            /* Identify the point half way along the line, and draw the
             * label there. */
            double half = 0.5 * dists[ np - 1 ];
            for ( int ip = 1; ip < np; ip++ ) {
                if ( dists[ ip ] >= half ) {
                    drawLabel( g2, bg, gps_[ ip - 1 ], gps_[ ip ],
                               ( half - dists[ ip - 1 ] )
                               / ( dists[ ip ] - dists[ ip - 1 ] ) );
                    return;
                }
            }
            assert false;
        }
    }

    /**
     * Draws the label at a given fractional distance along a straight line
     * defined by two given points.  The text will be rotated so that
     * it runs along the line in question, though flipped if necessary
     * so it's not upside down.
     *
     * @param  g2  graphics context
     * @param  bg  background colour, or null
     * @param  gp0  start point of line
     * @param  gp1  end point of line
     * @param  frac  fractional distance along line at which text should
     *               be centered
     */
    private void drawLabel( Graphics2D g2, Color bg, Point2D gp0, Point2D gp1,
                            double frac ) {
        g2 = (Graphics2D) g2.create();
        double gx0 = gp0.getX();
        double gy0 = gp0.getY();
        double gx1 = gp1.getX();
        double gy1 = gp1.getY();
        boolean flip = gx0 == gx1 ? ( gy0 > gy1 ) : ( gx0 > gx1 );
        double gxA = flip ? gx1 : gx0;
        double gyA = flip ? gy1 : gy0;
        double gxB = flip ? gx0 : gx1;
        double gyB = flip ? gy0 : gy1;
        g2.translate( PlotUtil.scaleValue( gx0, gx1, frac ),
                      PlotUtil.scaleValue( gy0, gy1, frac ) );
        g2.rotate( Math.atan2( gyB - gyA, gxB - gxA ) );
        FontMetrics fm = g2.getFontMetrics();
        g2.translate( - fm.stringWidth( label_ ) / 2, -3 );
        if ( bg != null ) {
            Color color0 = g2.getColor();
            g2.setColor( bg );
            g2.fill( fm.getStringBounds( label_, g2 ).getBounds() );
            g2.setColor( color0 );
        }
        g2.drawString( label_, 0, 0 );
    }

    @Override
    public String toString() {
        return label_ + ": " + gps_[ 0 ] + "->" + gps_[ gps_.length - 1 ];
    }
}
