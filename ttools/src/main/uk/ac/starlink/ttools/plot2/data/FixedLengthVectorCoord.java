package uk.ac.starlink.ttools.plot2.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import uk.ac.starlink.table.Domain;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Coord implementation for a fixed-length array of values.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2025
 */
public abstract class FixedLengthVectorCoord extends SingleCoord {

    /** Domain for 3-vector. */
    public static final VectorDomain FLOATING_VECTOR3_DOMAIN =
        createFloatingVector3Domain();

    /** 3-vector coordinate. */
    public static final FixedLengthVectorCoord XYZ_COORD = createXyzCoord();

    /**
     * Constructor.
     *
     * @param  meta   metadata for single input
     * @param  isRequired   true if this coordinate is required for plotting
     * @param  domain   domain
     * @param  storage   storage type
     */
    protected FixedLengthVectorCoord( InputMeta meta, boolean isRequired,
                                      VectorDomain domain,
                                      StorageType storage ) {
        super( meta, isRequired, domain, storage );
    }

    /**
     * Reads vector elements from a specified column in a given tuple
     * into a supplied array.
     *
     * @param  tuple  tuple
     * @param  icol   index of field in tuple corresponding to this Coord
     * @param  vec    array of at least nel elements to receive the result
     */
    public abstract void readElements( Tuple tuple, int icol, double[] vec );

    /**
     * Creates a 3-element floating point vector coordinate.
     *
     * @param  meta  input value metadata
     * @param  isRequired   true if this coordinate is required for plotting
     * @return  new coordinate
     */
    private static FixedLengthVectorCoord
            createFloatingVector3Coord( InputMeta meta, boolean isRequired ) {
        if ( PlotUtil.storeFullPrecision() ) {
            return new FixedLengthVectorCoord( meta, isRequired,
                                               FLOATING_VECTOR3_DOMAIN,
                                               StorageType.DOUBLE3 ) {
                public Function<Object[],double[]>
                       inputStorage( ValueInfo[] infos, DomainMapper[] dms ) {
                    return doubleInputStorage( infos[ 0 ].getContentClass(),
                                               3 );
                }
                public void readElements( Tuple tuple, int icol,
                                          double[] vec3 ) {
                    double[] dval = (double[]) tuple.getObjectValue( icol );
                    vec3[ 0 ] = dval[ 0 ];
                    vec3[ 1 ] = dval[ 1 ];
                    vec3[ 2 ] = dval[ 2 ];
                }
            };
        }
        else {
            return new FixedLengthVectorCoord( meta, isRequired,
                                               FLOATING_VECTOR3_DOMAIN,
                                               StorageType.FLOAT3 ) {
                public Function<Object[],float[]>
                       inputStorage( ValueInfo[] infos, DomainMapper[] dms ) {
                    return floatInputStorage( infos[ 0 ].getContentClass(),
                                              3 );
                }
                public void readElements( Tuple tuple, int icol,
                                          double[] vec3 ) {
                    float[] fval = (float[]) tuple.getObjectValue( icol );
                    vec3[ 0 ] = fval[ 0 ];
                    vec3[ 1 ] = fval[ 1 ];
                    vec3[ 2 ] = fval[ 2 ];
                }
            };
        }
    }

    /**
     * Creates a coord instance for 3-element floating point XYZ vectors.
     *
     * @return  new coord
     */
    private static FixedLengthVectorCoord createXyzCoord() {
        InputMeta meta = new InputMeta( "xyz", "XYZ Vector" );
        meta.setShortDescription( "3-element Cartesian component array" );
        meta.setValueUsage( "array" );
        meta.setXmlDescription( new String[] {
            "<p>3-element array giving the X, Y and Z components",
            "of a point in 3-d space.",
            "If an array longer than 3 elements is supplied,",
            "the extra elements are ignored.",
            "</p>",
        } );
        boolean isRequired = true;
        return createFloatingVector3Coord( meta, isRequired );
    }

    /**
     * Creates a domain suitable for a floating point 3-vector.
     *
     * @return  new domain
     */
    private static VectorDomain createFloatingVector3Domain() {
        return new VectorDomain( 3, new Class<?>[] {
            double[].class,
            float[].class,
            long[].class,
            int[].class,
            short[].class,
            byte[].class,
        } );
    }

    /**
     * Provides a function to turn a quantity in the user view
     * to a fixed-length double array value.
     *
     * @param  clazz  class of user view object;
     *                any numeric primitive array type will lead to a
     *                usable storage function
     * @param  nel   fixed length of output double array
     * @return  storage conversion function, or null
     */
    private static Function<Object[],double[]>
                   doubleInputStorage( Class<?> clazz, int nel ) {
        double[] blank = new double[ nel ];
        for ( int i = 0; i < nel; i++ ) {
            blank[ i ] = Double.NaN;
        }
        if ( double[].class.equals( clazz ) ) {
            return values -> {
                Object array = values[ 0 ];
                if ( array instanceof double[] ) {
                    double[] darray = (double[]) array;
                    if ( darray.length == nel ) {
                        return darray;
                    }
                    else if ( darray.length > nel ) {
                        double[] dv = new double[ nel ];
                        for ( int i = 0; i < nel; i++ ) {
                            dv[ i ] = darray[ i ];
                        }
                        return dv;
                    }
                    else {
                        return blank;
                    }
                }
                else {
                    return blank;
                }
            };
        }
        else if ( float[].class.equals( clazz ) ) {
            return values -> {
                Object array = values[ 0 ];
                if ( array instanceof float[] ) {
                    float[] farray = (float[]) array;
                    if ( farray.length >= nel ) {
                        double[] dv = new double[ nel ];
                        for ( int i = 0; i < nel; i++ ) {
                            dv[ i ] = farray[ i ];
                        }
                        return dv;
                    }
                    else {
                        return blank;
                    }
                }
                else {
                    return blank;
                }
            };
        }
        else if ( long[].class.equals( clazz ) ) {
            return values -> {
                Object array = values[ 0 ];
                if ( array instanceof long[] ) {
                    long[] larray = (long[]) array;
                    if ( larray.length >= nel ) {
                        double[] dv = new double[ nel ];
                        for ( int i = 0; i < nel; i++ ) {
                            dv[ i ] = (double) larray[ i ];
                        }
                        return dv;
                    }
                    else {
                        return blank;
                    }
                }
                else {
                    return blank;
                }
            };
        }
        else if ( int[].class.equals( clazz ) ) {
            return values -> {
                Object array = values[ 0 ];
                if ( array instanceof int[] ) {
                    int[] iarray = (int[]) array;
                    if ( iarray.length >= nel ) {
                        double[] dv = new double[ nel ];
                        for ( int i = 0; i < nel; i++ ) {
                            dv[ i ] = (double) iarray[ i ];
                        }
                        return dv;
                    }
                    else {
                        return blank;
                    }
                }
                else {
                    return blank;
                }
            };
        }
        else if ( short[].class.equals( clazz ) ) {
            return values -> {
                Object array = values[ 0 ];
                if ( array instanceof short[] ) {
                    short[] sarray = (short[]) array;
                    if ( sarray.length >= nel ) {
                        double[] dv = new double[ nel ];
                        for ( int i = 0; i < nel; i++ ) {
                            dv[ i ] = sarray[ i ];
                        }
                        return dv;
                    }
                    else {
                        return blank;
                    }
                }
                else {
                    return blank;
                }
            };
        }
        else if ( byte[].class.equals( clazz ) ) {
            return values -> {
                Object array = values[ 0 ];
                if ( array instanceof byte[] ) {
                    byte[] barray = (byte[]) array;
                    if ( barray.length >= nel ) {
                        double[] dv = new double[ nel ];
                        for ( int i = 0; i < nel; i++ ) {
                            dv[ i ] = barray[ i ];
                        }
                        return dv;
                    }
                    else {
                        return blank;
                    }
                }
                else {
                    return blank;
                }
            };
        }
        else {
            return null;
        }
    }

    /**
     * Provides a function to turn a quantity in the user view
     * to a fixed-length float array value.
     *
     * @param  clazz  class of user view object;
     *                any numeric primitive array type will lead to a
     *                usable storage function
     * @param  nel   fixed length of output float array
     * @return  storage conversion function, or null
     */
    private static Function<Object[],float[]>
                   floatInputStorage( Class<?> clazz, int nel ) {
        float[] blank = new float[ nel ];
        for ( int i = 0; i < nel; i++ ) {
            blank[ i ] = Float.NaN;
        }
        if ( float[].class.equals( clazz ) ) {
            return values -> {
                Object array = values[ 0 ];
                if ( array instanceof float[] ) {
                    float[] farray = (float[]) array;
                    if ( farray.length == nel ) {
                        return farray;
                    }
                    else if ( farray.length > nel ) {
                        float[] fv = new float[ nel ];
                        for ( int i = 0; i < nel; i++ ) {
                            fv[ i ] = farray[ i ];
                        }
                        return fv;
                    }
                    else {
                        return blank;
                    }
                }
                else {
                    return blank;
                }
            };
        }
        else if ( double[].class.equals( clazz ) ) {
            return values -> {
                Object array = values[ 0 ];
                if ( array instanceof double[] ) {
                    double[] darray = (double[]) array;
                    if ( darray.length >= nel ) {
                        float[] fv = new float[ nel ];
                        for ( int i = 0; i < nel; i++ ) {
                            fv[ i ] = (float) darray[ i ];
                        }
                        return fv;
                    }
                    else {
                        return blank;
                    }
                }
                else {
                    return blank;
                }
            };
        }
        else if ( long[].class.equals( clazz ) ) {
            return values -> {
                Object array = values[ 0 ];
                if ( array instanceof long[] ) {
                    long[] larray = (long[]) array;
                    if ( larray.length >= nel ) {
                        float[] fv = new float[ nel ];
                        for ( int i = 0; i < nel; i++ ) {
                            fv[ i ] = (float) larray[ i ];
                        }
                        return fv;
                    }
                    else {
                        return blank;
                    }
                }
                else {
                    return blank;
                }
            };
        }
        else if ( int[].class.equals( clazz ) ) {
            return values -> {
                Object array = values[ 0 ];
                if ( array instanceof int[] ) {
                    int[] iarray = (int[]) array;
                    if ( iarray.length >= nel ) {
                        float[] fv = new float[ nel ];
                        for ( int i = 0; i < nel; i++ ) {
                            fv[ i ] = iarray[ i ];
                        }
                        return fv;
                    }
                    else {
                        return blank;
                    }
                }
                else {
                    return blank;
                }
            };
        }
        else if ( short[].class.equals( clazz ) ) {
            return values -> {
                Object array = values[ 0 ];
                if ( array instanceof short[] ) {
                    short[] sarray = (short[]) array;
                    if ( sarray.length >= nel ) {
                        float[] fv = new float[ nel ];
                        for ( int i = 0; i < nel; i++ ) {
                            fv[ i ] = sarray[ i ];
                        }
                        return fv;
                    }
                    else {
                        return blank;
                    }
                }
                else {
                    return blank;
                }
            };
        }
        else if ( byte[].class.equals( clazz ) ) {
            return values -> {
                Object array = values[ 0 ];
                if ( array instanceof byte[] ) {
                    byte[] barray = (byte[]) array;
                    if ( barray.length >= nel ) {
                        float[] fv = new float[ nel ];
                        for ( int i = 0; i < nel; i++ ) {
                            fv[ i ] = barray[ i ];
                        }
                        return fv;
                    }
                    else {
                        return blank;
                    }
                }
                else {
                    return blank;
                }
            };
        }
        else {
            return values -> blank;
        }
    }

    /**
     * Domain for use with FixedLengthVectorCoord.
     * Note it is its own sole mapper, so it implements both
     * the Domain and DomainMapper interfaces.
     */
    public static class VectorDomain
            implements Domain<VectorDomain>, DomainMapper {

        private final int nel_;
        private final Collection<Class<?>> userClasses_;
        private final VectorDomain mapper_;
        private final VectorDomain domain_;

        /**
         * Constructor.
         *
         * @param   nel  fixed number of elements in array
         * @param  userClasses  classes of user object that can be handled
         *                      int this domain
         */
        public VectorDomain( int nel, Class<?>[] userClasses ) {
            nel_ = nel;
            userClasses_ = Arrays.asList( userClasses );
            mapper_ = this;
            domain_ = this;
        }

        public String getDomainName() {
            return nel_ + "-vector";
        }

        public VectorDomain[] getMappers() {
            return new VectorDomain[] { mapper_ };
        }

        public VectorDomain getProbableMapper( ValueInfo info ) {
            int[] shape = info.getShape();
            if ( shape != null && shape.length == 1 && shape[ 0 ] == nel_ &&
                 userClasses_.contains( info.getContentClass() ) ) {
                return mapper_;
            }
            else {
                return null;
            }
        }

        public VectorDomain getPossibleMapper( ValueInfo info ) {
            if ( userClasses_.contains( info.getContentClass() ) ) {
                int[] shape = info.getShape();
                if ( shape != null && shape.length == 1 ) {
                    if ( shape[ 0 ] < 0 || shape[ 0 ] >= nel_ ) {
                        return mapper_;
                    }
                    else {
                        return null;
                    }
                }
                else {
                    return mapper_;
                }
            }
            else {
                return null;
            }
        }

        public VectorDomain getTargetDomain() {
            return domain_;
        }

        public Class<?> getSourceClass() {
            return Object.class;
        }

        public String getSourceName() {
            return nel_ + "-vector";
        }

        public String getSourceDescription() {
            return nel_ + "-element array-valued quantity";
        }

        @Override
        public int hashCode() {
            return nel_;
        }

        @Override
        public boolean equals( Object o ) {
            return o instanceof VectorDomain
                && ((VectorDomain) o).nel_ == this.nel_;
        }
    }
}
