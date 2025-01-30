package uk.ac.starlink.ttools.plot2.layer;

import cds.healpix.Healpix;
import cds.healpix.HealpixNestedBMOC;
import cds.healpix.VerticesAndPathComputer;
import cds.healpix.common.sphgeom.Cone;
import cds.healpix.common.sphgeom.CooXYZ;
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
import uk.ac.starlink.ttools.cone.CdsHealpixUtil;
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
    private final VerticesAndPathComputer vpc_;
    private final long nside_;
    private final Rotation rotation_;
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
        vpc_ = Healpix.getNested( hpxOrder ).newVerticesAndPathComputer();
        Set<Long> visPixels =
            calculateVisiblePixels( surf, rotation, hpxOrder,
                                    PolygonTiler.CDS_POLY );
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
        return visiblePixels_.contains( Long.valueOf( hpxIndex ) );
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
        double[] dpos0 =
            CdsHealpixUtil.lonlatToVector( vpc_.center( hpxIndex ) );
        rotation_.rotate( dpos0 );
        Point2D.Double gpos0 = new Point2D.Double();
        if ( surf_.dataToGraphics( dpos0, false, gpos0 ) ) {
            double[][] lonlatVertices =
                CdsHealpixUtil
               .lonlatVertices( vpc_, hpxIndex,
                                CdsHealpixUtil.DFLT_INTERPOLATE_DEPTH );
            int nv = lonlatVertices.length;
            int[] gxs = new int[ nv ];
            int[] gys = new int[ nv ];
            double[] dpos1 = new double[ 3 ];
            Point2D.Double gpos1 = new Point2D.Double();
            int np = 0;
            int nInvisible = 0;
            for ( int i = 0; i < nv; i++ ) {
                CdsHealpixUtil.lonlatToVector( lonlatVertices[ i ], dpos1 );
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
     * Returns a list of unit vectors that outline the area visible
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
        int nq = 16;
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
                            return Long.valueOf( lx_++ );
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
         * PolygonTiler based on CDS Healpix polygon overlap implementation.
         * This one ought to be most efficient (return fewest tiles outside
         * the polygon, but it might miss some out for weird projections.
         */
        public static PolygonTiler CDS_POLY = new PolygonTiler() {
            Set<Long> queryPolygon( int order, List<double[]> xyzs ) {
                int nv = xyzs.size();
                double[][] lonlatVertices = new double[ nv ][ 2 ];
                for ( int iv = 0; iv < nv; iv++ ) {
                    CdsHealpixUtil.vectorToLonlat( xyzs.get( iv ),
                                                   lonlatVertices[ iv ] );
                }
                HealpixNestedBMOC bmoc =
                    Healpix.getNested( order ).newPolygonComputer()
                           .overlappingCells( lonlatVertices );
                return CdsHealpixUtil.bmocSet( bmoc );
            }
        };

        /**
         * PolygonTiler based on CDS Healpix cone overlap implementation.
         * Should be pretty robust, but will pull in a few extra tiles.
         */
        public static PolygonTiler CDS_CONE = new PolygonTiler() {
            Set<Long> queryPolygon( int order, List<double[]> xyzs ) {
                int nc = xyzs.size();
                CooXYZ[] coos = new CooXYZ[ nc ];
                for ( int ic = 0; ic < nc; ic++ ) {
                    double[] xyz = xyzs.get( ic );
                    coos[ ic ] = new CooXYZ( xyz[ 0 ], xyz[ 1 ], xyz[ 2 ] );
                }
                Cone cone = Cone.mec( coos );
                if ( cone != null ) {
                    HealpixNestedBMOC bmoc =
                        Healpix.getNested( order )
                       .newConeComputerApprox( cone.radiusRad() )
                       .overlappingCells( cone.lon(), cone.lat() );
                    return CdsHealpixUtil.bmocSet( bmoc );
                }
                else {
                    return null;
                }
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
