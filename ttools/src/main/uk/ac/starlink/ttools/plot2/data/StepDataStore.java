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
        final TupleSequence baseSeq = baseStore_.getTupleSequence( spec );
        return new TupleSequence() {
            public boolean next() {
                for ( int i = 0; i < step_; i++ ) {
                    if ( ! baseSeq.next() ) {
                        return false;
                    }
                }
                return true;
            }
            public long getRowIndex() {
                return baseSeq.getRowIndex();
            }
            public Object getObjectValue( int icol ) {
                return baseSeq.getObjectValue( icol );
            }
            public double getDoubleValue( int icol ) {
                return baseSeq.getDoubleValue( icol );
            }
            public int getIntValue( int icol ) {
                return baseSeq.getIntValue( icol );
            }
            public boolean getBooleanValue( int icol ) {
                return baseSeq.getBooleanValue( icol );
            }
        };
    }

    public boolean hasData( DataSpec spec ) {
        return baseStore_.hasData( spec );
    }
}
