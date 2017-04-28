package uk.ac.starlink.ttools.plot2.data;

import uk.ac.starlink.table.DomainMapper;
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

    public Input[] getInputs() {
        return skyVariant_.getInputs();
    }

    public StorageType getStorageType() {
        return storageType_;
    }

    public boolean isRequired() {
        return isRequired_;
    }

    /**
     * Reads a sky vector value from an appropriate column in the current row
     * of a given Tuple.
     *
     * @param   tuple  tuple
     * @param  icol   index of field in tuple corresponding to this Coord
     * @param  v3   3-element vector into which the (x,y,z) sky position
     *              will be written
     * @return   true iff a valid position has been successfully read
     */
    public abstract boolean readSkyCoord( Tuple tuple, int icol, double[] v3 );

    /**
     * Factory method to create an instance of this class.
     *
     * @param   variant  type of sky coordinates
     * @param   isRequired  true if this coordinate is required for plotting
     */
    public static SkyCoord createCoord( SkyVariant variant,
                                        boolean isRequired ) {
        if ( PlotUtil.storeFullPrecision() ) {
            return new DoubleSkyCoord( variant, isRequired );
        }
        else if ( variant.hasRadius() ) {
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

        public Object inputToStorage( Object[] values,
                                      DomainMapper[] mappers ) {
            return variant_.inputToDouble3( values );
        }

        public boolean readSkyCoord( Tuple tuple, int icol, double[] v3 ) {
            double[] dval = (double[]) tuple.getObjectValue( icol );
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

        public Object inputToStorage( Object[] values,
                                      DomainMapper[] mappers ) {
            double[] d3 = variant_.inputToDouble3( values );
            return new float[] {
                (float) d3[ 0 ],
                (float) d3[ 1 ],
                (float) d3[ 2 ],
            };
        }

        public boolean readSkyCoord( Tuple tuple, int icol, double[] v3 ) {
            float[] fval = (float[]) tuple.getObjectValue( icol );
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

        public Object inputToStorage( Object[] values,
                                      DomainMapper[] mappers ) {
            double[] v3 = variant_.inputToDouble3( values );
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

        public boolean readSkyCoord( Tuple tuple, int icol, double[] v3 ) {
            int[] ival = (int[]) tuple.getObjectValue( icol );
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
    public static abstract class SkyVariant {

        private final boolean hasRadius_;
        final Input lonInput_;
        final Input latInput_;

        /** No radial coordinate, vectors all on unit sphere surface. */
        public static SkyVariant SURFACE = createSurfaceSkyVariant();

        /** Has radial coordinate, point considered blank if radius blank .*/
        public static SkyVariant VOLUME_OR_NULL =
            createVolumeSkyVariant( false );

        /** Has radial coordinate, considered on unit sphere if radius blank. */
        public static SkyVariant VOLUME_OR_UNIT =
            createVolumeSkyVariant( true );

        /**
         * Constructor.
         *
         * @param  hasRadius  true if a radial coordinate is used
         */
        private SkyVariant( boolean hasRadius ) {
            hasRadius_ = hasRadius;
            InputMeta lonMeta =
                new InputMeta( "lon", "Lon" )
               .setShortDescription( "Longitude in decimal degrees" )
               .setValueUsage( "deg" );
            InputMeta latMeta =
                new InputMeta( "lat", "Lat" )
               .setShortDescription( "Latitude in decimal degrees" )
               .setValueUsage( "deg" );
            lonInput_ = new Input( lonMeta, Number.class, null );
            latInput_ = new Input( latMeta, Number.class, null );
        }

        /**
         * Returns the user-directed metadata for value acquisition.
         *
         * @return  one metadata item for each input coordinate
         */
        abstract Input[] getInputs();

        /**
         * Converts input value array to storage coordinate array.
         *
         * @param   inputValues  object array corresponding to inputs
         * @return  3-element storage coordinate array (x,y,z)
         */
        abstract double[] inputToDouble3( Object[] inputValues );

        /**
         * Indicates whether this coord can represent non-unit vectors.
         *
         * @return   true iff this coord has a radial part
         */
        boolean hasRadius() {
            return hasRadius_;
        }

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
         * Converts lon/lat object values into a unit 3-vector.
         *
         * @param  lonObj  Number giving longitude in degrees
         * @param  latObj  Number giving latitude in degrees
         * @return  3-element unit vector
         */
        static double[] getUnitVector( Object lonObj, Object latObj ) {
            return lonLatDegreesToDouble3( toDouble( lonObj ),
                                           toDouble( latObj ) );
        }
    }

    /**
     * Partial SkyVariant implementation intended for positions that can
     * be anywhere in space (not restricted to unit sphere).
     */
    private static abstract class VolumeSkyVariant extends SkyVariant {
        private final Input radiusInput_;
        VolumeSkyVariant() {
            super( true );
            InputMeta meta =
                new InputMeta( "r", "Radius" )
               .setShortDescription( "Radial distance" );
            radiusInput_ = new Input( meta, Number.class, null );
        }
        public Input[] getInputs() {
            return new Input[] { lonInput_, latInput_, radiusInput_ };
        }
        public double[] inputToDouble3( Object[] inputValues ) {
            Object radObj = inputValues[ 2 ];
            if ( radObj instanceof Number ) {
                double radius = ((Number) radObj).doubleValue();
                if ( ! Double.isNaN( radius ) ) {
                    double[] vec3 =
                        getUnitVector( inputValues[ 0 ], inputValues[ 1 ] );
                    vec3[ 0 ] *= radius;
                    vec3[ 1 ] *= radius;
                    vec3[ 2 ] *= radius;
                    return vec3;
                }
            }
            return getNoRadiusVector( inputValues[ 0 ], inputValues[ 1 ] );
        }
 
        /**
         * Returns the 3-vector to be used for a position in which no
         * (non-blank) radius has been supplied.
         *
         * @param  lonObj  Number giving longitude in degrees
         * @param  latObj  Number giving latitude in degrees
         * @return  3-element unit vector
         */
        abstract double[] getNoRadiusVector( Object lonObj, Object latObj );
    }

    /**
     * Returns a SkyVariant for positions restricted to the unit sphere.
     *
     * @return  new skyvariant
     */
    private static SkyVariant createSurfaceSkyVariant() {
        return new SkyVariant( false ) {
            public Input[] getInputs() {
                return new Input[] { lonInput_, latInput_ };
            }
            public double[] inputToDouble3( Object[] values ) {
                return getUnitVector( values[ 0 ], values[ 1 ] );
            }
        };
    }

    /**
     * Returns a SkyVariant for positions in which a radius is supplied.
     *
     * @param   nullRadiusPermitted  if true, a null radius is considered
     *                               equivalent to a unit value
     */
    private static SkyVariant
            createVolumeSkyVariant( boolean nullRadiusPermitted ) {
        return nullRadiusPermitted
             ? new VolumeSkyVariant() {
                   public double[] getNoRadiusVector( Object lonObj,
                                                      Object latObj ) {
                       return getUnitVector( lonObj, latObj );
                   }
               }
             : new VolumeSkyVariant() {
                   public double[] getNoRadiusVector( Object lonObj,
                                                      Object latObj ) {
                       return new double[] { Double.NaN,
                                             Double.NaN,
                                             Double.NaN };
                   }
               };
    }
}
