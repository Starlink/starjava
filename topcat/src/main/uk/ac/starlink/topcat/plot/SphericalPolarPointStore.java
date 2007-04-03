package uk.ac.starlink.topcat.plot;

import uk.ac.starlink.table.ValueStore;
import uk.ac.starlink.table.storage.ArrayPrimitiveStore;

/**
 * PointStore implementation for storing spherical polar data.
 * In fact currently the data points are stored in Cartesian coordinates 
 * (X,Y,Z), since spherical decoding is taken care of by the 
 * PointSelection object (probably saving time on replots)
 * but the errors are stored as radial and tangential deltas
 * (since storing the error points would take a lot of extra space).
 *
 * <p>The error points returned by {@link #getErrors} consist of a
 * 1- (radial only), 2- (tangential only) or 3- (tangential followed by
 * radial) pair array of 3-d coordinate arrays.  Each pair is a lower 
 * bound followed by an upper bound along the relevant dimension.
 *
 * @author   Mark Taylor
 * @since    3 Apr 2007
 */
public class SphericalPolarPointStore implements PointStore {

    private final ValueStore valueStore_;
    private final boolean hasTanerr_;
    private final int npoint_;
    private final int nword_;
    private final ErrorReader radialReader_;
    private final double[] linebuf_;
    private final double[] point_;
    private int ipoint_;

    /**
     * Constructor.
     *
     * @param   radialMode  type of radial error information to store
     * @param   hasTanerr   whether to store tangential error information
     * @param   npoint  number of points to store
     */
    public SphericalPolarPointStore( ErrorMode radialMode, boolean hasTanerr,
                                     int npoint ) {
        radialReader_ = getErrorReader( radialMode );
        hasTanerr_ = hasTanerr;
        npoint_ = npoint;

        /* Calculate the number of words required for each data point. */
        nword_ = 3 + ( hasTanerr_ ? 1 : 0 ) + radialReader_.getWordCount();

        /* Initialise some buffers. */
        linebuf_ = new double[ nword_ ];
        point_ = new double[ 3 ];

        /* Initialise the value store which will hold the values.
         * Other implementations are possible. */
        valueStore_ = new ArrayPrimitiveStore( double.class, nword_ * npoint_ );
        assert double.class.equals( valueStore_.getType() );
    }

    public void storePoint( Object[] coordRow, Object[] errorRow ) {
        int iw = 0;
        for ( int i = 0; i < 3; i++ ) {
            linebuf_[ iw++ ] = CartesianPointStore.doubleValue( coordRow[ i ] );
        }
        for ( int i = 0; i < nword_ - 3; i++ ) {
            linebuf_[ iw++ ] = CartesianPointStore.doubleValue( errorRow[ i ] );
        }
        valueStore_.put( ipoint_++ * (long) nword_, linebuf_ );
    }

    public int getCount() {
        return npoint_;
    }

    public int getNdim() {
        return 3;
    }

    public double[] getPoint( int ipoint ) {
        valueStore_.get( ipoint * (long) nword_, point_ );
        return point_;
    }

    public int getNerror() {
        return 0;
    }

    public double[][] getErrors( int ipoint ) {
        return new double[ 0 ][];
    }

    /**
     * Returns an ErrorReader object suitable for a given ErrorMode.
     *
     * @param  mode  error mode
     * @return   error reader
     */
    private static ErrorReader getErrorReader( ErrorMode mode ) {
        if ( ErrorMode.SYMMETRIC.equals( mode ) ) {
            return new ErrorReader( 1 ) {
                protected void convert( double[] rawErrors, double[] deltas ) {
                    deltas[ 0 ] = rawErrors[ 0 ];
                    deltas[ 1 ] = rawErrors[ 0 ];
                }
            };
        }
        else if ( ErrorMode.LOWER.equals( mode ) ) {
            return new ErrorReader( 1 ) {
                protected void convert( double[] rawErrors, double[] deltas ) {
                    deltas[ 0 ] = rawErrors[ 0 ];
                    deltas[ 1 ] = 0.0;
                }
            };
        }
        else if ( ErrorMode.UPPER.equals( mode ) ) {
            return new ErrorReader( 1 ) {
                protected void convert( double[] rawErrors, double[] deltas ) {
                    deltas[ 0 ] = 0.0;
                    deltas[ 1 ] = rawErrors[ 0 ];
                }
            };
        }
        else if ( ErrorMode.BOTH.equals( mode ) ) {
            return new ErrorReader( 2 ) {
                protected void convert( double[] rawErrors, double[] deltas ) {
                    deltas[ 0 ] = rawErrors[ 0 ];
                    deltas[ 1 ] = rawErrors[ 1 ];
                }
            };
        }
        else {
            assert ErrorMode.NONE.equals( mode );
            return new ErrorReader( 0 ) {
                public void readErrors( ValueStore store, int off,
                                        double[] deltas ) {
                    deltas[ 0 ] = 0.0;
                    deltas[ 1 ] = 0.0;
                }
                protected void convert( double[] rawErrors, double[] deltas ) {
                    assert false;
                }
            };
        }
    }

    /**
     * Helper class which decodes error information from the ValueStore.
     * When it comes time to read error information from the store,
     * the reader is pointed at the offset into it at which the error
     * information is found, and then reads some number of values to
     * determine the upper and lower errors.
     */
    private static abstract class ErrorReader {
        private final double[] buf_;

        /**
         * Constructor.
         *
         * @param   wordCount  number of words from the value store which
         *          are used to store error information for each datum
         */
        public ErrorReader( int wordCount ) {
            buf_ = new double[ wordCount ];
        }

        /**
         * Returns the number of words in the value store which are used
         * to store error information for each datum.
         *
         * @return   word count
         */
        public int getWordCount() {
            return buf_.length;
        }

        /**
         * Reads error lower and upper deltas from the value store into
         * a 2-element array.
         *
         * @param  store  value store
         * @param  off  offset into store at which error information starts
         * @param  deltas  2-element array which on exit holds (lower,upper)
         *         error deltas; these are non-negative and 0 is equivalent 
         *         to no error information
         */
        public void readErrors( ValueStore store, int off, double[] deltas ) {
            store.get( off, buf_ );
            convert( buf_, deltas );
        }

        /**
         * Converts values read from the value store into lower and upper
         * error delta values.
         *
         * @param  rawErrors  nWord-element array read from value store
         * @param  deltas     2-element array which on exit holds (lower,upper)
         *         error deltas; these are non-negative and 0 is equivalent
         *         to no error information
         */
        protected abstract void convert( double[] rawErrors, double[] deltas );
    }
}
