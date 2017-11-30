package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Wrapper data store implementation used only for counting the number
 * of tuples requested from it.
 *
 * <p>The intention is that an operation involving data access can be
 * run using this data store to find out how many tuples in total
 * would be read by the operation.  In order to run quickly however,
 * this implementation does not supply all those rows, it truncates
 * the number of tuples dispensed per sequence to a given (small) number.
 * When the operation is complete, the {@link #getTupleCount} method
 * can be called.
 *
 * @author   Mark Taylor
 * @since    15 Nov 2013
 */
public class CountDataStore implements DataStore {

    private final DataStore base_;
    private final int maxCount_;
    private long tupleCount_;

    /**
     * Constructor.
     *
     * @param  base   base data store to which most operations are delegated
     * @param  maxCount  maximum number of tuples actually dispensed
     *                   from tuplesequences acquired from this store
     */
    public CountDataStore( DataStore base, int maxCount ) {
        base_ = base;
        maxCount_ = maxCount;
    }

    public boolean hasData( DataSpec spec ) {
        return base_.hasData( spec );
    }

    public synchronized TupleSequence getTupleSequence( DataSpec spec ) {
        assert spec instanceof GuiDataSpec;
        long count = spec instanceof GuiDataSpec
                   ? ((GuiDataSpec) spec).getRowCount()
                   : spec.getSourceTable().getRowCount();
        tupleCount_ = count >= 0 ? tupleCount_ + count
                                 : -1;
        return new TruncatedTupleSequence( base_.getTupleSequence( spec ),
                                           maxCount_ );
    }

    /**
     * Returns the total number of tuples represented by the tuple sequences
     * dispensed by this data store since construction time.
     *
     * @return   total tuple count for non-truncated tuple sequences
     *           dispensed to date, or -1 if not known
     */
    public synchronized long getTupleCount() {
        return tupleCount_;
    }

    /**
     * TupleSequence wrapper implementation which limits the number of
     * tuples dispensed to some given number.
     */
    private static class TruncatedTupleSequence implements TupleSequence {
        private final TupleSequence base_;
        private final int maxCount_;
        private int index_;

        /**
         * Constructor.
         *
         * @param  base   base sequence to which most methods are delegated
         * @param  maxCount  maximum number of tuples actually dispensed
         *                   from tuplesequences acquired from this store
         */
        TruncatedTupleSequence( TupleSequence base, int maxCount ) {
            base_ = base;
            maxCount_ = maxCount;
        }

        public boolean next() {
            return index_++ < maxCount_ && base_.next();
        }

        public long getRowIndex() {
            return base_.getRowIndex();
        }

        public boolean getBooleanValue( int icol ) {
            return base_.getBooleanValue( icol );
        }

        public int getIntValue( int icol ) {
            return base_.getIntValue( icol );
        }

        public long getLongValue( int icol ) {
            return base_.getLongValue( icol );
        }

        public double getDoubleValue( int icol ) {
            return base_.getDoubleValue( icol );
        }

        public Object getObjectValue( int icol ) {
            return base_.getObjectValue( icol );
        }
    }
}
