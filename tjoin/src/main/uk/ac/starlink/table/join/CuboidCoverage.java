package uk.ac.starlink.table.join;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Partial coverage implementation representing an N-dimensional hypercuboid.
 * It is suitable for representing simple contiguous regions in
 * N-dimensional Cartesian space. 
 *
 * <p>Factory methods are provided for concrete instances of this class.
 *
 * @author   Mark Taylor
 * @since    8 Jun 2022
 */
public abstract class CuboidCoverage implements Coverage {

    final int ndim_;
    final double[] mins_;
    final double[] maxs_;

    /**
     * Constructor.  Instance is empty on construction.
     *
     * @param  ndim   dimensionality of region
     */
    @SuppressWarnings("this-escape")
    protected CuboidCoverage( int ndim ) {
        ndim_ = ndim;
        mins_ = new double[ ndim ];
        maxs_ = new double[ ndim ];
        setEmpty();
    }

    public boolean isEmpty() {
        for ( int idim = 0; idim < ndim_; idim++ ) {
            if ( mins_[ idim ] > maxs_[ idim ] ) {
                return true;
            }
        }
        return false;
    }

    public void union( Coverage other ) {
        CuboidCoverage cother = (CuboidCoverage) other;
        for ( int idim = 0; idim < ndim_; idim++ ) {
            mins_[ idim ] = Math.min( mins_[ idim ], cother.mins_[ idim ] );
            maxs_[ idim ] = Math.max( maxs_[ idim ], cother.maxs_[ idim ] );
        }
    }

    public void intersection( Coverage other ) {
        CuboidCoverage cother = (CuboidCoverage) other;
        for ( int idim = 0; idim < ndim_; idim++ ) {
            mins_[ idim ] = Math.max( mins_[ idim ], cother.mins_[ idim ] );
            maxs_[ idim ] = Math.min( maxs_[ idim ], cother.maxs_[ idim ] );
            if ( ! ( mins_[ idim ] <= maxs_[ idim ] ) ) {
                setEmpty();
                return;
            }
        }
    }

    public String coverageText() {
        if ( isEmpty() ) {
            return "none";
        }
        else {
            StringBuffer sbuf = new StringBuffer();
            for ( int i = 0; i < ndim_; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ", " );
                }
                sbuf.append( Float.toString( (float) mins_[ i ] ) )
                    .append( " .. " )
                    .append( Float.toString( (float) maxs_[ i ] ) );
            }
            return sbuf.toString();
        }
    }

    /**
     * Resets the state of this object to represent empty coverage.
     */
    private void setEmpty() {
        for ( int idim = 0; idim < ndim_; idim++ ) {
            mins_[ idim ] = Double.POSITIVE_INFINITY;
            maxs_[ idim ] = Double.NEGATIVE_INFINITY;
        }
        assert isEmpty();
    }

    /**
     * Creates a coverage suitable for a fixed isotropic match error
     * in an N-dimensional Cartesian space,
     * where tuples are simple N-dimensional coordinate vectors.
     *
     * @param   ndim   dimensionality
     * @param   err    maximum separation for match
     * @return   new empty coverage
     */
    public static CuboidCoverage createFixedCartesianCoverage( int ndim,
                                                               double err ) {
         final double[] errs = new double[ ndim ];
         for ( int idim = 0; idim < ndim; idim++ ) {
             errs[ idim ] = err >= 0 ? err : 0;
         }
         return createFixedCartesianCoverage( ndim, errs );
    }

    /**
     * Creates a coverage suitable for a fixed anisotropic match error
     * in an N-dimensional Cartesian space,
     * where tuples are simple N-dimensional coordinate vectors.
     *
     * @param  ndim  dimensionality
     * @param  errs  ndim-dimensional array giving maximum separations along
     *               each axis
     * @return  new empty coverage
     */
    public static CuboidCoverage createFixedCartesianCoverage( int ndim,
                                                               double[] errs ) {
        return createFixedErrorCoverage( ndim, errs,
                                         createCartesianPointDecoder( ndim ) );
    }

    /**
     * Creates a coverage suitable for a fixed anisotropic match error
     * in an N-dimensional coordinate space,
     * with custom tuple-&gt;coordinate mapping.
     *
     * @param  ndim  dimensionality of Cartesian space
     * @param  errors  ndim-dimensional array giving maximum separations along
     *                 each axis
     * @param  pointDecoder  thread-safe converter of tuples into
     *                       N-dimensional Cartesian position vectors
     * @return   new empty coverage
     */
    public static CuboidCoverage createFixedErrorCoverage(
            int ndim, double[] errors, PointDecoder pointDecoder ) {
        final double[] errs = new double[ ndim ];
        for ( int idim = 0; idim < ndim; idim++ ) {
            errs[ idim ] = errors[ idim ] >= 0 ? errors[ idim ] : 0;
        }
        return new CuboidCoverage( ndim ) {
            final double[] point_ = new double[ ndim ];
            public void extend( Object[] tuple ) {
                if ( pointDecoder.decodePoint( tuple, point_ ) ) {
                    for ( int idim = 0; idim < ndim; idim++ ) {
                        double p = point_[ idim ];
                        double err = errs[ idim ];
                        mins_[ idim ] = Math.min( mins_[ idim ], p - err );
                        maxs_[ idim ] = Math.max( maxs_[ idim ], p + err );
                    }
                }
            }
            public Supplier<Predicate<Object[]>> createTestFactory() {
                final double[] mins = mins_.clone();
                final double[] maxs = maxs_.clone();
                return () -> {
                    final double[] point = new double[ ndim ];
                    return tuple -> {
                        if ( pointDecoder.decodePoint( tuple, point ) ) {
                            for ( int idim = 0; idim < ndim; idim++ ) {
                                double p = point[ idim ];
                                if ( ! ( p >= mins[ idim ] &&
                                         p <= maxs[ idim ] )) {
                                    return false;
                                }
                            }
                            return true;
                        }
                        else {
                            return false;
                        }
                    };
                };
            }
        };
    }

    /**
     * Creates a coverage suitable for variable isotropic match errors
     * in an N-dimensional coordinate space.
     *
     * @param  ndim  dimensionality of Cartesian space
     * @param  pointDecoder  thread-safe converter of tuples into
     *                       N-dimensional Cartesian position vectors
     * @param  errorDecoder  thread-safe extractor of isotropic maximum
     *                       match separation from tuples
     * @return  new empty coverage
     */
    public static CuboidCoverage createVariableErrorCoverage(
            int ndim, PointDecoder pointDecoder, ErrorDecoder errorDecoder ) {
        return new CuboidCoverage( ndim ) {
            final double[] point_ = new double[ ndim ];
            public void extend( Object[] tuple ) {
                if ( pointDecoder.decodePoint( tuple, point_ ) ) {
                    double err = errorDecoder.decodeError( tuple );
                    err = err >= 0 ? err : 0;
                    for ( int idim = 0; idim < ndim_; idim++ ) {
                        double p = point_[ idim ];
                        mins_[ idim ] = Math.min( mins_[ idim ], p - err );
                        maxs_[ idim ] = Math.max( maxs_[ idim ], p + err );
                    }
                }
            }
            public Supplier<Predicate<Object[]>> createTestFactory() {
                final double[] mins = mins_.clone();
                final double[] maxs = maxs_.clone();
                return () -> {
                    final double[] point = new double[ ndim ];
                    return tuple -> {
                        if ( pointDecoder.decodePoint( tuple, point ) ) {
                            double err = errorDecoder.decodeError( tuple );
                            err = err >= 0 ? err : 0;
                            for ( int idim = 0; idim < ndim; idim++ ) {
                                double p = point[ idim ];
                                if ( ! ( p + err >= mins[ idim ] &&
                                         p - err <= maxs[ idim ] ) ) {
                                    return false;
                                }
                            }
                            return true;
                        }
                        else {
                            return false;
                        }
                    };
                };
            }
        };
    }

    /**
     * Returns a PointDecoder for simple Cartesian tuples.
     *
     * @param  ndim  dimensionality of tuples and output vectors
     * @return  new point decoder
     */
    public static PointDecoder createCartesianPointDecoder( final int ndim ) {
        return ( tuple, point ) -> {
            for ( int idim = 0; idim < ndim; idim++ ) {
                Object val = tuple[ idim ];
                if ( val instanceof Number ) {
                    double dval = ((Number) val).doubleValue();
                    if ( Double.isFinite( dval ) ) {
                        point[ idim ] = dval;
                    }
                    else {
                        return false;
                    }
                }
                else {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Defines mapping of tuple to Cartesian position.
     */
    @FunctionalInterface
    public interface PointDecoder {

        /**
         * Converts a tuple to a Cartesian vector.
         * The result is written into a supplied workspace array.
         *
         * <p>Note this method must be thread-safe, it may be called
         * from multiple threads concurrently.
         *
         * @param  tuple  tuple data
         * @param  pos   n-dimensional workspace for output
         * @return  true on success
         */
        boolean decodePoint( Object[] tuple, double[] pos );
    }

    /**
     * Defines mapping of tuple to error value.
     */
    @FunctionalInterface
    public interface ErrorDecoder {

        /**
         * Returns the error value corresponding to a supplied tuple.
         *
         * <p>Note this method must be thread-safe, it may be called
         * from multiple threads concurrently.
         *
         * @param  tuple  tuple data
         * @return  maximum separation for match
         */
        double decodeError( Object[] tuple );
    }
}
