package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Line2D;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Draws lines composed of a sequence of points, submitted one at a time.
 * To use it make multiple calls of {@link #addVertex addVertex},
 * followed by a call to {@link #flush}.
 *
 * <p>Where possible, sub-sequences of the point sequence
 * are aggregated in supplied work
 * arrays and plotted using <code>Graphics2D.draw(Shape)</code>.
 * This is superior to the more obvious strategy of calling
 * <code>Graphics.drawLine</code> for every pair of points.
 * It is probably faster, it can work with non-integer coordinates,
 * I think the line joining is better,
 * and it is necessary to get the dashing right for dashed strokes,
 * otherwise the dash starts anew for each edge.
 * However, this is only possible for runs of points of the same colour;
 * line segments of different colours have to be drawn separately,
 * which means that lines of varying colour may look worse than
 * single colour ones (poorer sub-pixel positioning, nastier line joins,
 * incorrect dash phase).
 *
 * <p>This class does some other useful things like avoid attempts to plot
 * lines which are extremely long or which are known to be outside the clip.
 *
 * @author   Mark Taylor
 * @since    12 Jun 2013
 */
public class LineTracer {

    private final int xlo_;
    private final int xhi_;
    private final int ylo_;
    private final int yhi_;
    private final int xmid_;
    private final int ymid_;
    private final double gLimit2_;
    private final Graphics2D g2_;
    private final int nwork_;
    private final double[] xWork_;
    private final double[] yWork_;
    private final VertexStore lastVertex_;
    private int iLine_;
    private Color lastColor_;
    private int lastRegionX_;
    private int lastRegionY_;

    /**
     * Constructor.
     * The <code>nwork</code> parameter determines the number of points
     * aggregated into a single plotting call.
     * There may be visual anomalies every <code>nwork</code> points, so
     * it should not be too small, but arrays of this size are allocated,
     * so it should not be too large either.
     *
     * @param   g  the base graphics context
     * @param   bounds  bounds beyond which lines should not be drawn
     * @param   stroke  line stroke
     * @param   antialias  whether lines are to be antialiased
     * @param   nwork  workspace array size
     * @param   isPixel   if true, the graphics context is considered to be
     *                    pixellised, allowing some optimisations to be made
     *                    that should not be visible
     */
    public LineTracer( Graphics g, Rectangle bounds, Stroke stroke,
                       boolean antialias, int nwork, boolean isPixel ) {
        nwork_ = nwork;
        xlo_ = bounds.x;
        xhi_ = bounds.x + bounds.width;
        ylo_ = bounds.y;
        yhi_ = bounds.y + bounds.height;

        /* Set up a graphics context in which polylines will be drawn. */
        g2_ = (Graphics2D) g.create();
        g2_.clip( bounds );
        g2_.setStroke( stroke );
        g2_.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                              antialias ? RenderingHints.VALUE_ANTIALIAS_ON
                                        : RenderingHints.VALUE_ANTIALIAS_OFF );

        /* Work out distances so far from the clip that no attempt should be
         * made to draw lines there. */
        xmid_ = ( xlo_ + xhi_ ) / 2;
        ymid_ = ( ylo_ + yhi_ ) / 2;
        gLimit2_ = Math.pow( Math.max( bounds.width, bounds.height ) * 100, 2 );

        /* Set up workspace arrays and plotting state. */
        xWork_ = new double[ nwork_ ];
        yWork_ = new double[ nwork_ ];
        lastVertex_ = createVertexStore( isPixel );
    }

    /**
     * Adds a point to the sequence to be plotted.
     * A null value for the <code>color</code> argument results in a
     * break in the line.
     *
     * @param  dx  graphics X coordinate
     * @param  dy  graphics Y coordinate
     * @param  color  line colour at point
     */
    public void addVertex( double dx, double dy, Color color ) {

        /* As well as handling the colour manipulations required for
         * aux-shaded lines, this method does various calculations to
         * optimise the polyline that is sent to the graphics context
         * for painting. */

        /* Treat a null colour as a break in the line. */
        if ( color == null ) {
            flushPoly();
            iLine_ = 0;
        }

        /* Only proceed if the current point is not right on top of
         * the previous one, since in that case the line would be
         * zero length and hence invisible. */
        else if ( ! lastVertex_.equalsVertex( dx, dy ) ) {
            int regionX = getRegion( dx, xlo_, xhi_ );
            int regionY = getRegion( dy, ylo_, yhi_ );

            /* If the current polyline is empty (this is the first point
             * in the line, or the first point since a break in the line),
             * initialise the polyline with the coordinates of the current
             * point. */
            if ( iLine_ == 0 ) {
                addIncludedVertex( dx, dy );
            }

            /* Find out if the line between the previous and current points
             * needs to be drawn.  First compare the 'regions' for the
             * X and Y coordinates; this simple arithmetic is able to
             * exclude many lines that will not cross the plot bounds,
             * since if two points are both (e.g.) to the left of the bounds,
             * the line between them cannot intercept the bounds so the line
             * does not need to be drawn.  Also check whether the current
             * point is right on top of the previous one; if so, the
             * line would be zero length and hence invisible. */
            else if ( regionX * lastRegionX_ != 1 &&
                      regionY * lastRegionY_ != 1 ) {

                /* If the colours assigned to the two points are the same,
                 * just add the point to the list that will be drawn as
                 * a polyline in the current colour.  This will get
                 * plotted eventually. */
                if ( color.equals( lastColor_ ) ) {
                    addIncludedVertex( dx, dy );
                }

                /* If the colours are different, first flush any pending
                 * polyline, then draw this line segment in a way that
                 * fades from one colour to the other. */
                else {
                    flushPoly();
                    assert iLine_ == 1;
                    addIncludedVertex( dx, dy );
                    flushPair0( lastColor_, color );
                }
            }

            /* Otherwise, no line will be drawn to the current point.
             * Take the opportunity to flush any pending polyline,
             * and start a new (potential) polyline starting with the
             * current point. */
            else {
                flushPoly();
                iLine_ = 0;
                addIncludedVertex( dx, dy );
            }

            /* Store information about the current point that will be
             * needed when processing the next one. */
            lastVertex_.setVertex( dx, dy );
            lastRegionX_ = regionX;
            lastRegionY_ = regionY;
            assert iLine_ > 0;
        }
        lastColor_ = color;
    }

    /**
     * Ensures that all points have been drawn.
     * Since points are plotted in bursts for reasons of aesthetics and
     * efficiency, this must be called after all {@link #addVertex} calls
     * to ensure that the drawing has actually been done.
     */
    public void flush() {
        flushPoly();
    }

    /**
     * Ensures that all lines in the pending polyline have been drawn,
     * and prepares it for subsequent use.
     */
    private void flushPoly() {
        if ( iLine_ > 1 ) {
            assert lastColor_ != null;
            g2_.setColor( lastColor_ );
            Path2D.Double path =
                new Path2D.Double( Path2D.WIND_NON_ZERO, iLine_ );
            path.moveTo( xWork_[ 0 ], yWork_[ 0 ] );
            for ( int ip = 1; ip < iLine_; ip++ ) {
                path.lineTo( xWork_[ ip ], yWork_[ ip ] );
            }
            g2_.draw( path );
            xWork_[ 0 ] = xWork_[ iLine_ - 1 ];
            yWork_[ 0 ] = yWork_[ iLine_ - 1 ];
            iLine_ = 1;
        }
    }

    /**
     * Paints a line between the first two positions in the pending polyline,
     * with a colour fading linearly between two supplied values.
     * Must be called when only two positions are in the polyline.
     * On exit, the polyline contains only the ending position,
     * ready for subsequent lines.
     *
     * @param  color0  colour for starting point
     * @param  color1  colour for ending point
     */
    private void flushPair0( Color color0, Color color1 ) {
        assert iLine_ == 2;
        double dx0 = xWork_[ 0 ];
        double dy0 = yWork_[ 0 ];
        double dx1 = xWork_[ 1 ];
        double dy1 = yWork_[ 1 ];
        g2_.setPaint( new GradientPaint( (float) dx0, (float) dy0, color0,
                                         (float) dx1, (float) dy1, color1 ) );
        g2_.draw( new Line2D.Double( dx0, dy0, dx1, dy1 ) );
        xWork_[ 0 ] = dx1;
        yWork_[ 0 ] = dy1;
        iLine_ = 1;
    }

    /**
     * Adds a vertex to the pending polyline which will have lines
     * drawn between them.
     *
     * @param   x  X graphics coordinate
     * @param   y  Y graphics coordinate
     */
    private void addIncludedVertex( double x, double y ) {

        /* If we've filled up the points buffer, flush it.
         * In this case, copy the last point in the full buffer as
         * the first point in the new one so that the lines join up. */
        if ( iLine_ == nwork_ ) {
            flush();
            assert iLine_ == 1;
        }

        /* If an attempt is made to draw to a line which is monstrously
         * far off the graphics clip, it can lead to problems (e.g.
         * the display system attempting to locate so much memory that
         * the kernel kills the JVM).  In this case, approximate the
         * point to somewhere far away in roughly the right direction.
         * This isn't likely to happen very often in any case. */
        double sx = x - xmid_;
        double sy = y - ymid_;
        double s2 = sx * sx + sy + sy;
        if ( s2 > gLimit2_ ) {
            double theta = Math.atan2( sy, sx );
            double r = Math.sqrt( gLimit2_ );
            x = xmid_ + r * Math.cos( theta );
            y = ymid_ + r * Math.sin( theta );
        }
     
        /* Store the point for later plotting. */
        xWork_[ iLine_ ] = x;
        yWork_[ iLine_ ] = y;
        iLine_++;
    }

    /**
     * Returns the region of a point with respect to an interval.
     * The return value is -1, 0, or 1 according to whether the point
     * is lower than, within, or higher than the interval bounds.
     *
     * @param   point   test value
     * @param   lo    region lower bound
     * @param   hi    region upper bound
     * @return  region code
     */
    private static int getRegion( double point, int lo, int hi ) {
        return point >= lo ? ( point < hi ? 0
                                          : +1 )
                           : -1;
    }

    /**
     * Returns a vertex store suitable for a given context.
     *
     * @param  isPixel  true iff graphics are to be treated as pixellated
     * @return   new vertex store
     */
    private static VertexStore createVertexStore( boolean isPixel ) {
        if ( isPixel ) {
            return new VertexStore() {
                private int gx_ = Integer.MIN_VALUE;
                private int gy_ = Integer.MIN_VALUE;
                public double getX() {
                    return gx_;
                }
                public double getY() {
                    return gy_;
                }
                public void setVertex( double x, double y ) {
                    gx_ = toInt( x );
                    gy_ = toInt( y );
                }
                public boolean equalsVertex( double x, double y ) {
                    return toInt( x ) == gx_ && toInt( y ) == gy_;
                }
                private int toInt( double d ) {
                    return PlotUtil.ifloor( d );
                }
            };
        }
        else {
            return new VertexStore() {
                private double dx_ = Double.NaN;
                private double dy_ = Double.NaN;
                public double getX() {
                    return dx_;
                }
                public double getY() {
                    return dy_;
                }
                public void setVertex( double x, double y ) {
                    dx_ = x;
                    dy_ = y;
                }
                public boolean equalsVertex( double x, double y ) {
                    return x == dx_ && y == dy_;
                }
            };
        }
    }

    /**
     * Stores a graphics position.
     */
    private interface VertexStore {

        /**
         * Returns the current X coordinate.
         *
         * @return  x
         */
        double getX();

        /**
         * Returns the current Y coordinate.
         *
         * @return  y
         */
        double getY();

        /**
         * Sets the current coordinates.
         *
         * @param   dx   X coordinate
         * @param   dy   Y coordinate
         */
        void setVertex( double dx, double dy );

        /**
         * Determines whether the current position is equal
         * (as near as makes no difference in the context of this object)
         * to the given coordinates.
         *
         * @param   dx  X coordinate to test
         * @param   dy  Y coordinate to test
         * @return  true  iff supplied coordinates are close to the
         *                current vertex
         */
        boolean equalsVertex( double dx, double dy );
    }
}
