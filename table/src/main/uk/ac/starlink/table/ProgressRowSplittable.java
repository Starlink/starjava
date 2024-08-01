package uk.ac.starlink.table;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * RowSplittable wrapper that can manage progress reporting,
 * as well as force termination of iteration.
 *
 * <p>Note that it is important to close instances of this splittable
 * for correct progress reporting.
 *
 * @author   Mark Taylor
 * @since    12 Oct 2020
 */
public class ProgressRowSplittable implements RowSplittable {

    private final RowSplittable base_;
    private final Tracker tracker_;
    private final int trackBlock_;
    private long itrack_;

    /**
     * Public constructor.
     *
     * @param  base   splittable which this one is to monitor
     * @param  target  object to which progress events are reported
     */
    public ProgressRowSplittable( RowSplittable base, Target target ) {
        this( base, new Tracker( target ) );
    }

    /**
     * Constructor used internally.
     *
     * @param  base   splittable which this one is to monitor
     * @param  tracker  internal object to which progress events are reported
     */
    @SuppressWarnings("this-escape")
    private ProgressRowSplittable( RowSplittable base, Tracker tracker ) {
        base_ = base;
        tracker_ = tracker;
        trackBlock_ = 10_000;
        tracker_.addWorker( this );
    }

    public boolean next() throws IOException {
        if ( base_.next() ) {
            trackNext();
            return true;
        }
        else {
            return false;
        }
    }

    public ProgressRowSplittable split() {
        RowSplittable split0 = base_.split();
        return split0 == null ? null
                              : new ProgressRowSplittable( split0, tracker_ );
    }

    public Object getCell( int icol ) throws IOException {
        return base_.getCell( icol );
    }

    public Object[] getRow() throws IOException {
        return base_.getRow();
    }

    public LongSupplier rowIndex() {
        return base_.rowIndex();
    }

    public long splittableSize() {
        return base_.splittableSize();
    }

    public void close() throws IOException {
        if ( itrack_ > 0 ) {
            try {
                tracker_.addCount( itrack_ );
            }
            catch ( IOException e ) {
                // swallow this exception, which probably indicates interruption
            }
            itrack_ = 0;
        }
        tracker_.removeWorker( this );
        base_.close();
    }

    /**
     * Records an iteration for potential reporting to the tracker.
     * Such reports are only made after a significant number of iterations,
     * for reasons of efficiency.
     */
    private void trackNext() throws IOException {
        if ( ++itrack_ >= trackBlock_ ) {
            tracker_.addCount( itrack_ );
            itrack_ = 0;
            if ( Thread.interrupted() ) {
                throw new IOException( "Sequence interrupted" );
            }
        }
    }

    /**
     * Callback interface for objects that will be informed about iteration
     * progress, and also given the opportunity to terminate iteration.
     * Implementations should be thread-safe; there is no
     * guarantee about what threads they may be used from.
     */
    public interface Target {

        /**
         * Reports an updated figure for the progress.
         *
         * <p>Implementations may throw an IOException from this method;
         * the exception will be thrown during iteration from the
         * {@link ProgressRowSplittable#next next} method of this
         * <code>ProgressRowSplittable</code> and thus halt its iteration.
         *
         * @param  count  number of iterations so far
         * @throws   IOException  to interrupt execution
         */
        void updateCount( long count ) throws IOException;

        /**
         * Reports that progress has finished.
         *
         * @param  count  final number of iterations
         */
        void done( long count );
    }

    /**
     * Object used internally to keep track of progress.
     */
    private static class Tracker {
        private final Target target_;
        private final Set<ProgressRowSplittable> workers_;
        private final AtomicLong count_;

        /**
         * Constructor.
         *
         * @param  target  user-supplied messaging callback
         */
        Tracker( Target target ) {
            target_ = target;
            workers_ = new HashSet<ProgressRowSplittable>();
            count_ = new AtomicLong();
        }

        /**
         * Reports an increment in the number of rows processed.
         *
         * @param  inc  number of newly processed rows
         */
        void addCount( long inc ) throws IOException {
            target_.updateCount( count_.addAndGet( inc ) );
        }

        /**
         * Registers a new splittable that will report to this tracker.
         *
         * @param  worker  new worker
         */
        void addWorker( ProgressRowSplittable worker ) {
            synchronized ( workers_ ) {
                workers_.add( worker );
            }
        }

        /**
         * Unregisters an existing splittable that will no longer report
         * to this tracker.
         *
         * @param  worker   worker to remove
         */
        void removeWorker( ProgressRowSplittable worker ) {
            boolean isFinal;
            synchronized ( workers_ ) {
                isFinal = workers_.remove( worker ) && workers_.isEmpty();
            }
            if ( isFinal ) {
                target_.done( count_.longValue() );
            }
        }
    }
}
