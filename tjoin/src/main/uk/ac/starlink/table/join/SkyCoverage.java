package uk.ac.starlink.table.join;

import cds.healpix.HashComputer;
import cds.healpix.Healpix;
import cds.healpix.HealpixNested;
import cds.healpix.HealpixNestedBMOC;
import cds.healpix.HealpixNestedFixedRadiusCone4XMatch;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Partial coverage implementation for use on the celestial sphere.
 * It makes use of the HEALPix tesselation.
 * Storage of HEALPix coverage information is handled by a supplied
 * mask object.
 *
 * <p>Factory methods are provided for concrete instances of this class.
 *
 * @author   Mark Taylor
 * @since    8 Jun 2022
 */
public abstract class SkyCoverage implements Coverage {

    private final HealpixMask mask_;
    private final int txtOrder_;

    /**
     * Constructor using a custom healpix mask implementation.
     *
     * @param   mask  mask implementation
     */
    protected SkyCoverage( HealpixMask mask ) {
        mask_ = mask;
        txtOrder_ = 1;
    }

    /**
     * Constructor using an empty default healpix mask implementation.
     */
    protected SkyCoverage() {
        this( new BitsetMask( BitsetMask.DEFAULT_ORDER ) );
    }

    /**
     * Returns the HEALPix mask implementation used by this object.
     *
     * @return  mask
     */
    public HealpixMask getMask() {
        return mask_;
    }

    public boolean isEmpty() {
        return mask_.isEmpty();
    }

    public void intersection( Coverage other ) {
        mask_.intersection( ((SkyCoverage) other).mask_ );
    }

    public void union( Coverage other ) {
        mask_.union( ((SkyCoverage) other).mask_ );
    }

    public String coverageText() {
        double fraction = mask_.getSkyFraction();
        if ( fraction == 0 ) {
            return "none";
        }
        else if ( fraction == 1.0 ) {
            return "All sky";
        }
        else {
            return new StringBuffer()
                .append( (float) fraction )
                .append( " of sky (HEALPix " )
                .append( txtOrder_ )
                .append( ": " )
                .append( hexMask( txtOrder_, mask_.createPixelTester() ) )
                .append( ")" )
                .toString();
        }
    }

    /**
     * Creates a sky coverage suitable for a fixed match radius.
     *
     * @param  errRad  match error in radians
     * @param  posDecoder   thread-safe converter from tuples to sky position;
     *                      output is to (longitude, latitude) in radians
     * @return  new empty coverage
     */
    public static SkyCoverage
            createFixedErrorCoverage( double errRad, TupleDecoder posDecoder ) {

        /* The depth must be at most this value for the
         * HealpixNestedFixedRadiusCone4XMatch to work
         * (to ensure that a maximum of 9 pixels are overlapping). */
        int maxDepth = Healpix.getBestStartingDepth( errRad );

        /* Since the coverage is only intended to be a rough aid for filtering,
         * the resolution is not that important.  Reduce the depth by one,
         * which should mean most calls only return a single pixel,
         * and reduce the work that the mask object has to do. */
        final int depth = maxDepth - 1;
        final HealpixNested healpixNested = Healpix.getNested( depth );
        final HealpixNestedFixedRadiusCone4XMatch coneComputer =
            healpixNested.newConeComputer4Xmatch( errRad );
        return new SkyCoverage() {
            final HealpixMask mask_ = getMask();
            final double[] lonlat_ = new double[ 2 ];
            final long[] cells_ = new long[ 9 ];
            public void extend( Object[] tuple ) {
                if ( posDecoder.decodeTuple( tuple, lonlat_ ) ) {
                    int ncell = coneComputer
                               .overlappingCells( lonlat_[ 0 ], lonlat_[ 1 ],
                                                  cells_ );
                    for ( int icell = 0; icell < ncell; icell++ ) {
                        mask_.addPixel( depth, cells_[ icell ] );
                    }
                }
            }
            public Supplier<Predicate<Object[]>> createTestFactory() {
                final HealpixMask.PixelTester pixTester =
                    mask_.createPixelTester();
                return () -> {
                    final double[] lonlat = new double[ 2 ];
                    final HashComputer hashComputer =
                        healpixNested.newHashComputer();
                    return tuple -> {
                        if ( posDecoder.decodeTuple( tuple, lonlat ) ) {
                            long hash = hashComputer.hash( lonlat[ 0 ],
                                                           lonlat[ 1 ] );
                            return pixTester.containsPixel( depth, hash );
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
     * Creates a sky coverage suitable for a variable match radius.
     *
     * @param  scaleRad  characteristic scale of errors in radians
     *                   (tuning parameter)
     * @param  coneDecoder  thread-safe converter from tuples to sky region;
     *                      output is to (longitude, latitude, radius) 
     *                      in radians
     * @return   new empty coverage
     */
    public static SkyCoverage
            createVariableErrorCoverage( double scaleRad,
                                         TupleDecoder coneDecoder ) {
        final int depth = Healpix.getBestStartingDepth( scaleRad );
        final HealpixNested healpixNested = Healpix.getNested( depth );
        return new SkyCoverage() {
            final HealpixMask mask_ = getMask();
            final double[] lonLatErr_ = new double[ 3 ];
            public void extend( Object[] tuple ) {
                HealpixNestedBMOC bmoc = calculateBmoc( tuple, lonLatErr_ );
                if ( bmoc != null ) {
                    for ( HealpixNestedBMOC.CurrentValueAccessor acc : bmoc ) {
                        mask_.addPixel( acc.getDepth(), acc.getHash() );
                    }
                }
            }
            public Supplier<Predicate<Object[]>> createTestFactory() {
                final HealpixMask.PixelTester pixTester =
                    mask_.createPixelTester();
                return () -> {
                    final double[] lonLatErr = new double[ 3 ];
                    return tuple -> {
                        HealpixNestedBMOC bmoc =
                            calculateBmoc( tuple, lonLatErr );
                        if ( bmoc != null ) {
                            for ( HealpixNestedBMOC.CurrentValueAccessor acc :
                                  bmoc ) {
                                if ( pixTester.containsPixel( acc.getDepth(),
                                                              acc.getHash() ) ){
                                    return true;
                                }
                            }
                            return false;
                        }
                        else {
                            return false;
                        }
                    };
                };
            }
            private HealpixNestedBMOC calculateBmoc( Object[] tuple,
                                                     double[] lonLatErr ) {
                if ( coneDecoder.decodeTuple( tuple, lonLatErr ) ) {
                    double lon = lonLatErr[ 0 ];
                    double lat = lonLatErr[ 1 ];
                    double err = lonLatErr[ 2 ];
                    return healpixNested
                          .newConeComputerApprox( err >= 0 ? err : 0 )
                          .overlappingCells( lon, lat );
                }
                else {
                    return null;
                }
            }
        };
    }

    /**
     * Returns a compact hexadecimal representation of the pixels covered
     * by a healpix mask at a given order.
     *
     * @param  order  healpix order governing output resolution
     * @param  tester  pixel inclusion mask
     * @return  compact, somewhat human readable, string
     */
    private static String hexMask( int order, HealpixMask.PixelTester tester ) {
        long nbit = 12 << 2 * order;
        StringBuffer sbuf = new StringBuffer( 3 + (int) ( nbit / 4 ) );
        for ( long i = 0; i < nbit; i += 4 ) {
            if ( i > 0 && i % ( nbit / 3 ) == 0 ) {
                sbuf.append( ' ' );
            }
            int digit = ( tester.containsPixel( order, i + 0 ) ? 8 : 0 )
                      + ( tester.containsPixel( order, i + 1 ) ? 4 : 0 )
                      + ( tester.containsPixel( order, i + 2 ) ? 2 : 0 )
                      + ( tester.containsPixel( order, i + 3 ) ? 1 : 0 );
            sbuf.append( Integer.toHexString( digit ) );
        }
        return sbuf.toString();
    }

    /**
     * Defines mapping a tuple to sky positional information.
     */
    @FunctionalInterface
    public interface TupleDecoder {

        /**
         * Extracts a sky coordinate information from a tuple.
         * The result is written into a supplied workspace array.
         *
         * <p>Note this method must be thread-safe, it may be called
         * from multiple threads concurrently.
         *
         * @param  tuple  input tuple data
         * @param  out  workspace array into which positional information
         *              will be written on successful output
         * @return  true on success
         */
        boolean decodeTuple( Object[] tuple, double[] out );
    }
}
