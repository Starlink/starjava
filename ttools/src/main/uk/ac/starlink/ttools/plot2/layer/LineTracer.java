package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Draws lines composed of a sequence of points, submitted one at a time.
 * To use it make multiple calls of {@link #addVertex addVertex},
 * followed by a call to {@link #flush}.
 *
 * <p>Sub-sequences of the point sequence are aggregated in supplied work
 * arrays and plotted using <code>Graphics2D.draw(Shape)</code>.
 * This is superior to the more obvious strategy of calling
 * <code>Graphics.drawLine</code> for every pair of points.
 * It is probably faster, it can work with non-integer coordinates,
 * and it is necessary to get the dashing right for dashed strokes,
 * otherwise the dash starts anew for each edge.
 * This class does some other useful things like avoid attempts to plot
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
    private final double xVeryLo_;
    private final double xVeryHi_;
    private final double yVeryLo_;
    private final double yVeryHi_;
    private final Graphics2D g2_;
    private final int nwork_;
    private final double[] xWork_;
    private final double[] yWork_;
    private int iLine_;
    private boolean lastInclude_;
    private VertexStore lastVertex_;
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
     * @param   color   line colour
     * @param   stroke  line stroke
     * @param   antialias  whether lines are to be antialiased
     * @param   nwork  workspace array size
     * @param   isPixel   if true, the graphics context is considered to be
     *                    pixellised, allowing some optimisations to be made
     *                    that should not be visible
     */
    public LineTracer( Graphics g, Rectangle bounds, Color color,
                       Stroke stroke, boolean antialias, int nwork,
                       boolean isPixel ) {
        nwork_ = nwork;
        xlo_ = bounds.x;
        xhi_ = bounds.x + bounds.width;
        ylo_ = bounds.y;
        yhi_ = bounds.y + bounds.height;

        /* Set up a graphics context in which polylines will be drawn. */
        g2_ = (Graphics2D) g.create();
        g2_.clip( bounds );
        g2_.setColor( color );
        g2_.setStroke( stroke );
        g2_.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                              antialias ? RenderingHints.VALUE_ANTIALIAS_ON
                                        : RenderingHints.VALUE_ANTIALIAS_OFF );

        /* Work out distances so far from the clip that no attempt should be
         * made to draw lines there. */
        int huge = Math.max( bounds.width, bounds.height ) * 100;
        xVeryLo_ = xlo_ - huge;
        xVeryHi_ = xhi_ + huge;
        yVeryLo_ = ylo_ - huge;
        yVeryHi_ = yhi_ + huge;

        /* Set up workspace arrays and plotting state. */
        xWork_ = new double[ nwork_ ];
        yWork_ = new double[ nwork_ ];
        lastVertex_ = createVertexStore( isPixel );
        lastInclude_ = true;
    }

    /**
     * Adds a point to the sequence to be plotted.
     *
     * @param  dx  graphics X coordinate
     * @param  dy  graphics Y coordinate
     */
    public void addVertex( double dx, double dy ) {

        /* This method does various calculations to optimise the points
         * that will be sent to the graphics context with drawPolyLine.
         * Although some of this may duplicate work that's done in
         * software or hardware by the graphics system (though I'm not
         * so sure), we can reduce the amount of storage required by
         * doing it here. */

        /* Don't plot points on top of each other. */
        if ( ! lastVertex_.equalsVertex( dx, dy ) ) {

            /* Work out for X and Y whether the current point is within
             * the plot bounds or on one side or the other, and compare
             * it to the same information about the last point.
             * If two points are both (e.g.) to the left of the bounds,
             * the line between then cannot intercept the bounds so
             * the line between them does not need to be drawn.
             * Otherwise, even if neither point is in the bounds,
             * the line might cross the bounds, so plot it.
             * The arithmetic of the region function makes it easy to
             * work this out. */
            int regionX = getRegion( dx, xlo_, xhi_ );
            int regionY = getRegion( dy, ylo_, yhi_ );
            boolean include = regionX * lastRegionX_ != 1
                           && regionY * lastRegionY_ != 1;
            if ( include ) {

                /* If we didn't plot the last pair of points, add the last
                 * point to the plot list so that we're drawing from the
                 * right place to the current point. */
                if ( ! lastInclude_ ) {
                    addIncludedVertex( lastVertex_.getX(), lastVertex_.getY() );
                }

                /* Draw to the current point. */
                addIncludedVertex( dx, dy );
            }

            /* If this pair will not be plotted, take the opportunity to
             * plot the currently accumulated set. */
            else {
                flush();
            }

            /* Save information about the current point since it will form
             * part of the next line segment. */
            lastVertex_.setVertex( dx, dy );
            lastInclude_ = include;
            lastRegionX_ = regionX;
            lastRegionY_ = regionY;
        }
    }

    /**
     * Ensures that all points have been drawn.
     * Since points are plotted in bursts for reasons of aesthetics and
     * efficiency, this must be called after all {@link #addVertex} calls
     * to ensure that the drawing has actually been done.
     */
    public void flush() {
        if ( iLine_ > 1 ) {
            g2_.draw( createLineShape( xWork_, yWork_, iLine_ ) );
        }
        iLine_ = 0;
    }

    /**
     * Adds a vertex to the list which will have lines drawn between
     * them.
     *
     * @param   x  X graphics coordinate
     * @param   y  Y graphics coordinate
     */
    private void addIncludedVertex( double x, double y ) {

        /* If we've filled up the points buffer, flush it.
         * In this case, copy the last point in the full buffer as
         * the first point in the new one so that the lines join up. */
        if ( iLine_ == nwork_ ) {
            double x0 = xWork_[ iLine_ - 1 ];
            double y0 = yWork_[ iLine_ - 1 ];
            flush();
            xWork_[ 0 ] = x0;
            yWork_[ 0 ] = y0;
            iLine_++;
        }

        /* If an attempt is made to draw to a line which is monstrously
         * far off the graphics clip, it can lead to problems (e.g.
         * the display system attempting to locate so much memory that
         * the kernel kills the JVM).  In this case, approximate the
         * point to somewhere far away in roughly the right direction.
         * This isn't likely to happen very often in any case. */
        x = Math.max( xVeryLo_, Math.min( xVeryHi_, x ) );
        y = Math.max( yVeryLo_, Math.min( yVeryHi_, y ) );

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
     * Turns an array of coordinates into a Shape representing line
     * segments joining them.
     *
     * @param  xs  X coordinates
     * @param  ys  Y coordinates
     * @param  np  number of points (used length of xs and ys arrays)
     * @return    polyline shape
     */
    private static Shape createLineShape( double[] xs, double[] ys, int np ) {
        GeneralPath path = new GeneralPath( GeneralPath.WIND_NON_ZERO, np );
        path.moveTo( (float) xs[ 0 ], (float) ys[ 0 ] );
        for ( int ip = 1; ip < np; ip++ ) {
            path.lineTo( (float) xs[ ip ], (float) ys[ ip ] );
        }
        return path;
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
    private static interface VertexStore {

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
