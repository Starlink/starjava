package uk.ac.starlink.ttools.plot2.data;

import java.util.function.Supplier;

/**
 * TupleSequence implementation based on CachedColumns.
 *
 * @author   Mark Taylor
 * @since    3 Jan 2020
 */
public class CachedTupleSequence implements TupleSequence {

    private final Supplier<CachedReader> maskSupplier_;
    private final Supplier<CachedReader[]> colsSupplier_;
    private final CachedReader maskRdr_;
    private final CachedReader[] colRdrs_;
    private long irow_;
    private long nrow_;
    private static final CachedReader TRUE_READER = new TrueReader();

    /**
     * Public constructor.
     *
     * @param  maskSupplier  supplier for boolean-typed column reader,
     *                       providing inclusion flags per row;
     *                       null means include all rows
     * @param  colsSupplier  supplier for array of columns providing
     *                       data cells per row
     */
    public CachedTupleSequence( Supplier<CachedReader> maskSupplier,
                                Supplier<CachedReader[]> colsSupplier,
                                long nrow ) {
        this( maskSupplier, colsSupplier, -1L, nrow );
    }

    /**
     * Constructor for internal use (recursion).
     *
     * @param  maskSupplier  supplier for boolean-typed column reader,
     *                       providing inclusion flags per row;
     *                       null means include all rows
     * @param  colsSupplier  supplier for array of columns providing
     *                       data cells per row
     * @param  irow   row index immediately before start of iteration range
     * @param  nrow   row index immediately after end of iteration range
     */
    private CachedTupleSequence( Supplier<CachedReader> maskSupplier,
                                 Supplier<CachedReader[]> colsSupplier,
                                 long irow, long nrow ) {
        maskSupplier_ = maskSupplier;
        colsSupplier_ = colsSupplier;
        irow_ = irow;
        nrow_ = nrow;
        CachedReader maskRdr = maskSupplier == null ? null : maskSupplier.get();
        maskRdr_ = maskRdr == null ? TRUE_READER : maskRdr;
        colRdrs_ = colsSupplier.get();
    }

    public boolean next() {
        while ( ++irow_ < nrow_ ) {
            if ( maskRdr_.getBooleanValue( irow_ ) ) {
                return true;
            }
        }
        return false;
    }

    public TupleSequence split() {
        if ( nrow_ - irow_ > 2 ) {
            long mid = ( irow_ + nrow_ ) / 2;
            TupleSequence split =
                new CachedTupleSequence( maskSupplier_, colsSupplier_,
                                         irow_, mid );
            irow_ = mid - 1;
            return split;
        }
        else {
            return null;
        }
    }

    public long splittableSize() {
        return nrow_ - irow_;
    }

    public long getRowIndex() {
        return irow_;
    }

    public Object getObjectValue( int icol ) {
        return colRdrs_[ icol ].getObjectValue( irow_ );
    }

    public double getDoubleValue( int icol ) {
        return colRdrs_[ icol ].getDoubleValue( irow_ );
    }

    public int getIntValue( int icol ) {
        return colRdrs_[ icol ].getIntValue( irow_ );
    }

    public long getLongValue( int icol ) {
        return colRdrs_[ icol ].getLongValue( irow_ );
    }

    public boolean getBooleanValue( int icol ) {
        return colRdrs_[ icol ].getBooleanValue( irow_ );
    }

    /**
     * CachedReader implementation whose boolean value is always true.
     */
    private static class TrueReader implements CachedReader {
        public boolean getBooleanValue( long ix ) {
            return true;
        }
        public double getDoubleValue( long ix ) {
            return -1;
        }
        public int getIntValue( long ix ) {
            return -1;
        }
        public long getLongValue( long ix ) {
            return -1L;
        }
        public Object getObjectValue( long ix ) {
            return null;
        }
    }
}
