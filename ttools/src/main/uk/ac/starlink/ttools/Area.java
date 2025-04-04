package uk.ac.starlink.ttools;

import cds.healpix.Healpix;
import cds.healpix.HealpixNestedBMOC;
import cds.healpix.HealpixNestedPolygonComputer;
import java.util.Arrays;
import uk.ac.starlink.ttools.cone.CdsHealpixUtil;
import uk.ac.starlink.ttools.func.Coverage;
import uk.ac.starlink.util.LongList;

/**
 * Coordinate value representing a two-dimensional shaped area.
 * Instances of this class can be serialized to a plot tuple element.
 * The shape is defined by a numeric code (Type enum) and a numeric array,
 * so that it can be easily de/serialised.
 *
 * <p>Currently no distinction is made in this class between shapes
 * on a 2-d plane and on the surface of a sphere; instances of this
 * class may be interpreted in either context as required.
 *
 * @author   Mark Taylor
 * @since    27 Mar 2020
 */
public class Area {

    private final Type type_;
    private final double[] dataArray_;

    /**
     * Constructor.
     *
     * @param  type  area shape type
     * @param  dataArray   numeric array defining the actual shape of the area
     */
    public Area( Type type, double[] dataArray ) {
        type_ = type;
        dataArray_ = dataArray;
    }

    /**
     * Returns the type of this shape.
     *
     * @return  shape type
     */
    public Type getType() {
        return type_;
    }

    /**
     * Returns the numeric array that in conjunction with the type code
     * defines the coverage of this shape.
     *
     * @return   shape definition array
     */
    public double[] getDataArray() {
        return dataArray_;
    }

    /**
     * Writes the characteristic (typically central) position of this area
     * to a buffer that can be interpreted as the positional coordinates
     * in the data space of a plane plot (x, y values).
     * 
     * @param  buffer  output array for characteristic position,
     *                 length &gt;=2
     */
    public void writePlaneCoords2( double[] buffer ) {
        type_.writePlaneCoords2( dataArray_, buffer );
    }

    /**
     * Writes the characteristic (typically central) position of this area
     * to a buffer that can be interpreted as the positional coordinates
     * in the data space of a sky plot (3-element unit vector).
     * If the input values are out of range, the 3 components of the
     * returned vector will all be NaN.
     *
     * @param  buffer  output array for characteristic position,
     *                 length &gt;=3
     */
    public void writeSkyCoords3( double[] buffer ) {
        type_.writeSkyCoords3( dataArray_, buffer );
    }

    /**
     * Returns an array of UNIQ values corresponding to this shape
     * for a given maximum HEALPix level.
     * Tiles may be returned at a higher level than requested,
     * but the result should be at at least that resolution.
     *
     * @param  level  required HEALPix resolution level
     * @return   array of MOC tile uniq values
     */
    public long[] toMocUniqs( int level ) {
        return type_.toMocUniqs( dataArray_, level );
    }

    /**
     * Converts a list of Area instances into its MULTISHAPE numeric array
     * serialization.
     * 
     * @param  areas  array of area instances that form the multishape
     * @return   array serialization
     * @see  Type#MULTISHAPE
     */
    public static double[] serializeMultishape( Area[] areas ) {
        int nel = 1 + Arrays.stream( areas )
                            .mapToInt( a -> 2 + a.getDataArray().length )
                            .sum();
        double[] array = new double[ nel ];
        int iel = 0;
        array[ iel++ ] = areas.length;
        for ( Area area : areas ) {
            array[ iel++ ] = area.getType().ordinal();
            array[ iel++ ] = area.getDataArray().length;
        }
        for ( Area area : areas ) {
            double[] coords = area.getDataArray();
            int nc = coords.length;
            System.arraycopy( coords, 0, array, iel, nc );
            iel += nc;
        }
        assert iel == array.length;
        return array;
    }

    /**
     * Converts a numeric representation of a MULTISHAPE into a
     * list of Area instances.
     *
     * @param  data  array serialization of a MULTISHAPE
     * @return  list of decoded Area instances
     * @see  Type#MULTISHAPE
     */
    public static Area[] deserializeMultishape( double[] data ) {
        int iel = 0;
        int ns = (int) data[ iel++ ];
        Area[] shapes = new Area[ ns ];
        int kel = 1 + ns * 2;
        for ( int is = 0; is < ns; is++ ) {
            Area.Type type = Area.Type.values()[ (int) data[ iel++ ] ];
            int nc = (int) data[ iel++ ];
            double[] d = new double[ nc ];
            System.arraycopy( data, kel, d, 0, nc );
            kel += nc;
            shapes[ is ] = new Area( type, d );
        }
        assert kel == data.length;
        return shapes;
    }

    /**
     * Creates a multishape area from a list of other areas.
     *
     * @param   areas  list of subordinate areas
     * @return   new area with Type.MULTISHAPE,
     *           or null if there the <code>areas</code> list is null or empty
     */
    public static Area createMultishape( Area[] areas ) {
        return areas != null && areas.length > 0 
             ? new Area( Type.MULTISHAPE, serializeMultishape( areas ) )
             : null;
    }

    /**
     * Writes the unit vector corresponding to a latitude, longitude pair
     * into a supplied array.
     * If the input values are out of range, the 3 components of the
     * returned vector will all be NaN.
     *
     * @param  lonDeg  longitude in degrees
     * @param  latDeg  latitude in degrees
     * @param  buffer  output array for position, length &gt;=3
     */
    private static void writeLonLatSky3( double lonDeg, double latDeg,
                                         double[] buffer ) {
        if ( latDeg >= -90 && latDeg <= +90 ) {
            double theta = Math.toRadians( 90 - latDeg );
            double phi = Math.toRadians( lonDeg % 360. );
            double z = Math.cos( theta );
            double sd = Math.sin( theta );
            double x = Math.cos( phi ) * sd;
            double y = Math.sin( phi ) * sd;
            buffer[ 0 ] = x;
            buffer[ 1 ] = y;
            buffer[ 2 ] = z;
        }
        else {
            buffer[ 0 ] = Double.NaN;
            buffer[ 1 ] = Double.NaN;
            buffer[ 2 ] = Double.NaN;
        }
    }

    /**
     * Available shape types.
     */
    public enum Type {

        /**
         * Polygon or sequence of polygons defined by a list of vertices
         * (x1, y1, x2, y2, ..., xN, yN).  If a coordinate pair (xA,yA) are
         * both NaN, it indicates a break between polygons: so for instance
         * (0,0, 0,1, 1,0, NaN,NaN, 2,2, 2,3, 3,2) defines two disjoint
         * triangles.  Breaks may not occur at the start or end of the array,
         * or adjacent to each other.
         */
        POLYGON() {
            public boolean isLegalArrayLength( int n ) {
                return n % 2 == 0 && n >= 6;
            }
            public void writePlaneCoords2( double[] data, double[] buffer ) {
                int nc2 = data.length;
                double x0 = data[ 0 ];
                double y0 = data[ 1 ];
                double xmin = x0;
                double xmax = x0;
                double ymin = y0;
                double ymax = y0;
                for ( int ic2 = 2; ic2 < nc2; ic2 += 2 ) {
                    double x = data[ ic2 + 0 ];
                    double y = data[ ic2 + 1 ];
                    if ( !Double.isNaN( x ) && !Double.isNaN( y ) ) {
                        xmin = Math.min( xmin, x );
                        xmax = Math.max( xmax, x );
                        ymin = Math.min( ymin, y );
                        ymax = Math.max( ymax, y );
                    }
                }
                buffer[ 0 ] = 0.5 * ( xmin + xmax );
                buffer[ 1 ] = 0.5 * ( ymin + ymax );
            }
            public void writeSkyCoords3( double[] data, double[] buffer ) {
                double[] v3 = new double[ 3 ];
                int nc2 = data.length;
                double sx = 0;
                double sy = 0;
                double sz = 0;
                for ( int ic2 = 0; ic2 < nc2; ic2 += 2 ) {
                    double lonDeg = data[ ic2 + 0 ];
                    double latDeg = data[ ic2 + 1 ];
                    if ( !Double.isNaN( lonDeg ) && !Double.isNaN( latDeg ) ) {
                        writeLonLatSky3( lonDeg, latDeg, v3 );
                        sx += v3[ 0 ];
                        sy += v3[ 1 ];
                        sz += v3[ 2 ];
                    }
                }
                double fact = 1.0 / Math.sqrt( sx * sx + sy * sy + sz * sz );
                buffer[ 0 ] = sx * fact;
                buffer[ 1 ] = sy * fact;
                buffer[ 2 ] = sz * fact;
            }
            public long[] toMocUniqs( double[] data, int level ) {
                LongList uniqList = new LongList();
                int nc2 = data.length;
                int ic2start = 0;
                HealpixNestedPolygonComputer polyComputer =
                    Healpix.getNested( level ).newPolygonComputer();
                for ( int ic2 = 0; ic2 < nc2; ic2 += 2 ) {
                    boolean isBreak = Double.isNaN( data[ ic2 + 0 ] )
                                   || Double.isNaN( data[ ic2 + 1 ] );
                    if ( isBreak || ic2 + 2 == nc2 ) {
                        if ( ic2 > ic2start ) {
                            int nv2 = isBreak ? ic2 - ic2start
                                              : ic2 + 2 - ic2start;
                            int nv = nv2 / 2;
                            double[][] vertices = new double[ nv ][];
                            for ( int iv = 0; iv < nv; iv++ ) {
                                double lonDeg = data[ ic2start + 2 * iv + 0 ];
                                double latDeg = data[ ic2start + 2 * iv + 1 ];
                                vertices[ iv ] =
                                    new double[] { Math.toRadians( lonDeg ),
                                                   Math.toRadians( latDeg ), };
                            }
                            HealpixNestedBMOC bmoc =
                                polyComputer.overlappingCells( vertices );
                            for ( HealpixNestedBMOC.CurrentValueAccessor a :
                                  bmoc ) {
                                uniqList.add( Coverage.mocUniq( a.getDepth(),
                                                                a.getHash() ) );
                            }
                        }
                        ic2start = ic2 + 2;
                    }
                }
                return uniqList.toLongArray();
            }
        },

        /** Circle defined by central point and a radius (x, y, r). */
        CIRCLE() {
            public boolean isLegalArrayLength( int n ) {
                return n == 3;
            }
            public void writePlaneCoords2( double[] data, double[] buffer ) {
                buffer[ 0 ] = data[ 0 ];
                buffer[ 1 ] = data[ 1 ];
            }
            public void writeSkyCoords3( double[] data, double[] buffer ) {
                writeLonLatSky3( data[ 0 ], data[ 1 ], buffer );
            }
            public long[] toMocUniqs( double[] data, int level ) {
                double lonDeg = data[ 0 ];
                double latDeg = data[ 1 ];
                double rDeg = data[ 2 ];
                HealpixNestedBMOC bmoc =
                    Healpix.getNested( level )
                           .newConeComputer( Math.toRadians( rDeg ) )
                           .overlappingCells( Math.toRadians( lonDeg ),
                                              Math.toRadians( latDeg ) );
                long[] uniqs = new long[ bmoc.size() ];
                int i = 0;
                for ( HealpixNestedBMOC.CurrentValueAccessor access : bmoc ) {
                    uniqs[ i++ ] = Coverage.mocUniq( access.getDepth(),
                                                     access.getHash() );
                }
                assert i == bmoc.size();
                return uniqs;
            }
        },

        /** Point defined by two coordinates (x, y). */
        POINT() {
            public boolean isLegalArrayLength( int n ) {
                return n == 2;
            }
            public void writePlaneCoords2( double[] data, double[] buffer ) {
                buffer[ 0 ] = data[ 0 ];
                buffer[ 1 ] = data[ 1 ];
            }
            public void writeSkyCoords3( double[] data, double[] buffer ) {
                writeLonLatSky3( data[ 0 ], data[ 1 ], buffer );
            }
            public long[] toMocUniqs( double[] data, int level ) {
                double lonDeg = data[ 0 ];
                double latDeg = data[ 1 ];
                long hash = Healpix.getNestedFast( level )
                                   .hash( Math.toRadians( lonDeg ),
                                          Math.toRadians( latDeg ) );
                return new long[] { Coverage.mocUniq( level, hash ) };
            }
        },

        /**
         * Multi-Order Coverage map;
         * each array element contains 64-bit NUNIQ bit pattern equivalenced
         * to the double value.
         */
        MOC() {
            public boolean isLegalArrayLength( int n ) {
                return n > 0;
            }
            public void writePlaneCoords2( double[] data, double[] buffer ) {
                double[] r3 = new double[ 3 ];
                writeSkyCoords3( data, r3 );
                double lat = 90 - Math.toDegrees( Math.acos( r3[ 2 ] ) );
                double lon = Math.toDegrees( Math.atan2( r3[ 1 ], r3[ 0 ] ) );
                buffer[ 0 ] = lon;
                buffer[ 1 ] = lat;
            }
            public void writeSkyCoords3( double[] data, double[] buffer ) {
                int nd = data.length;
                double tx = 0;
                double ty = 0;
                double tz = 0;
                double[] lonlat = new double[ 2 ];
                double[] xyz = new double[ 3 ];
                for ( int i = 0; i < nd; i++ ) {
                    long uniq = Double.doubleToRawLongBits( data[ i ] );
                    int order = ( 61 - Long.numberOfLeadingZeros( uniq ) ) >> 1;
                    long ipix = uniq - ( 4L << ( 2 * order ) );
                    Healpix.getNestedFast( order ).center( ipix, lonlat );
                    CdsHealpixUtil.lonlatToVector( lonlat, xyz );
                    double factor = 1.0 / ( 1L << ( 2 * order ) );
                    tx += factor * xyz[ 0 ];
                    ty += factor * xyz[ 1 ];
                    tz += factor * xyz[ 2 ];
                }
                double scale = 1.0 / Math.sqrt( tx * tx + ty * ty + tz * tz );
                buffer[ 0 ] = tx * scale;
                buffer[ 1 ] = ty * scale;
                buffer[ 2 ] = tz * scale;
            }
            public long[] toMocUniqs( double[] data, int level ) {
                // Note this doesn't currently reduce the MOC to the requested
                // level.  That's OK, given the semantics of the level argument.
                int npix = data.length;
                long[] moc = new long[ npix ];
                for ( int i = 0; i < npix; i++ ) {
                    moc[ i ] = Double.doubleToRawLongBits( data[ i ] );
                }
                return moc;
            }
        },

        /**
         * Collection of Area instances, to be considered as a union.
         * Serialization is
         * <code>[nshape, type0, ncoord0, type1, ncoord1, ..., coords]</code>.
         * Type values are ordinals of the {@link Type} enum.
         *
         * @see #serializeMultishape serializeMultishape
         * @see #deserializeMultishape deserializeMultishape
         */
        MULTISHAPE() {
            public boolean isLegalArrayLength( int n ) {
                return n > 3;
            }
            public void writePlaneCoords2( double[] data, double[] buffer ) {
                Area[] shapes = deserializeMultishape( data );
                assert shapes.length > 0;
                Area shape0 = shapes[ 0 ];
                shape0.getType()
                      .writePlaneCoords2( shape0.getDataArray(), buffer );
                double x0 = buffer[ 0 ];
                double y0 = buffer[ 1 ];
                double xmin = x0;
                double xmax = x0;
                double ymin = y0;
                double ymax = y0;
                for ( int is = 1; is < shapes.length; is++ ) {
                    Area shape = shapes[ is ];
                    shape.getType()
                         .writePlaneCoords2( shape.getDataArray(), buffer );
                    double x = buffer[ 0 ];
                    double y = buffer[ 1 ];
                    xmin = Math.min( xmin, x );
                    xmax = Math.max( xmax, x );
                    ymin = Math.min( ymin, y );
                    ymax = Math.max( ymax, y );
                }
                buffer[ 0 ] = 0.5 * ( xmin + xmax );
                buffer[ 1 ] = 0.5 * ( ymin + ymax );
            }
            public void writeSkyCoords3( double[] data, double[] buffer ) {
                double sx = 0;
                double sy = 0;
                double sz = 0;
                for ( Area shape : deserializeMultishape( data ) ) {
                    shape.getType()
                         .writeSkyCoords3( shape.getDataArray(), buffer );
                    sx += buffer[ 0 ];
                    sy += buffer[ 1 ];
                    sz += buffer[ 2 ];
                }
                double fact = 1.0 / Math.sqrt( sx * sx + sy * sy + sz * sz );
                buffer[ 0 ] = sx * fact;
                buffer[ 1 ] = sy * fact;
                buffer[ 2 ] = sz * fact;
            }
            public long[] toMocUniqs( double[] data, int level ) {
                LongList uniqList = new LongList();
                for ( Area shape : deserializeMultishape( data ) ) {
                    uniqList.addAll( shape.getType()
                                    .toMocUniqs( shape.getDataArray(), level ));
                }
                return uniqList.toLongArray();
            }
        };

        private static final Type[] VALUES = values();

        /**
         * Indicates whether a given data array length can represent
         * a shape of this type.
         *
         * @param  n  length of candidate array
         * @return   true iff this shape can be represented by a data array
         *           of the given length
         */
        public abstract boolean isLegalArrayLength( int n );

        /**
         * Writes the characteristic (typically central) position of an area
         * of this type to a buffer that can be interpreted
         * as the positional coordinates in the data space of
         * a plane plot (x, y values).
         * 
         * @param  data   data array containing shape details
         * @param  buffer  output array for characteristic position,
         *                 length &gt;=2
         */
        abstract void writePlaneCoords2( double[] data, double[] buffer );

        /**
         * Writes the characteristic (typically central) of an area
         * of this type to a buffer that can be interpreted
         * as the positional coordinates in the data space of
         * a sky plot (3-element unit vector).
         * If the input values are out of range, the 3 components of the
         * returned vector will all be NaN.
         * 
         * @param  data   data array containing shape details
         * @param  buffer  output array for characteristic position,
         *                 length &gt;=3
         */
        abstract void writeSkyCoords3( double[] data, double[] buffer );


        /**
         * Returns an array of UNIQ values corresponding to this shape
         * for a given maximum HEALPix level.
         * Tiles may be returned at a higher level than requested,
         * but the result should be at at least that resolution.
         *
         * @param  data   data array containing shape details
         * @param  level  maximum HEALPix resolution level
         * @return   array of MOC tile uniq values
         */
        public abstract long[] toMocUniqs( double[] data, int level );

        /**
         * Retrieves an instance of this enum from its numeric code.
         *
         * @param   itype  type ordinal
         * @return  type instance
         */
        public static Type fromInt( int itype ) {
            return itype >= 0 && itype < VALUES.length
                 ? VALUES[ itype ]
                 : null;
        }
    }
}
