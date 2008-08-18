package uk.ac.starlink.topcat.plot;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import uk.ac.starlink.table.ValueStore;
import uk.ac.starlink.table.storage.ArrayPrimitiveStore;
import uk.ac.starlink.ttools.plot.ErrorMode;

/**
 * PointStore implementation used for storing coordinate information about
 * from Cartesian point selection.
 * A {@link uk.ac.starlink.table.ValueStore} is used to back the storage.
 *
 * <p>The error points returned by {@link #getErrors} are an array with 
 * two elements (N-d coordinate arrays) for each non-blank element 
 * of the ErrorMode array.  Each pair is a lower bound followed by an
 * upper bound along that dimension.
 *
 * @author   Mark Taylor
 * @since    29 Mar 2007
 */
public class CartesianPointStore implements PointStore {

    private final int ndim_;
    private final ErrorReader[] errorReaders_;
    private final int nword_;
    private final long lnword_;
    private final int nerrWord_;
    private final double[] point_;
    private final double[] centre_;
    private final double[][] errors_;
    private final double[] buf1_;
    private final ValueStore valueStore_;
    private final int npoint_;
    private int ipoint_;

    private static final double MILLISECONDS_PER_YEAR =
        365.25 * 24 * 60 * 60 * 1000;

    /**
     * Constructor.
     *
     * @param  ndim  coordinate dimensionality
     * @param  errorModes   error mode array
     * @param  npoint  number of times {@link #storePoint} will be called.
     */
    public CartesianPointStore( int ndim, ErrorMode[] errorModes, int npoint ) {
        ndim_ = ndim;
        npoint_ = npoint;

        /* Set up an array of objects which can read error information for
         * each error dimension from the ValueStore. */
        List rdrList = new ArrayList();
        int nerrWord = 0;
        for ( int idim = 0; idim < errorModes.length; idim++ ) {
            ErrorReader rdr = createErrorReader( idim, errorModes[ idim ] );
            if ( rdr != null ) {
                rdrList.add( rdr );
                nerrWord += rdr.getWordCount();
            }
        }
        nerrWord_ = nerrWord;
        errorReaders_ = (ErrorReader[]) rdrList.toArray( new ErrorReader[ 0 ] );

        /* Work out the total number of words (doubles in this case) which
         * are used for each row of data. */
        nword_ = ndim_ + nerrWord;
        lnword_ = (long) nword_;

        /* Prepare buffers. */
        buf1_ = new double[ 1 ];
        point_ = new double[ ndim_ ];
        centre_ = new double[ ndim_ ];
        errors_ = new double[ errorReaders_.length * 2 ][];

        /* Initialise the value store which will hold the values.
         * Other implementations are possible. */
        valueStore_ = new ArrayPrimitiveStore( double.class, nword_ * npoint_ );
        assert double.class.equals( valueStore_.getType() );
    }

    public void storePoint( Object[] coordRow, Object[] errorRow,
                            String label ) {
        long ioff = ipoint_ * lnword_;
        for ( int i = 0; i < ndim_; i++ ) {
            buf1_[ 0 ] = doubleValue( coordRow[ i ] );
            valueStore_.put( ioff++, buf1_, 0, 1 );
        }
        for ( int i = 0; i < nerrWord_; i++ ) {
            buf1_[ 0 ] = doubleValue( errorRow[ i ] );
            valueStore_.put( ioff++, buf1_, 0, 1 );
        }
        assert ioff == ( ipoint_ + 1 ) * lnword_;
        ipoint_++;
    }

    public int getCount() {
        return npoint_;
    }

    public int getNdim() {
        return ndim_;
    }

    public double[] getPoint( int ipoint ) {
        valueStore_.get( ipoint * lnword_, point_, 0, ndim_ );
        return point_;
    }

    public int getNerror() {
        return errorReaders_.length * 2;
    }

    public double[][] getErrors( int ipoint ) {
        valueStore_.get( ipoint * lnword_, centre_, 0, ndim_ );
        long off = ipoint * nword_ + ndim_;
        int ierr = 0;
        for ( int ir = 0; ir < errorReaders_.length; ir++ ) {
            ErrorReader rdr = errorReaders_[ ir ];
            double[][] errs = rdr.readErrors( centre_, valueStore_, off );
            errors_[ ierr++ ] = errs[ 0 ];
            errors_[ ierr++ ] = errs[ 1 ];
            off += rdr.getWordCount();
        }
        assert ierr == errorReaders_.length * 2;
        assert off == ( ipoint + 1 ) * nword_;
        return errors_;
    }

    public boolean hasLabels() {
        return false;
    }

    public String getLabel( int ipoint ) {
        return null;
    }

    /**
     * Utility method to convert an object into a numeric (double) value
     * where possible.
     *
     * @param  value  value to decode
     * @return   double precision equivalent
     */
    public static double doubleValue( Object value ) {
        if ( value instanceof Number ) {
            return ((Number) value).doubleValue();
        }
        else if ( value instanceof Date ) {
            long milliseconds = ((Date) value).getTime();
            return 1970.0 + milliseconds / MILLISECONDS_PER_YEAR;
        }
        else {
            return Double.NaN;
        }
    }

    /**
     * Returns an ErrorReader suitable for accessing data from a given
     * ErrorMode.
     *
     * @param   idim   error dimension index
     * @param   mode  error mode
     * @return   error reader
     */
    private ErrorReader createErrorReader( final int idim, ErrorMode mode ) {
        if ( ErrorMode.SYMMETRIC.equals( mode ) ) {
            return new ErrorReader( 1 ) {
                final double[] lo = new double[ ndim_ ];
                final double[] hi = new double[ ndim_ ];
                protected void convertErrors( double[] point, double[] rawErrs,
                                              double[][] errCoords ) {
                    double off = rawErrs[ 0 ];
                    if ( off > 0 ) {
                        for ( int id = 0; id < ndim_; id++ ) {
                            lo[ id ] = point[ id ];
                            hi[ id ] = point[ id ];
                        }
                        lo[ idim ] -= off;
                        hi[ idim ] += off;
                        errCoords[ 0 ] = lo;
                        errCoords[ 1 ] = hi;
                    }
                    else {
                        errCoords[ 0 ] = null;
                        errCoords[ 1 ] = null;
                    }
                }
            };
        }
        else if ( ErrorMode.LOWER.equals( mode ) ) {
            return new ErrorReader( 1 ) {
                final double[] lo = new double[ ndim_ ];
                protected void convertErrors( double[] point, double[] rawErrs,
                                              double[][] errCoords ) {
                    double off = rawErrs[ 0 ];
                    if ( off > 0 ) {
                        for ( int id = 0; id < ndim_; id++ ) {
                            lo[ id ] = point[ id ];
                        }
                        lo[ idim ] -= off;
                        errCoords[ 0 ] = lo;
                    }
                    else {
                        errCoords[ 0 ] = null;
                    }
                    errCoords[ 1 ] = null;
                }
            };
        }
        else if ( ErrorMode.UPPER.equals( mode ) ) {
            return new ErrorReader( 1 ) {
                final double[] hi = new double[ ndim_ ];
                protected void convertErrors( double[] point, double[] rawErrs,
                                              double[][] errCoords ) {
                    double off = rawErrs[ 0 ];
                    if ( off > 0 ) {
                        for ( int id = 0; id < ndim_; id++ ) {
                            hi[ id ] = point[ id ];
                        }
                        hi[ idim ] += off;
                        errCoords[ 1 ] = hi;
                    }
                    else {
                        errCoords[ 1 ] = null;
                    }
                    errCoords[ 0 ] = null;
                }
            };
        }
        else if ( ErrorMode.BOTH.equals( mode ) ) {
            return new ErrorReader( 2 ) {
                final double[] lo = new double[ ndim_ ];
                final double[] hi = new double[ ndim_ ];
                protected void convertErrors( double[] point, double[] rawErrs,
                                              double[][] errCoords ) {
                    double loOff = rawErrs[ 0 ];
                    double hiOff = rawErrs[ 1 ];
                    if ( loOff > 0 ) {
                        for ( int id = 0; id < ndim_; id++ ) {
                            lo[ id ] = point[ id ];
                        }
                        lo[ idim ] -= loOff;
                        errCoords[ 0 ] = lo;
                    }
                    else {
                        errCoords[ 0 ] = null;
                    }
                    if ( hiOff > 0 ) {
                        for ( int id = 0; id < ndim_; id++ ) {
                            hi[ id ] = point[ id ];
                        }
                        hi[ idim ] += hiOff;
                        errCoords[ 1 ] = hi;
                    }
                    else {
                        errCoords[ 1 ] = null;
                    }
                }
            };
        }
        else {
            return null;
        }
    }

    /**
     * Helper class which decodes error information from the ValueStore.
     * When it comes time to read error information from the store,
     * the reader is pointed at the offset into it at which the error
     * information is found, and then reads some number of values to
     * determine the upper and lower errors.
     */
    private abstract class ErrorReader {

        private final int wordCount_;
        private final double[] buf_;
        private final double[][] pair_;

        /**
         * Constructor.
         *
         * @param  wordCount  number of words read from the value store for
         *                    each point
         */
        ErrorReader( int wordCount ) {
            wordCount_ = wordCount;
            buf_ = new double[ wordCount ];
            pair_ = new double[ 2 ][];
        }

        /**
         * Returns the number of words read from the value store for each point.
         *
         * @return  word count
         */
        public int getWordCount() {
            return wordCount_;
        }

        /**
         * Gets coordinates of lower and upper error bounds using information
         * at a given offset in a ValueStore.
         *
         * @param  centre  coordinates of central point
         * @param  store   value store
         * @param  off     offset of error information into store
         * @return  2-element array giving coords of lower, upper error bounds; 
         *          elements may be null for no/zero error
         */
        public double[][] readErrors( double[] centre, ValueStore store,
                                      long off ) {
            store.get( off, buf_, 0, wordCount_ );
            convertErrors( centre, buf_, pair_ );
            return pair_;
        }

        /**
         * Works out the coordinates of the lower and upper error bounds
         * from a buffer containing the raw error values.
         * The results are written into the two elements of a supplied
         * double[][] array.  Null values should be used if the error
         * point is identical to the central point.
         *
         * @param  point   position of the central point
         * @param  rawErrs  raw error values
         * @param  2-element double[][] array to be filled with 
         *         lower and upper error point coordinates
         */
        protected abstract void convertErrors( double[] point,
                                               double[] rawErrs,
                                               double[][] errCoords );
    }
}
