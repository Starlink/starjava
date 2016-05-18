package uk.ac.starlink.ttools.plot2.layer;

import gov.fnal.eag.healpix.PixTools;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import javax.vecmath.Vector3d;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.geom.Rotation;
import uk.ac.starlink.ttools.plot2.geom.SkySurface;

/**
 * Understands the geometry of HEALPix tiles on a given SkySurface.
 *
 * <p><strong>Note:</strong> this class is intended for use when the
 * number of tiles is moderately small.  Use of resources (memory,
 * runtime?) is likely to be linear in the number of pixels at the
 * required order appearing within the given surface.
 *
 * <p><strong>Note:</strong> instances of this class are not
 * safe for concurrent use from multiple threads.
 *
 * @author   Mark Taylor
 * @since    31 Mar 2016
 */
public class SkySurfaceTiler {

    private final SkySurface surf_;
    private final long nside_;
    private final Rotation rotation_;
    private final PixTools pixTools_;
    private final Set<Long> visiblePixels_;

    /**
     * Constructor.
     *
     * @param  surf   sky surface
     * @param  rotation  additional rotation to apply to sky positions, not null
     * @param  hpxOrder   healpix order (0, 1, 2, ..)
     */
    public SkySurfaceTiler( SkySurface surf, Rotation rotation, int hpxOrder ) {
        surf_ = surf;
        nside_ = 1L << hpxOrder;
        rotation_ = rotation;
        pixTools_ = new PixTools();
        Set<Long> visPixels =
            calculateVisiblePixels( surf, rotation, hpxOrder,
                                    PolygonTiler.PIXTOOLS_DISC );
        visiblePixels_ = Collections.unmodifiableSet( visPixels );
    }

    /**
     * Returns a collection of pixels that are, or may be, visible on the
     * surface.  It may contain false positives, but an attempt is made to
     * keep that number low.  Calling this method is cheap (the same object
     * is returned every time), and iterating over the result
     * is a sensible way to find all the visible pixels.
     *
     * @return  an unmodifiable set of HEALPix indices for pixesl that are,
     *          or may be visible on the surface.
     */
    public Set<Long> visiblePixels() {
        return visiblePixels_;
    }

    /**
     * Indicates whether a given tile is considered to be visible on this
     * tiler's plot surface.  False positives are permitted (but preferably
     * not too many).
     * This should be faster to execute than {@link #getTileShape}.
     *
     * @param  hpxIndex  HEALPix index
     * @return   true iff tile may be visible
     */
    public boolean isVisible( long hpxIndex ) {
        return visiblePixels_.contains( new Long( hpxIndex ) );
    }

    /**
     * Returns the shape of the given tile on the sky surface.
     * The result is an approximation using integer graphics coordinates.
     *
     * <p>Calling this method is not an efficient way to determine whether
     * a given pixel is visible; use {@link #isVisible isVisible} instead.
     *
     * @param   hpxIndex  HEALPix index
     * @return   shape of indicated tile on graphics plane,
     *           or null if known to be invisible
     */
    public Polygon getTileShape( long hpxIndex ) {
        Vector3d v3 = pixTools_.pix2vect_nest( nside_, hpxIndex );
        double[] dpos0 = { v3.x, v3.y, v3.z };
        Point2D.Double gpos0 = new Point2D.Double();
        rotation_.rotate( dpos0 );
        if ( surf_.dataToGraphics( dpos0, false, gpos0 ) ) {
            double[][] vertices = pixTools_.pix2vertex_nest( nside_, hpxIndex );
            int[] gxs = new int[ 4 ];
            int[] gys = new int[ 4 ];
            double[] dpos1 = new double[ 3 ];
            Point2D.Double gpos1 = new Point2D.Double();
            int np = 0;
            int nInvisible = 0;
            for ( int i = 0; i < 4; i++ ) {
                dpos1[ 0 ] = vertices[ 0 ][ i ];
                dpos1[ 1 ] = vertices[ 1 ][ i ];
                dpos1[ 2 ] = vertices[ 2 ][ i ];
                rotation_.rotate( dpos1 );
                if ( surf_.dataToGraphicsOffset( dpos0, gpos0, dpos1,
                                                 false, gpos1 ) ) {
                    assert ! Double.isNaN( gpos1.x );
                    assert ! Double.isNaN( gpos1.y );
                    gxs[ np ] = PlotUtil.ifloor( gpos1.x );
                    gys[ np ] = PlotUtil.ifloor( gpos1.y );
                    np++;
                }
                else {
                    if ( ++nInvisible > 1 ) {
                        return null;
                    }
                }
            }
            assert np >= 1;
            return new Polygon( gxs, gys, np );
        }
        else {
            return null;
        }
    }

    /**
     * Returns a set of HEALPix index values corresponding to all those that
     * are visible on this tiler's plot surface.
     * False positives are permitted, but an attempt is made to keep the
     * number of them low.
     * The <code>contains</code> method of the returned collection
     * is expected to be fast.
     *
     * @param  surf   sky surface
     * @param  rotation  additional rotation to apply to sky positions, not null
     * @param  order   healpix order (0, 1, 2, ..)
     * @return   set of visible pixel indices
     */
    private static Set<Long> calculateVisiblePixels( SkySurface surf,
                                                     Rotation rotation,
                                                     int order,
                                                     PolygonTiler ptiler ) {

        /* Prepare a polygon corresponding to the plot surface bounds. */
        List<double[]> vertexList = createSurfacePolygon( surf, rotation );

        /* Try to work out what HEALPix pixels this covers. */
        Set<Long> indexSet = vertexList == null
                           ? null
                           : ptiler.queryPolygon( order, vertexList );

        /* If that worked return it, otherwise use a custom Set that includes
         * all pixels at the current HEALPix order. */
        return indexSet != null ? indexSet
                                : createIntegerSet( 12L << ( 2 * order ) );
    }

    /**
     * Returns a list of Vector3d objects that outline the area visible
     * within the plotting bounds of this object's plotting surface.
     *
     * @param  surf   sky surface
     * @param  rotation  additional rotation to apply to sky positions, not null
     * @return   list of polygon vertices outlining plot surface
     */
    private static List<double[]> createSurfacePolygon( SkySurface surf,
                                                        Rotation rotation ) {
        Rectangle bounds = surf.getPlotBounds();
        Rotation unrot = rotation.invert();
        int nq = 4;
        double nq1 = 1.0 / nq;
        List<double[]> vertexList = new ArrayList<double[]>( 4 * nq + 1 );
        for ( int is = 0; is < 4; is++ ) {
            for ( int iq = 0; iq < nq; iq++ ) {
                Point2D.Double gpos = traceEdge( bounds, is, iq * nq1 );
                double[] dpos = surf.graphicsToData( gpos, null );
                if ( dpos == null ) {
                    return null;
                }
                else {
                    unrot.rotate( dpos );
                    vertexList.add( dpos );
                }
            }
        }
        vertexList.add( vertexList.get( 0 ) );
        return vertexList;
    }

    /**
     * Takes a position part-way along a named edge of a rectangle.
     * Looping over the sides in order will trace round the edge of the
     * rectangle.
     *
     * @param  bounds  input rectangle
     * @param  iside   label of side, in range 0..3
     * @param  fraction  fractional distance along the side, in range 0..1
     * @return    coordinates for specified point on edge of bounds
     */
    private static Point2D.Double traceEdge( Rectangle bounds, int iside,
                                             double fraction ) {
        final double gx;
        final double gy;
        if ( iside == 0 ) {
            gx = bounds.x + bounds.width * fraction;
            gy = bounds.y;
        }
        else if ( iside == 1 ) {
            gx = bounds.x + bounds.width;
            gy = bounds.y + bounds.height * fraction;
        }
        else if ( iside == 2 ) {
            gx = bounds.x + bounds.width * ( 1.0 - fraction );
            gy = bounds.y + bounds.height;
        }
        else if ( iside == 3 ) {
            gx = bounds.x;
            gy = bounds.y + bounds.height * ( 1.0 - fraction );
        }
        else {
            throw new IllegalArgumentException();
        }
        return new Point2D.Double( gx, gy );
    }

    /**
     * Returns a collection of long integers that contains the first
     * <code>leng</code> integers.
     * The implementation is efficient in memory usage and
     * performance of the <code>contains</code> method.
     *
     * @param  leng  number of integers in set
     * @return  ordered collection of all values &gt;=0 and &lt;leng
     */
    private static Set<Long> createIntegerSet( final long leng ) {
        return new AbstractSet<Long>() {
            public int size() {
                return (int) leng;
            }
            @Override
            public boolean contains( Object o ) {
                if ( o instanceof Number ) {
                    long l = ((Number) o).longValue();
                    return l >= 0 && l < leng;
                }
                else {
                    return false;
                }
            }
            public Iterator<Long> iterator() {
                return new Iterator<Long>() {
                    private long lx_;
                    public boolean hasNext() {
                        return lx_ < leng;
                    }
                    public Long next() {
                        if ( hasNext() ) {
                            return new Long( lx_++ );
                        }
                        else {
                            throw new NoSuchElementException();
                        }
                    }
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * Abstracts healpix implementation for determining polygon tile inclusion.
     */
    private static abstract class PolygonTiler {

        /**
         * Implementation based on PixTools.query_polygon.
         * This looks like it should be the right way to do it,
         * but in practice it's not much use, since it doesn't work well
         * if the supplied polygon is not convex (it writes to stdout
         *    The polygon has more than one concave vertex,
         *    The result is unpredictable
         * and the results often miss pixels).  In most cases it seems
         * that concave polygons are what we need to give it.
         * Note that the JHealpix queryPolygonInclusiveNest implementation
         * suffers from the same thing (though that just throws an
         * exception).
         */
        public static final PolygonTiler PIXTOOLS_POLY = new PolygonTiler() {
            private static final long NEST = 1;
            private static final long INCLUSIVE = 1;
            Set<Long> queryPolygon( int order, List<double[]> vertices ) {
                ArrayList<Vector3d> v3list =
                    new ArrayList<Vector3d>( vertices.size() );
                for ( double[] p : vertices ) {
                    v3list.add( new Vector3d( p[ 0 ], p[ 1 ], p[ 2 ] ) );
                }
                long nside = 1L << order;
                final List<Long> indexList;
                try {
                    indexList = new PixTools()
                               .query_polygon( nside, v3list, NEST, INCLUSIVE );
                }
                catch ( Exception e ) {
                    return null;
                }
                return new TreeSet<Long>( indexList );
            }
        };

        /**
         * Implementation based on PixTools.query_disc.
         * It essentially draws a circle that encloses all the vertices,
         * and returns the pixels within that.
         */
        public static final PolygonTiler PIXTOOLS_DISC = new PolygonTiler() {
            private static final int NEST = 1;
            private static final int INCLUSIVE = 1;
            Set<Long> queryPolygon( int order, List<double[]> vertices ) {

                /* Determine the vector at the center of the polygon.
                 * This doesn't need to be that accurate; we need a reference
                 * point that is ideally equidistant from the vertices. */
                double[] sv = new double[ 3 ];
                for ( double[] dpos : vertices ) {
                    for ( int i = 0; i < 3; i++ ) {
                        sv[ i ] += dpos[ i ];
                    }
                }
                double fv = 1.0 / Math.sqrt( sv[ 0 ] * sv[ 0 ] +
                                             sv[ 1 ] * sv[ 1 ] +
                                             sv[ 2 ] * sv[ 2 ] );
                double[] c0 = { sv[ 0 ] * fv, sv[ 1 ] * fv, sv[ 2 ] * fv };

                /* Go over each vertex and record the maximum angular distance
                 * between the center and any of the vertices. */
                double maxTheta = 0;
                for ( double[] dpos : vertices ) {
                    double dotp = c0[ 0 ] * dpos[ 0 ]
                                + c0[ 1 ] * dpos[ 1 ]
                                + c0[ 2 ] * dpos[ 2 ];
                    if ( dotp < 0 ) {
                        return null;
                    }
                    else {
                        maxTheta = Math.max( maxTheta, Math.acos( dotp ) );
                    }
                }

                /* Then get the pixels for a circle thus defined.
                 * It must enclose all the supplied vertices (though it
                 * probably includes quite a bit extra too). */
                final List<Long> indexList;
                try {
                    long nside = 1L << order;
                    Vector3d cv = new Vector3d( c0[ 0 ], c0[ 1 ], c0[ 2 ] );
                    indexList = new PixTools()
                               .query_disc( nside, cv, maxTheta,
                                            NEST, INCLUSIVE );
                }
                catch ( Exception e ) {
                    return null;
                }
                return new TreeSet<Long>( indexList );
            }
        };

        /**
         * Returns a list of healpix indices at a given order covered
         * (at least partially) by a polygon whose vertices are supplied.
         *
         * @param  order   healpix order (0, 1, 2, ..)
         * @param  vertices  list of polygon vertices,
         *                   as unit 3-vectors on the sky 
         * @return   list of healpix nested pixel indices
         */
        abstract Set<Long> queryPolygon( int order, List<double[]> vertices );
    }
}
