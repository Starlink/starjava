package uk.ac.starlink.ttools.plot2.data;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Coord implementation for sky positions.
 * To the user these look like (longitude, latitude) pairs,
 * but they are stored as (x,y,z) triples.
 * This class exists in two variants: for surface the vectors are normalised
 * to a magnitude of 1, so that they all fall on the centre of the unit sphere.
 * For volume, that constraint does not apply.
 *
 * @author   Mark Taylor
 * @since    4 Feb 2013
 */
public abstract class SkyCoord implements Coord {

    private final SkyVariant skyVariant_;
    private final StorageType storageType_;
    private final boolean isRequired_;
    private static final double[] NO_SKY =
        new double[] { Double.NaN, Double.NaN, Double.NaN };
    private static final int[] ZERO3 = new int[ 3 ];

    /**
     * Constructor.
     *
     * @param   skyVariant  indicates whether vectors are all  
     *                      on surface of sphere
     * @param   storageType  indicates numeric type for data storage
     * @param   isRequired  true if this coordinate is required for plotting
     */
    private SkyCoord( SkyVariant skyVariant, StorageType storageType,
                      boolean isRequired ) {
        skyVariant_ = skyVariant;
        storageType_ = storageType;
        isRequired_ = isRequired;
    }

    public ValueInfo[] getUserInfos() {
        return skyVariant_.getUserInfos();
    }

    public List<Class<? extends DomainMapper>> getUserDomains() {
        List<Class<? extends DomainMapper>> list =
            new ArrayList<Class<? extends DomainMapper>>( 2 );
        list.add( null );
        list.add( null );
        return list;
    }

    public StorageType getStorageType() {
        return storageType_;
    }

    public boolean isRequired() {
        return isRequired_;
    }

    /**
     * Reads a sky vector value from an appropriate column in the current row
     * of a given TupleSequence.
     *
     * @param   tseq  sequence positioned at a row
     * @param  icol   index of column in sequence corresponding to this Coord
     * @param  v3   3-element vector into which the (x,y,z) sky position
     *              will be written
     * @return   true iff a valid position has been successfully read
     */
    public abstract boolean readSkyCoord( TupleSequence tseq, int icol,
                                          double[] v3 );

    /**
     * Factory method to create an instance of this class.
     *
     * @param  hasRadius   true for volume, false for unit sphere surface
     * @param   isRequired  true if this coordinate is required for plotting
     */
    public static SkyCoord createCoord( boolean hasRadius,
                                        boolean isRequired ) {
        SkyVariant variant = hasRadius ? SkyVariant.VOLUME
                                       : SkyVariant.SURFACE;
        if ( PlotUtil.storeFullPrecision() ) {
            return new DoubleSkyCoord( variant, isRequired );
        }
        else if ( hasRadius ) {
            return new FloatSkyCoord( variant, isRequired );
        }
        else {
            return new IntegerSkyCoord( variant, isRequired );
        }
    }

    /**
     * Converts a longitude, latitude pair into a normalised 3-vector.
     * If the input values are out of range, the components of the
     * returned vector will all be NaN.
     *
     * @param   lonDeg  longitude in degrees
     * @param   latDeg  latitude in degrees
     * @return  (x,y,z) array of components of a normalised vector
     */
    public static double[] lonLatDegreesToDouble3( double lonDeg,
                                                   double latDeg ) {
        if ( latDeg >= -90 && latDeg <= +90 ) {
            double theta = Math.toRadians( 90 - latDeg );
            double phi = Math.toRadians( lonDeg % 360. );
            double z = Math.cos( theta );
            double sd = Math.sin( theta );
            double x = Math.cos( phi ) * sd;
            double y = Math.sin( phi ) * sd;
            return new double[] { x, y, z };
        }
        else {
            return NO_SKY;
        }
    }

    /**
     * SkyCoord implementation that uses a triple of double precision values.
     */
    private static class DoubleSkyCoord extends SkyCoord {

        private final SkyVariant variant_;

        /**
         * Constructor.
         *
         * @param   variant  surface or volume
         * @param   isRequired  true if this coordinate is required for plotting
         */
        DoubleSkyCoord( SkyVariant variant, boolean isRequired ) {
            super( variant, StorageType.DOUBLE3, isRequired );
            variant_ = variant;
        }

        public Object userToStorage( Object[] userCoords,
                                     DomainMapper[] mappers ) {
            return variant_.userToDouble3( userCoords );
        }

        public boolean readSkyCoord( TupleSequence tseq, int icol,
                                     double[] v3 ) {
            double[] dval = (double[]) tseq.getObjectValue( icol );
            if ( Double.isNaN( dval[ 0 ] ) ) {
                return false;
            }
            else {
                v3[ 0 ] = dval[ 0 ];
                v3[ 1 ] = dval[ 1 ];
                v3[ 2 ] = dval[ 2 ];
                return true;
            }
        }
    }

    /**
     * SkyCoord implementation that uses a triple of float values.
     */
    private static class FloatSkyCoord extends SkyCoord {

        private final SkyVariant variant_;

        /**
         * Constructor.
         *
         * @param   variant  surface or volume
         * @param   isRequired  true if this coordinate is required for plotting
         */
        FloatSkyCoord( SkyVariant variant, boolean isRequired ) {
            super( variant, StorageType.FLOAT3, isRequired );
            variant_ = variant;
        }

        public Object userToStorage( Object[] userCoords,
                                     DomainMapper[] mappers ) {
            double[] d3 = variant_.userToDouble3( userCoords );
            return new float[] {
                (float) d3[ 0 ],
                (float) d3[ 1 ],
                (float) d3[ 2 ],
            };
        }

        public boolean readSkyCoord( TupleSequence tseq, int icol,
                                     double[] v3 ) {
            float[] fval = (float[]) tseq.getObjectValue( icol );
            if ( Float.isNaN( fval[ 0 ] ) ) {
                return false;
            }
            else {
                v3[ 0 ] = fval[ 0 ];
                v3[ 1 ] = fval[ 1 ];
                v3[ 2 ] = fval[ 2 ];
                return true;
            }
        }
    }

    /**
     * SkyCoord implementation that uses a triple of int values.
     * This fixed-point representation is a reasonable way to do it
     * if the length of the vectors is fixed.
     */
    private static class IntegerSkyCoord extends SkyCoord {

        private final SkyVariant variant_;
        private static final double SCALE = - (double) Integer.MIN_VALUE;
        private static final double SCALE1 = 1.0 / SCALE;

        /**
         * Constructor.
         *
         * @param   variant  surface or volume
         * @param   isRequired  true if this coordinate is required for plotting
         */
        IntegerSkyCoord( SkyVariant variant, boolean isRequired ) {
            super( variant, StorageType.INT3, isRequired );
            variant_ = variant;
        }

        public Object userToStorage( Object[] userCoords,
                                     DomainMapper[] mappers ) {
            double[] v3 = variant_.userToDouble3( userCoords );
            if ( v3 == NO_SKY ) {
                return ZERO3;
            }
            else {
                /* out of range values are just pinned at
                 * Integer.MIN/MAX_VALUE, which is OK -
                 * a tiny error from 2^31-1 ~= 2^31 is acceptable. */
                return new int[] {
                    (int) ( SCALE * v3[ 0 ] ),
                    (int) ( SCALE * v3[ 1 ] ),
                    (int) ( SCALE * v3[ 2 ] ),
                };
            }
        }

        public boolean readSkyCoord( TupleSequence tseq, int icol,
                                     double[] v3 ) {
            int[] ival = (int[]) tseq.getObjectValue( icol );
            int ix = ival[ 0 ];
            int iy = ival[ 1 ];
            int iz = ival[ 2 ];
            if ( ix == 0 && iy == 0 && iz == 0 ) {
                return false;
            }
            else {
                v3[ 0 ] = ix * SCALE1;
                v3[ 1 ] = iy * SCALE1;
                v3[ 2 ] = iz * SCALE1;
                return true;
            }
        }
    }

    /**
     * Controls the interpretation of sky vectors, either fixed to the 
     * surface of the unit sphere or not.
     */
    private static abstract class SkyVariant {

        /** Vectors are all normalised to modulus 1, on unit sphere surface. */
        public static SkyVariant SURFACE = createSkyVariant( false );

        /** No constraints on vector modulus. */
        public static SkyVariant VOLUME = createSkyVariant( true );

        /**
         * Returns the user-directed metadata for value acquisition.
         *
         * @return  one metadata item for each user coordinate
         */
        abstract ValueInfo[] getUserInfos();

        /**
         * Converts user coordinate array to storage coordinate array.
         *
         * @param   userCoords  object array corresponding to user infos
         * @return  3-element storage coordinate array (x,y,z)
         */
        abstract double[] userToDouble3( Object[] userCoords );

        /**
         * Converts an object to a double value.
         *
         * @param   val  value object
         * @return  numeric value, may be NaN
         */
        private static double toDouble( Object val ) {
            return val instanceof Number ? ((Number) val).doubleValue()
                                         : Double.NaN;
        }

        /**
         * Returns a new SkyVariant instance.
         *
         * @param  hasRadius  true for volume, false for surface
         */
        private static SkyVariant createSkyVariant( boolean hasRadius ) {
            final DefaultValueInfo lonInfo =
                new DefaultValueInfo( "Lon", Number.class, "Longitude" );
            final DefaultValueInfo latInfo =
                new DefaultValueInfo( "Lat", Number.class, "Latitude" );
            lonInfo.setUnitString( "deg" );
            latInfo.setUnitString( "deg" );
            final DefaultValueInfo radiusInfo =
                new DefaultValueInfo( "Radius", Number.class,
                                      "Radial distance" );
            return hasRadius
                 ? new SkyVariant() {
                       public ValueInfo[] getUserInfos() {
                           return new ValueInfo[] { lonInfo, latInfo,
                                                    radiusInfo };
                       }
                       public double[] userToDouble3( Object[] userCoords ) {
                           double lon = toDouble( userCoords[ 0 ] );
                           double lat = toDouble( userCoords[ 1 ] );
                           double radius = toDouble( userCoords[ 2 ] );
                           double[] vec3 = lonLatDegreesToDouble3( lon, lat );
                           vec3[ 0 ] *= radius;
                           vec3[ 1 ] *= radius;
                           vec3[ 2 ] *= radius;
                           return vec3;
                       }
                   }
                 : new SkyVariant() {
                       public ValueInfo[] getUserInfos() {
                           return new ValueInfo[] { lonInfo, latInfo };
                       }
                       public double[] userToDouble3( Object[] userCoords ) {
                           double lon = toDouble( userCoords[ 0 ] );
                           double lat = toDouble( userCoords[ 1 ] );
                           return lonLatDegreesToDouble3( lon, lat );
                       }
                   };
        }
    }
}
