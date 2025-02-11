package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.function.IntSupplier;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Pixer that iterates over all points inside a polygon.
 * Should handle all polygons (convex, concave, re-entrant).
 * I think(?) the algorithm is about as efficient as it's going to get
 * for a single-threaded implementation.  Memory consumption is low.
 *
 * @author   Mark Taylor
 * @since    6 Oct 2021
 * @see  <a href="https://alienryderflex.com/polygon_fill/"
 *               >https://alienryderflex.com/polygon_fill/</a>
 */
public class FillPixer implements Pixer {

    private final int np_;
    private final boolean hasBounds_;
    private final int[] us_;
    private final int[] vs_;
    private final int umin_;
    private final int umax_;
    private final int vmin_;
    private final int vmax_;
    private final double[] grads_;
    private final IntSupplier getX_;
    private final IntSupplier getY_;
    private final int[] nodes_;  // list of crossing points on current row
    private int nspan_;          // number of start,stop pairs in nodes_
    private int ispan_;          // current start,stop pair
    private int spanhi_;         // end of current span
    private int u_;
    private int v_;

    /* We have the machinery to map the X and Y coordinates to U and V
     * adaptively according to whether the shape is short and fat or
     * tall and thin, since it is expected to be faster to fill in
     * a small number of long rows than a large number of short rows.
     * This switch configures whether such adaptive switching is in fact done.
     * It is turned off, since although doing it like that works,
     * different rounding errors for the different orientations mean
     * that you end up with pixel-order gaps between polygons that
     * should butt right up to each other's edges. */
    private static final boolean OPTIMISE_XY = false;

    /**
     * Constructor.
     *
     * @param   xs   np-element array giving graphics X vertex coordinates
     * @param   ys   np-element array giving graphics Y vertex coordinates
     * @param  np  number of vertices
     * @param  bounds  actual bounds within which pixels are required,
     *                 or null for all pixels
     */
    public FillPixer( int[] xs, int[] ys, int np, Rectangle bounds ) {
        np_ = np;
        nodes_ = new int[ np ];

        /* Calculate polygon bounds. */
        int xlo = xs[ 0 ];
        int xhi = xs[ 0 ];
        int ylo = ys[ 0 ];
        int yhi = ys[ 0 ];
        for ( int ip = 1; ip < np; ip++ ) {
            int x = xs[ ip ];
            int y = ys[ ip ];
            xlo = Math.min( xlo, x );
            xhi = Math.max( xhi, x );
            ylo = Math.min( ylo, y );
            yhi = Math.max( yhi, y );
        }

        /* Adjust bounds according to supplied clip (if any),
         * and record whether clip checking will be required later. */
        if ( bounds == null ) {
            hasBounds_ = false;
        }
        else {
            boolean hasBounds = false;
            int bxlo = bounds.x;
            int bxhi = bounds.x + bounds.width - 1;
            int bylo = bounds.y;
            int byhi = bounds.y + bounds.height - 1;
            if ( bxlo > xlo ) {
                xlo = bxlo;
                hasBounds = true;
            }
            if ( bylo > ylo ) {
                ylo = bylo;
                hasBounds = true;
            }
            if ( bxhi < xhi ) {
                xhi = bxhi;
                hasBounds = true;
            }
            if ( byhi < yhi ) {
                yhi = byhi;
                hasBounds = true;
            }
            hasBounds_ = hasBounds;
        }

        /* Map the externally supplied X and Y coordinates to the U and V
         * coordinates we use internally.
         * Unless disabled, this uses V as the slower-varying index (row value)
         * U as the faster- varying one, since this should be quicker
         * than the other way round.
         * The code may be easier to read if you think of U=X and Y=V,
         * though for tall thin shapes the mapping will actually be
         * the other way round. */
        if ( !OPTIMISE_XY || ( yhi - ylo <= xhi - xlo ) ) {
            us_ = xs;
            vs_ = ys;
            umin_ = xlo;
            umax_ = xhi;
            vmin_ = ylo;
            vmax_ = yhi;
            getX_ = () -> u_;
            getY_ = () -> v_;
        }
        else {
            us_ = ys;
            vs_ = xs;
            umin_ = ylo;
            umax_ = yhi;
            vmin_ = xlo;
            vmax_ = xhi;
            getX_ = () -> v_;
            getY_ = () -> u_;
        }

        /* Pre-calculate line gradients, since we will need them later. */
        grads_ = new double[ np ];
        for ( int ip = 0; ip < np; ip++ ) {
            int ip1 = ( ip + 1 ) % np;
            grads_[ ip ] = ( us_[ ip1 ] - us_[ ip ] ) 
                / (double) ( vs_[ ip1 ] - vs_[ ip ] ); 
        }

        /* Initialise iteration state. */
        v_ = vmin_ - 1;
    }

    public boolean next() {
        while ( ++u_ > spanhi_ ) {
            while ( ++ispan_ >= nspan_ ) {
                if ( ++v_ > vmax_ ) {
                    return false;
                }
                updateSpans( v_ );
                ispan_--;
            }
            u_ = nodes_[ ispan_ * 2 ];
            spanhi_ = nodes_[ ispan_ * 2 + 1 ];
        }
        return true;
    }

    public int getX() {
        return getX_.getAsInt();
    }

    public int getY() {
        return getY_.getAsInt();
    }

    /**
     * Initialises state for iterating over points in a new row.
     * This updates the nodes_, nspan_ and ispan_ variables.
     * 
     * @param  v  row for which spans are required
     */
    private void updateSpans( int v ) {

        /* Fill nodes_ array with all U values at which edges cross this
         * V value. */
        int in = 0;
        for ( int ip = 0; ip < np_; ip++ ) {
            int ip1 = ( ip + 1 ) % np_;
            int v0 = vs_[ ip ];
            int v1 = vs_[ ip1 ];

            /* Only use edges that exist at this V value. */
            if ( v >= v0 ^ v >= v1 ) {
                nodes_[ in++ ] = us_[ ip ]
                               + PlotUtil.ifloor( grads_[ ip ] * ( v - v0 ) );
            }
        }
        assert in % 2 == 0;
        nspan_ = in / 2;
        ispan_ = 0;
        if ( ispan_ < nspan_ ) {

            /* Sort the values.  The result is a set of pairs of
             * (shape-start-u, shape-end-u) values, each of which we
             * call a span. */
            Arrays.sort( nodes_, 0, in );

            /* Clip spans if applicable. */
            if ( hasBounds_ ) {

                /* Remove any spans at either end which are completely outside
                 * the clip. */
                while ( ispan_ < nspan_ &&
                        nodes_[ 2 * ispan_ + 1 ] < umin_ ) {
                    ispan_++;
                }
                while ( ispan_ < nspan_ &&
                        nodes_[ nspan_ * 2 - 2 ] >= umax_ ) {
                    nspan_--;
                }

                /* Trim any spans which intersect the clip bounds. */
                if ( ispan_ < nspan_ ) {
                    nodes_[ ispan_ * 2 ] =
                        Math.max( nodes_[ ispan_ * 2 ], umin_ );
                    nodes_[ nspan_ * 2 - 1 ] =
                        Math.min( nodes_[ nspan_ * 2 - 1], umax_ );
                }
            }
        }
    }
}
