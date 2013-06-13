package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * Draws lines composed of a sequence of points, submitted one at a time.
 * To use it make multiple calls of {@link #addVertex addVertex},
 * followed by a call to {@link #flush}.
 *
 * <p>Sub-sequences of the point sequence are aggregated in supplied work
 * arrays and plotted using <code>Graphics2D.drawPolyLine</code>.
 * This is superior to the more obvious strategy of calling
 * <code>Graphics.drawLine</code> for every pair of points.
 * It is probably faster, and it is necessary to get the dashing right
 * for dashed strokes, otherwise the dash starts anew for each edge.
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
    private final int xVeryLo_;
    private final int xVeryHi_;
    private final int yVeryLo_;
    private final int yVeryHi_;
    private final Graphics2D g2_;
    private final int nwork_;
    private final int[] xWork_;
    private final int[] yWork_;
    private int iLine_;
    private boolean lastInclude_;
    private int lastX_;
    private int lastY_;
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
     */
    public LineTracer( Graphics g, Rectangle bounds, Color color,
                       Stroke stroke, boolean antialias, int nwork ) {
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
        xWork_ = new int[ nwork_ ];
        yWork_ = new int[ nwork_ ];
        lastX_ = Integer.MIN_VALUE;
        lastY_ = Integer.MIN_VALUE;
        lastInclude_ = true;
    }

    /**
     * Adds a point to the sequence to be plotted.
     *
     * <p>At present points are submitted as integer x, y coordinates since
     * that's how the rest of the plotting system works.  However, if it
     * gets upgraded to floating point, the implementation here should
     * change, perhaps using a {@link java.awt.geom.PathIterator}.
     *
     * @param  px  graphics X coordinate
     * @param  py  graphics Y coordinate
     */
    public void addVertex( int px, int py ) {

        /* This method does various calculations to optimise the points
         * that will be sent to the graphics context with drawPolyLine.
         * Although some of this may duplicate work that's done in
         * software or hardware by the graphics system (though I'm not
         * so sure), we can reduce the amount of storage required by
         * doing it here. */

        /* Don't plot points on top of each other. */
        if ( px != lastX_ || py != lastY_ ) {

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
            int regionX = getRegion( px, xlo_, xhi_ );
            int regionY = getRegion( py, ylo_, yhi_ );
            boolean include = regionX * lastRegionX_ != 1
                           && regionY * lastRegionY_ != 1;
            if ( include ) {

                /* If we didn't plot the last pair of points, add the last
                 * point to the plot list so that we're drawing from the
                 * right place to the current point. */
                if ( ! lastInclude_ ) {
                    addIncludedVertex( lastX_, lastY_ );
                }

                /* Draw to the current point. */
                addIncludedVertex( px, py );
            }

            /* If this pair will not be plotted, take the opportunity to
             * plot the currently accumulated set. */
            else {
                flush();
            }

            /* Save information about the current point since it will form
             * part of the next line segment. */
            lastX_ = px;
            lastY_ = py;
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
            g2_.drawPolyline( xWork_, yWork_, iLine_ );
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
    private void addIncludedVertex( int x, int y ) {

        /* If we've filled up the points buffer, flush it.
         * In this case, copy the last point in the full buffer as
         * the first point in the new one so that the lines join up. */
        if ( iLine_ == nwork_ ) {
            int x0 = xWork_[ iLine_ - 1 ];
            int y0 = yWork_[ iLine_ - 1 ];
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
    private int getRegion( int point, int lo, int hi ) {
        return point >= lo ? ( point < hi ? 0 
                                          : +1 )
                           : -1;
    }
}
