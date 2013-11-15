package uk.ac.starlink.topcat.plot2;

import java.util.concurrent.atomic.AtomicLong;
import javax.swing.BoundedRangeModel;
import javax.swing.SwingUtilities;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

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
    private final BoundedRangeModel progModel_;
    private final long tupleCount_;
    private final long step_;
    private final long countScale_;
    private final AtomicLong index_;

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
        progModel_ = progModel;
        tupleCount_ = tupleCount;
        step_ = Math.max( tupleCount / 200, 1000 );
        countScale_ = 1 + ( tupleCount / Integer.MAX_VALUE );
        index_ = new AtomicLong();
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                progModel_.setMinimum( 0 );
                progModel_.setMaximum( getProgValue( tupleCount_ ) );
                progModel_.setValue( 0 );
            }
        } );
    }

    public boolean hasData( DataSpec dataSpec ) {
        return base_.hasData( dataSpec );
    }

    public TupleSequence getTupleSequence( DataSpec dataSpec ) {
        return new ProgressTupleSequence( base_.getTupleSequence( dataSpec ) );
    }

    /**
     * Called every time a tuple is read, to update the progress bar
     * as appropriate.
     */
    private void incrementCounter() {

        /* The modulo here serves two purposes: first if the supplied
         * limit is too low for some reason, the progress bar will wrap
         * around, which is visually reasonable.  Second, when the last
         * (expected) tuple has been read, the progress bar will be reset
         * to the start. */
        long ix = index_.incrementAndGet() % tupleCount_;

        /* Perform a GUI update only once every few tuples. */
        if ( ix % step_ == 0 ) {
            final int value = getProgValue( ix );
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    progModel_.setValue( value );
                }
            } );
        }
    }

    /**
     * Maps a value indicating which tuple has been read to a value
     * used by the progress bar.
     *
     * @param   tupleIndex   number in the range 0..tupleCount
     * @return   number in the range progMin..progMax
     */
    private int getProgValue( long tupleIndex ) {
        return (int) ( tupleIndex / countScale_ );
    }

    /**
     * Wrapper TupleSequence implementation which makes progress bar
     * increments when each tuple is read.
     */
    private class ProgressTupleSequence implements TupleSequence {

        private final TupleSequence baseSeq_;

        /**
         * Constructor.
         *
         * @param  baseSeq   base sequence to which most methods are delegated
         */
        ProgressTupleSequence( TupleSequence baseSeq ) {
            baseSeq_ = baseSeq;
        }

        public boolean next() {
            if ( baseSeq_.next() ) {
                incrementCounter();
                return true;
            }
            else {
                return false;
            }
        }

        public long getRowIndex() {
            return baseSeq_.getRowIndex();
        }

        public boolean getBooleanValue( int icol ) {
            return baseSeq_.getBooleanValue( icol );
        }

        public double getDoubleValue( int icol ) {
            return baseSeq_.getDoubleValue( icol );
        }

        public Object getObjectValue( int icol ) {
            return baseSeq_.getObjectValue( icol );
        }
    }
}
