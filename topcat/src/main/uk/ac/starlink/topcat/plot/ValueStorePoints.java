package uk.ac.starlink.topcat.plot;

import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.table.ValueStore;
import uk.ac.starlink.table.storage.ArrayPrimitiveStore;

/**
 * Points implementation based on a {@link uk.ac.starlink.table.ValueStore}.
 * Is writable as well as readable.
 *
 * @author   Mark Taylor
 * @since    2 Nov 2005
 */
public class ValueStorePoints implements Points {

    private final int ndim_;
    private final int npoint_;
    private final ErrorReader[] errorReaders_;
    private final int nerr_;
    private final int nword_;
    private final ValueStore store_;
    private static final Map ERROR_READER_MAP = createErrorReaderMap();

    /**
     * Constructs a new points object.
     *
     * @param  ndim  dimensionality
     * @param  npoint number of points
     * @param  errorModes describes how error information will be stored
     */
    public ValueStorePoints( int ndim, int npoint, ErrorMode[] errorModes ) {
        ndim_ = ndim;
        npoint_ = npoint;

        /* Set the number of dimensions for which errors will be provided. */
        nerr_ = errorModes.length;

        /* Set up an array of objects which can read error information for
         * each error dimension from the ValueStore. */
        errorReaders_ = new ErrorReader[ nerr_ ];
        int nerrWord = 0;
        for ( int ierr = 0; ierr < nerr_; ierr++ ) {
            errorReaders_[ ierr ] = getErrorReader( errorModes[ ierr ] );
            nerrWord += errorReaders_[ ierr ].getWordCount();
        }

        /* Work out the total number of words (doubles in this case) which
         * are used for each row of data. */
        nword_ = ndim_ + nerrWord;

        /* Currently an ArrayPrimitiveStore implementation is hardwired in.
         * This could be modified so that it uses the default storage 
         * policy to get a suitable store object. */
        store_ = new ArrayPrimitiveStore( double.class, nword_ * npoint );
    }

    public int getNdim() {
        return ndim_;
    }

    public int getCount() {
        return npoint_;
    }

    public void getCoords( int ipoint, double[] coords ) {
        store_.get( ipoint * nword_, coords );
    }

    public boolean[] hasErrors() {
        boolean[] hasErrs = new boolean[ nerr_ ];
        for ( int ierr = 0; ierr < nerr_; ierr++ ) {
            hasErrs[ ierr ] = errorReaders_[ ierr ].hasErrors();
        }
        return hasErrs;
    }

    public void getErrors( int ipoint, double[] loErrs, double[] hiErrs ) {
        long off = ipoint * nword_ + ndim_;
        for ( int ierr = 0; ierr < nerr_; ierr++ ) {
            ErrorReader rdr = errorReaders_[ ierr ];
            rdr.readErrors( store_, off, loErrs, hiErrs, ierr );
            off += rdr.getWordCount();
        }
        assert off == ( ipoint + 1 ) * nword_;
    }

    /**
     * Stores a point in the vector.
     *
     * @param   ipoint  index of point
     * @param   coords  coordinate array 
     */
    public void putCoords( int ipoint, double[] coords ) {
        store_.put( ipoint * nword_, coords );
    }

    /**
     * Stores error information in the vector.  The errors array should be
     * the non-redundant list of values determined by the selectors 
     * determining the errors - its meaning can be determined only in
     * conjunction with the <code>ErrorMode</code> array supplied in the
     * constructor.
     *
     * @param  ipoint  point index
     * @param  errors  error value array
     */
    public void putErrors( int ipoint, double[] errors ) {
        store_.put( ipoint * nword_ + ndim_, errors );
    }

    /**
     * Returns an ErrorReader suitable for accessing data from a given
     * ErrorMode.
     *
     * @param   mode  error mode
     * @return   error reader
     */
    private static ErrorReader getErrorReader( ErrorMode mode ) {
        return (ErrorReader) ERROR_READER_MAP.get( mode );
    }

    /**
     * Returns a map from ErrorMode objects to the corresponding ErrorReaders.
     *
     * @return  {@link ErrorMode}-&gt;{@link ErrorReader} map
     */
    private static Map createErrorReaderMap() {
        Map map = new HashMap();
        map.put( ErrorMode.SYMMETRIC, new ErrorReader( 1 ) {
            protected void transfer( double[] buf, double[] lo, double[] hi,
                                     int off ) {
                lo[ off ] = buf[ 0 ];
                hi[ off ] = buf[ 0 ];
            }
        } );
        map.put( ErrorMode.LOWER, new ErrorReader( 1 ) {
            protected void transfer( double[] buf, double[] lo, double[] hi,
                                     int off ) {
                lo[ off ] = buf[ 0 ];
                hi[ off ] = 0;
            }
        } );
        map.put( ErrorMode.UPPER, new ErrorReader( 1 ) {
            protected void transfer( double[] buf, double[] lo, double[] hi,
                                     int off ) {
                lo[ off ] = 0;
                hi[ off ] = buf[ 0 ];
            }
        } );
        map.put( ErrorMode.BOTH, new ErrorReader( 2 ) {
            protected void transfer( double[] buf, double[] lo, double[] hi,
                                     int off ) {
                lo[ off ] = buf[ 0 ];
                hi[ off ] = buf[ 1 ];
            }
        } );
        map.put( ErrorMode.NONE, new ErrorReader( 0 ) {
            public void readErrors( ValueStore store, long storeOffset,
                                    double[] loErrs, double[] hiErrs,
                                    int errOffset ) {
                    loErrs[ errOffset ] = 0;
                hiErrs[ errOffset ] = 0;
            }
            protected void transfer( double[] buf, double[] lo, double[] hi,
                                     int off ) {
                assert false;
            }
        } );
        return map;
    }

    /**
     * Helper class which decodes error information from the ValueStore.
     * When it comes time to read error information from the store, 
     * the reader is pointed at the offset into it at which the error
     * information is found, and then reads some number of values to
     * determine the upper and lower errors.
     */
    private static abstract class ErrorReader {

        private final int wordCount_;
        protected final double[] buf_;

        /**
         * Constructor.
         *
         * @param  wordCount  number of words read from the value store for
         *                    each point
         */
        ErrorReader( int wordCount ) {
            wordCount_ = wordCount;
            buf_ = new double[ wordCount ];
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
         * Indicates whether there is any non-blank error information returned
         * by this reader.
         *
         * @return  false if this reader always reads blank error information
         */
        public boolean hasErrors() {
            return wordCount_ > 0;
        }

        /**
         * Reads error information from a ValueStore into lower and upper
         * error arrays.
         *
         * @param  store  value store
         * @param  storeOffset  index into the value store of first error value
         * @param  loErrs  destination array for lower error bound
         * @param  hiErrs  destination array for upper error bound
         * @param  errOffset  index into loErrs and hiErrs at which bounds
         *                    should be written
         */
        public void readErrors( ValueStore store, long storeOffset,
                                double[] loErrs, double[] hiErrs,
                                int errOffset ) {
            store.get( storeOffset, buf_ );
            transfer( buf_, loErrs, hiErrs, errOffset );
        }

        /**
         * Transfers values from a buffer into lower and upper error bound
         * arrays.  Called by the default {@link #readErrors} implementation.
         *
         * @param  buf     buffer containing <code>wordCount_</code> values
         *                 read from value store
         * @param  loErrs  destination array for lower error bound
         * @param  hiErrs  destination array for upper error bound
         * @param  off     index into loErrs and hiErrs at which bounds
         *                    should be written
         */
        protected abstract void transfer( double[] buf, double[] loErrs,
                                          double[] hiErrs, int off );
    }
}
