package uk.ac.starlink.ttools.plot2.data;

/**
 * Wrapper data store implementation that dispenses all column data
 * as evenly spaced subsamples by row.
 *
 * @author   Mark Taylor
 * @since    13 Nov 2013
 */
public class StepDataStore implements DataStore {

    private final DataStore baseStore_;
    private final int step_;

    /**
     * Constructor.
     *
     * @param   base  base data store to which most behaviour is delegated
     * @param   step  stride indicating the size of the subsample;
     *                step=1 means all rows, step=2 means every other one etc
     *                
     */
    public StepDataStore( DataStore base, int step ) {
        baseStore_ = base;
        step_ = step;
    }

    public TupleSequence getTupleSequence( DataSpec spec ) {
        return new StepTupleSequence( baseStore_.getTupleSequence( spec ),
                                      step_ );
    }

    public boolean hasData( DataSpec spec ) {
        return baseStore_.hasData( spec );
    }

    public TupleRunner getTupleRunner() {
        return baseStore_.getTupleRunner();
    }

    /**
     * TupleSequence implementation for StepDataStore.
     */
    private static class StepTupleSequence
            extends WrapperTuple
            implements TupleSequence {

        private final TupleSequence baseSeq_;
        private final int step_;

        /**
         * Constructor.
         *
         * @param  baseSeq   base sequence
         * @param  step     step
         */
        StepTupleSequence( TupleSequence baseSeq, int step ) {
            super( baseSeq );
            baseSeq_ = baseSeq;
            step_ = step;
        }

        public boolean next() {
            for ( int i = 0; i < step_; i++ ) {
                if ( ! baseSeq_.next() ) {
                    return false;
                }
            }
            return true;
        }

        public TupleSequence split() {
            TupleSequence splitSeq = baseSeq_.split();
            return splitSeq == null
                 ? null
                 : new StepTupleSequence( splitSeq, step_ );
        }

        public long splittableSize() {
            return baseSeq_.splittableSize() / step_;
        }
    }
}
