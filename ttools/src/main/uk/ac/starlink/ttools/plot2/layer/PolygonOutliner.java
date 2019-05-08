package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingArrayCoord;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.geom.CubeDataGeom;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.geom.GPoint3D;
import uk.ac.starlink.ttools.plot2.geom.PlaneDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * Outliner implementations for plotting shapes defined by listing
 * three or more vertices in data space.
 * There are lots of implementation details hidden in this class,
 * but external users should just need the public static factory methods.
 *
 * @author   Mark Taylor
 * @since    5 Mar 2019
 */
public class PolygonOutliner extends PixOutliner {

    private final PolygonMode.Glypher polyGlypher_;
    private final VertexReaderFactory vrfact_;
    private final Icon icon_;

    /**
     * Constructor.
     *
     * @param  polyGlypher  object that knows how to paint a polygon
     * @param  vrfact   object that knows how to get vertex positions from
     *                  a data tuple
     */
    private PolygonOutliner( PolygonMode.Glypher polyGlypher,
                             VertexReaderFactory vrfact ) {
        polyGlypher_ = polyGlypher;
        vrfact_ = vrfact;
        icon_ = new MultiPosIcon( 4 ) {
            protected void paintPositions( Graphics g, Point[] positions ) {
                int np = positions.length;
                int[] xs = new int[ np ];
                int[] ys = new int[ np ];
                for ( int i = 0; i < np; i++ ) {
                    xs[ i ] = positions[ i ].x;
                    ys[ i ] = positions[ i ].y;
                }
                polyGlypher_.paintPolygon( g, xs, ys, np );
            }
        };
    }

    public Icon getLegendIcon() {
        return icon_;
    }

    public Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom ) {
        return new HashMap<AuxScale,AuxReader>();
    }

    public ShapePainter create2DPainter( final Surface surf,
                                         final DataGeom geom,
                                         Map<AuxScale,Span> auxSpans,
                                         final PaperType2D paperType ) {
        final VertexReader vertReader = vrfact_.createVertexReader( geom );
        Rectangle bounds = surf.getPlotBounds();
        final int bxMin = bounds.x;
        final int bxMax = bounds.x + bounds.width;
        final int byMin = bounds.y;
        final int byMax = bounds.y + bounds.height;
        int ndim = surf.getDataDimCount();
        final double[] dpos0 = new double[ ndim ];
        final double[] dpos = new double[ ndim ];
        final Point2D.Double gpos0 = new Point2D.Double();
        final Point2D.Double gpos = new Point2D.Double();
        final Point igpos = new Point();
        final Glyph pointGlyph = XYShape.POINT;
        return new ShapePainter() {
            public void paintPoint( Tuple tuple, Color color, Paper paper ) {
                VertexData vdata = vertReader.readVertexData( tuple );

                /* Get the position of the first vertex in integer
                 * (rounded) graphics coordinates. */
                if ( vdata.readDataPos( 0, dpos0 ) &&
                     surf.dataToGraphics( dpos0, false, gpos0 ) &&
                     PlotUtil.isPointFinite( gpos0 ) ) {
                    int np = vdata.getVertexCount();
                    if ( np > 0 ) {
                        int[] gxs = new int[ np ];
                        int[] gys = new int[ np ];
                        PlotUtil.quantisePoint( gpos0, igpos );
                        gxs[ 0 ] = igpos.x;
                        gys[ 0 ] = igpos.y;

                        /* Get the graphics positions of the other vertices.
                         * Reject the polygon if there is no continuous line
                         * between the first vertex and each of the others.
                         * This is to defend against drawing polygons going the
                         * wrong way around the sphere in sky plots. */
                        for ( int ip = 1; ip < np; ip++ ) {
                            if ( vdata.readDataPos( ip, dpos ) &&
                                 surf.dataToGraphics( dpos, false, gpos ) &&
                                 surf.isContinuousLine( dpos0, dpos ) &&
                                 PlotUtil.isPointFinite( gpos ) ) {
                                PlotUtil.quantisePoint( gpos, igpos );
                                gxs[ ip ] = igpos.x;
                                gys[ ip ] = igpos.y;
                            }
                            else {
                                return;
                            }
                        }

                        /* Work out the bounds of the graphics rectangle
                         * enclosing the polygon. */
                        int gxMin = bxMax;
                        int gxMax = bxMin;
                        int gyMin = byMax;
                        int gyMax = byMin;
                        for ( int ip = 0; ip < np; ip++ ) {
                            int gx = gxs[ ip ];
                            int gy = gys[ ip ];
                            gxMin = Math.min( gxMin, gx );
                            gxMax = Math.max( gxMax, gx );
                            gyMin = Math.min( gyMin, gy );
                            gyMax = Math.max( gyMax, gy );
                        }

                        /* If the bounds are outside the plot, do nothing.
                         * If the bounds are all the same (in integer
                         * graphics coordinates), the polygon can be
                         * represented cheaply as a single point.
                         * Otherwise, draw it properly. */
                        if ( gxMax >= bxMin && gxMin <= bxMax &&
                             gyMax >= byMin && gyMin <= byMax ) {
                            if ( gxMin == gxMax && gyMin == gyMax ) {
                                paperType.placeGlyph( paper, gxMin, gyMin,
                                                      pointGlyph, color );
                            }
                            else {
                                polyGlypher_.placeGlyphs2D( paperType, paper,
                                                            gxs, gys, np,
                                                            color );
                            }
                        }
                    }
                }
            }
        };
    }

    public ShapePainter create3DPainter( final CubeSurface surf, DataGeom geom,
                                         Map<AuxScale,Span> auxSpans,
                                         final PaperType3D paperType ) {
        final VertexReader vertReader = vrfact_.createVertexReader( geom );
        Rectangle bounds = surf.getPlotBounds();
        final int bxMin = bounds.x;
        final int bxMax = bounds.x + bounds.width;
        final int byMin = bounds.y;
        final int byMax = bounds.y + bounds.height;
        int ndim = surf.getDataDimCount();
        final double[] dpos = new double[ ndim ];
        final GPoint3D gpos = new GPoint3D();
        final Point igpos = new Point();
        final Glyph pointGlyph = XYShape.POINT;
        return new ShapePainter() {
            public void paintPoint( Tuple tuple, Color color, Paper paper ) {
                VertexData vdata = vertReader.readVertexData( tuple );
                int np = vdata.getVertexCount();
                if ( np > 0 ) {
                    int[] gxs = new int[ np ];
                    int[] gys = new int[ np ];
                    double sz = 0;

                    /* Read all the vertex positions in graphics space.
                     * In this case we only accept polygons for which all
                     * vertices are visible within the 3d plot bounds,
                     * because of the difficulty of partially clipping
                     * polygons in 3d.  This means that polygons near the
                     * edge of the visible cube may not be painted.
                     * 3d plots don't have the possibility of discontinuous
                     * lines, so we don't need to defend against that here. */
                    for ( int ip = 0; ip < np; ip++ ) {
                        if ( vdata.readDataPos( ip, dpos ) &&
                             surf.dataToGraphicZ( dpos, true, gpos ) &&
                             PlotUtil.isPointFinite( gpos ) ) {
                            PlotUtil.quantisePoint( gpos, igpos );
                            gxs[ ip ] = igpos.x;
                            gys[ ip ] = igpos.y;
                            sz += gpos.z;
                        }
                        else {
                            return;
                        }
                    }

                    /* Work out the bounding box in the two graphics dimensions
                     * for the polygon. */
                    int gxMin = bxMax;
                    int gxMax = bxMin;
                    int gyMin = byMax;
                    int gyMax = byMin;
                    for ( int ip = 0; ip < np; ip++ ) {
                        int gx = gxs[ ip ];
                        int gy = gys[ ip ];
                        gxMin = Math.min( gxMin, gx );
                        gxMax = Math.max( gxMax, gx );
                        gyMin = Math.min( gyMin, gy );
                        gyMax = Math.max( gyMax, gy );
                    }

                    /* If it falls within the graphics bounds, plot it at the
                     * mean Z coordinate of all the vertices.  This is a fudge,
                     * but it's the best we can easily do.  Take a short cut
                     * if it's a point. */
                    if ( gxMax >= bxMin && gxMin <= bxMax &&
                         gyMax >= byMin && gyMin <= byMax ) {
                        double gz = sz / np;
                        if ( gxMin == gxMax && gyMin == gyMax ) {
                            paperType.placeGlyph( paper, gxMin, gyMin, gz,
                                                  pointGlyph, color );
                        }
                        else {
                            polyGlypher_.placeGlyphs3D( paperType, paper,
                                                        gxs, gys, np, gz,
                                                        color );
                        }
                    }
                }
            }
        };
    }

    @Override
    public int hashCode() {
        int code = 434482;
        code = 23 * code + polyGlypher_.hashCode();
        code = 23 * code + vrfact_.hashCode();
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof PolygonOutliner ) {
            PolygonOutliner other = (PolygonOutliner) o;
            return this.polyGlypher_.equals( other.polyGlypher_ )
                && this.vrfact_.equals( other.vrfact_ );
        }
        else {
            return false;
        }
    }

    /**
     * Returns an outliner for polygons with a fixed number of vertices.
     *
     * @param  np  number of vertices
     * @param  polyGlypher  polygon painter
     * @return  outliner
     */
    public static PolygonOutliner
            createFixedOutliner( int np, PolygonMode.Glypher polyGlypher ) {
        return new PolygonOutliner( polyGlypher,
                                    new FixedVertexReaderFactory( np ) );
    }

    /**
     * Returns an outliner for polygons defined by an array-valued coordinate
     * providing interleaved coordinates in user data space.
     * Each array instance may be of length N*D, where D is the number of
     * coordinates per point in user space.
     * For instance a triangle in plane coordinates would be
     * (x1,y1, x2,y2, x3,y3),
     * and in sky coordinates
     * (lon1,lat1, lon2,lat2, lon3,lat3).
     *
     * @param  arrayCoord   array-valued coordinate
     * @param  polyGlypher  polygon painter
     * @return  outliner
     */
    public static PolygonOutliner
            createArrayOutliner( FloatingArrayCoord arrayCoord,
                                 PolygonMode.Glypher polyGlypher ) {
        return new PolygonOutliner( polyGlypher,
                                    new ArrayVertexReaderFactory( arrayCoord ));
    }

    /**
     * Defines how to acquire vertex coordinates from a plot data tuple.
     */
    @Equality
    private interface VertexReaderFactory {

        /**
         * Constructs a vertex reader appropriate for a given DataGeom.
         *
         * @param  geom  geometry for vertex coordinates
         * @return  vertex reader
         */
        VertexReader createVertexReader( DataGeom geom );
    }

    /**
     * Can acquire vertex coordinates from a plot data tuple.
     */
    private interface VertexReader {

        /**
         * Reads polygon vertex data from a given tuple.
         *
         * @param  tuple  data tuple
         * @return  polygon vertex information
         */
        VertexData readVertexData( Tuple tuple );
    }

    /**
     * Information about the vertices of a polygon.
     */
    private interface VertexData {

        /**
         * Returns the number of vertices.
         *
         * @return  number of vertices
         */
        int getVertexCount();

        /**
         * Acquires the data coordinates of one vertex of a polygon.
         *
         * @param  ivert  vertex index
         * @param  dpos   array to receive vertex coordinates in data space
         * @return   true iff dpos contains a successfully converted
         *           vertex position on exit
         */
        boolean readDataPos( int ivert, double[] dpos );
    }

    /**
     * VertexReaderFactory implementation for polygons with a fixed
     * number of vertices.
     */
    private static class FixedVertexReaderFactory
            implements VertexReaderFactory {
        private final int np_;

        /**
         * Constructor.
         *
         * @param  np  number of vertices per polygon
         */
        FixedVertexReaderFactory( int np ) {
            np_ = np;
        }

        public VertexReader createVertexReader( final DataGeom geom ) {
            final int[] icPos = new int[ np_ ];
            for ( int ip = 0; ip < np_; ip++ ) {
                icPos[ ip ] = getPosCoordIndex( ip, geom );
            }
            return new VertexReader() {
                public VertexData readVertexData( final Tuple tuple ) {
                    return new VertexData() {
                        public int getVertexCount() {
                            return np_;
                        }
                        public boolean readDataPos( int ipos, double[] dpos ) {
                            return geom
                                  .readDataPos( tuple, icPos[ ipos ], dpos );
                        }
                    };
                }
            };
        }

        @Override
        public int hashCode() {
            int code = 288901;
            code = 23 * code + np_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof FixedVertexReaderFactory ) {
                FixedVertexReaderFactory other = (FixedVertexReaderFactory) o;
                return this.np_ == other.np_;
            }
            else {
                return false;
            }
        }

        /**
         * Returns the tuple coordinate index for a given vertex.
         *
         * @param  ivert  vertex index
         * @param  geom   data geom
         * @return  position in tuple at which vertex appears
         */
        private int getPosCoordIndex( int ivert, DataGeom geom ) {
            // Note this is questionable: it should really be
            // CoordGroup.getPosCoordIndex( ivert, geom ), but we don't have
            // the CoordGroup here.
            return geom.getPosCoords().length * ivert;
        }
    }

    /**
     * VertexReaderFactory implementation for polygons with vertices
     * supplied using an array-valued coordinate.
     * Coordinates are interleaved, for example (x1,y1, x2,y2, x3,y3).
     */
    private static class ArrayVertexReaderFactory
            implements VertexReaderFactory {
        private final FloatingArrayCoord arrayCoord_;

        /**
         * Constructor.
         *
         * @param  arrayCoord   array-valued coordinate
         */
        ArrayVertexReaderFactory( FloatingArrayCoord arrayCoord ) {
            arrayCoord_ = arrayCoord;
        }

        public VertexReader createVertexReader( DataGeom geom ) {
            if ( geom instanceof PlaneDataGeom ) {
                return new ArrayVertexReader( arrayCoord_, geom ) {
                    boolean readArrayPos( double[] array, int icPos,
                                          double[] dpos ) {
                        double x = array[ icPos ];
                        if ( ! Double.isNaN( x ) ) {
                            double y = array[ icPos + 1 ];
                            if ( ! Double.isNaN( y ) ) {
                                dpos[ 0 ] = x;
                                dpos[ 1 ] = y;
                                return true;
                            }
                        }
                        return false;
                    }
                };
            }
            else if ( geom instanceof CubeDataGeom ) {
                return new ArrayVertexReader( arrayCoord_, geom ) {
                    boolean readArrayPos( double[] array, int icPos,
                                          double[] dpos ) {
                        double x = array[ icPos ];
                        if ( ! Double.isNaN( x ) ) {
                            double y = array[ icPos + 1 ];
                            if ( ! Double.isNaN( y ) ) {
                                double z = array[ icPos + 2 ];
                                if ( ! Double.isNaN( z ) ) {
                                    dpos[ 0 ] = x;
                                    dpos[ 1 ] = y;
                                    dpos[ 2 ] = z;
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                };
            }
            else if ( geom instanceof SkyDataGeom ) {
                final SkyDataGeom skyGeom = (SkyDataGeom) geom;
                return new ArrayVertexReader( arrayCoord_, geom ) {
                    boolean readArrayPos( double[] array, int icPos,
                                          double[] dpos ) {
                        double latDeg = array[ icPos + 1 ];
                        if ( Math.abs( latDeg ) <= 90 ) {
                            double lonDeg = array[ icPos ];
                            if ( PlotUtil.isFinite( lonDeg ) ) {
                                double theta = Math.toRadians( 90 - latDeg );
                                double phi = Math.toRadians( lonDeg % 360. );
                                double z = Math.cos( theta );
                                double sd = Math.sin( theta );
                                double x = Math.cos( phi ) * sd;
                                double y = Math.sin( phi ) * sd;
                                dpos[ 0 ] = x;
                                dpos[ 1 ] = y;
                                dpos[ 2 ] = z;
                                skyGeom.rotate( dpos );
                                return true;
                            }
                        }
                        return false;
                    }
                };
            }
            else {
                assert false;
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public int hashCode() {
            int code = 222389;
            code = 23 * code + arrayCoord_.hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof ArrayVertexReaderFactory ) {
                ArrayVertexReaderFactory other = (ArrayVertexReaderFactory) o;
                return this.arrayCoord_.equals( other.arrayCoord_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * VertexReader implementation for use with the ArrayVertexReaderFactory.
     */
    private static abstract class ArrayVertexReader implements VertexReader {

        private final FloatingArrayCoord arrayCoord_;
        private final DataGeom geom_;
        private final int nuc_;
        private final int icPos0_;
        private final int icArray_;

        /**
         * Constructor.
         *
         * @param  arrayCoord  vertex array data coordinate
         * @param  geom      geometry for vertices
         */
        ArrayVertexReader( FloatingArrayCoord arrayCoord, DataGeom geom ) {
            arrayCoord_ = arrayCoord;
            geom_ = geom;

            /* Work out how many elements of the array coordinate will
             * correspond to a single position. */
            int nuc = 0;
            for ( Coord c : geom.getPosCoords() ) {
                nuc += c.getInputs().length;
            }
            nuc_ = nuc;

            /* Determine where in the tuple to find the initial position
             * coordinates and the array position coordinate.
             * This is not the correct way to do it: really we should use
             * the getPosCoordIndex and getExtraCoordIndex methods of
             * CoordGroup, but we don't have the CoordGroup here.
             * This implementation should work however if the coord group
             * looks as we expect it to for a polygon plotter. */
            icPos0_ = 0;
            icArray_ = geom.getPosCoords().length;
        }

        public VertexData readVertexData( final Tuple tuple ) {
            final double[] array =
                arrayCoord_.readArrayCoord( tuple, icArray_ );
            int nc = array.length;
            final int nv = nc % nuc_ == 0 ? 1 + nc / nuc_ : 0;
            return new VertexData() {
                public int getVertexCount() {
                    return nv;
                }
                public boolean readDataPos( int ipos, double[] dpos ) {
                    return ipos == 0
                         ? geom_.readDataPos( tuple, icPos0_, dpos )
                         : readArrayPos( array, ( ipos - 1 ) * nuc_, dpos );
                }
            };
        }

        /**
         * Reads the position of a single vertex from an array
         * containing multiple interleaved vertex coordinates
         * (for instance x1,y1,x2,y2,...)
         *
         * @param   array  array data
         * @param   icPos  starting index in array for coordinate tuple
         * @param   dpos   destination for vertex coordinates in data space
         * @return   true iff read succeeded; if true the dpos array
         *           will contain usable coordinates on exit
         */
        abstract boolean readArrayPos( double[] array, int icPos,
                                       double[] dpos );
    }
}
