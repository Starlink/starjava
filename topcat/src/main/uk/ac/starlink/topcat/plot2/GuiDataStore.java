package uk.ac.starlink.topcat.plot2;

import java.util.function.BooleanSupplier;
import javax.swing.BoundedRangeModel;
import uk.ac.starlink.ttools.plot2.data.AbortTupleSequence;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleRunner;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.data.WrapperTuple;

/**
 * DataStore wrapper implementation suitable for use from a GUI application.
 * This wrapper does two things.
 *
 * <p>First, it checks for thread interruptions regularly, and stops
 * dispensing tuples from the data store if in case of interruption.
 * The thread interruption status is undisturbed, and clients should
 * in general check the interuption status of the thread following use
 * of an instance of this class.
 *
 * <p>Second, it optionally updates a progress bar as tuples are read.
 * For this to work, it needs to know up front (at least a guess for)
 * how many tuples will be read in total over the lifetime of the store.
 * 
 * @author   Mark Taylor
 * @since    15 Nov 2013
 */
public class GuiDataStore implements DataStore {

    private final DataStore base_;
    private final Progresser progresser_;
    private volatile boolean isInit_;

    /**
     * Constructs a data store that checks for interruptions.
     *
     * @param  base  data store to which most behaviour will be delegated
     */
    public GuiDataStore( DataStore base ) {
        this( base, null, -1 );
    }

    /**
     * Constructs a data store that checks for interruptions and optionally
     * reports progress.
     * If <code>progBar</code> is null or <code>tupleCount</code> is negative,
     * there will be no progress updating, but the interruption handling
     * will still take place.
     *
     * @param  base  base data store to which most behaviour is delegated
     * @param  progModel  progress bar model, or null
     * @param  tupleCount   total number of tuples expected to be read
     *                      during the life of this data store;
     *                      -1 may be supplied if not known
     */
    public GuiDataStore( DataStore base, BoundedRangeModel progModel,
                         long tupleCount ) {
        base_ = base;
        progresser_ = progModel != null && tupleCount > 0
                    ? new Progresser( progModel, tupleCount )
                    : null;
    }

    public boolean hasData( DataSpec dataSpec ) {
        return base_.hasData( dataSpec );
    }

    public TupleSequence getTupleSequence( DataSpec dataSpec ) {
        if ( ! isInit_ ) {
            if ( progresser_ != null ) {
                progresser_.init();
            }
            isInit_ = true;
        }
        AbortTupleSequence tseq =
            new AbortTupleSequence( base_.getTupleSequence( dataSpec ),
                                    GuiDataStore::isInterrupted );
        return progresser_ == null
             ? tseq
             : new ProgressTupleSequence( tseq, progresser_ );
    }

    public TupleRunner getTupleRunner() {
        return base_.getTupleRunner();
    }

    /**
     * TupleSequence that updates a progresser during iteration.
     */
    private static class ProgressTupleSequence
            extends WrapperTuple
            implements TupleSequence {
        private final AbortTupleSequence baseSeq_;
        private final Progresser progresser_;
        private final int step_;
        private int count_;

        /**
         * Constructor.
         *
         * @param  baseSeq  base sequence
         * @param  progresser   progress indication control
         */
        ProgressTupleSequence( AbortTupleSequence baseSeq,
                               Progresser progresser ) {
            super( baseSeq );
            baseSeq_ = baseSeq;
            progresser_ = progresser;
            step_ = progresser.getStep();
            count_ = 0;
        }

        public boolean next() {
            if ( baseSeq_.next() ) {
                if ( ++count_ % step_ == 0 ) {
                    progresser_.add( count_ );
                    count_ = 0;
                } 
                return true;
            }
            else {
                if ( baseSeq_.isAborted() ) {
                    progresser_.reset();
                }
                else {
                    progresser_.add( count_ );
                }
                return false;
            }
        }

        public TupleSequence split() {
            AbortTupleSequence splitSeq = baseSeq_.split();
            return splitSeq == null
                 ? null
                 : new ProgressTupleSequence( splitSeq, progresser_ );
        }

        public long splittableSize() {
            return baseSeq_.splittableSize();
        }
    }

    /**
     * Checks for interruption status of the current thread.
     * If detected, the status is reset to its state on entry.
     *
     * @return  true iff current thread is interrupted
     */
    private static boolean isInterrupted() {
        if ( Thread.interrupted() ) {
            Thread.currentThread().interrupt();
            return true;
        }
        else {
            return false;
        }
    }
}
