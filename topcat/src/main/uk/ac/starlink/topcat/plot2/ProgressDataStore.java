package uk.ac.starlink.topcat.plot2;

import javax.swing.BoundedRangeModel;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.data.WrapperTupleSequence;

/**
 * DataStore wrapper implementation which updates a given progress bar.
 * The progress bar model is updated as each of the tuples is read from
 * the tuple sequences it dispenses.
 *
 * @author   Mark Taylor
 * @since    15 Nov 2013
 */
public class ProgressDataStore implements DataStore {

    private final DataStore base_;
    private final Progresser progresser_;

    /**
     * Constructor.
     *
     * @param  base  base data store to which most behaviour is delegated
     * @param  progModel  progress bar model
     * @param  tupleCount   total number of tuples expected to be read
     *                      during the life of this data store
     */
    public ProgressDataStore( DataStore base, BoundedRangeModel progModel,
                              long tupleCount ) {
        base_ = base;
        progresser_ = new Progresser( progModel, tupleCount );
    }

    public boolean hasData( DataSpec dataSpec ) {
        return base_.hasData( dataSpec );
    }

    public TupleSequence getTupleSequence( DataSpec dataSpec ) {
        final TupleSequence baseSeq = base_.getTupleSequence( dataSpec );
        return new WrapperTupleSequence( baseSeq ) {
            @Override
            public boolean next() {
                if ( baseSeq.next() ) {
                    progresser_.increment();
                    return true;
                }
                else {
                    return false;
                }
            }
        };
    }
}
