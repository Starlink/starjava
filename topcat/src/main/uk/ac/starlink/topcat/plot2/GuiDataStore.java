package uk.ac.starlink.topcat.plot2;

import javax.swing.BoundedRangeModel;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.data.WrapperTupleSequence;

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
        progresser_ = progModel != null && tupleCount >= 0
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
        final TupleSequence baseSeq = base_.getTupleSequence( dataSpec );
        if ( progresser_ == null ) {
            return new WrapperTupleSequence( baseSeq ) {
                @Override
                public boolean next() {
                    if ( baseSeq.next() ) {
                        if ( Thread.interrupted() ) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                        else {
                            return true;
                        }
                    }
                    else {
                        return false;
                    }
                }
            };
        }
        else {
            return new WrapperTupleSequence( baseSeq ) {
                @Override
                public boolean next() {
                    if ( baseSeq.next() ) {
                        if ( Thread.interrupted() ) {
                            Thread.currentThread().interrupt();
                            progresser_.reset();
                            return false;
                        }
                        else {
                            progresser_.increment();
                            return true;
                        }
                    }
                    else {
                        return false;
                    }
                }
            };
        }
    }
}
