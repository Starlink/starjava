package uk.ac.starlink.table.join;

/**
 * Wraps a ProgressIndicator for sequential usage.
 * This takes care of messaging a progress indicator periodically
 * given that the number of iterations is known up front.
 *
 * <p>This object should not be used from multiple threads concurrently.
 *
 * @author   Mark Taylor
 * @since    21 Sep 2022
 */
public class ProgressTracker implements AutoCloseable {

    private final ProgressIndicator progger_;
    private final double count1_;
    private final int blockSize_;
    private long index_;

    /**
     * Constructor.
     *
     * @param  progger  progress indicator
     * @param  count    number of invocations of progressNext expected
     * @param  txt      stage title to pass to indicator
     */
    public ProgressTracker( ProgressIndicator progger, long count,
                            String txt ) {
        progger_ = progger;
        count1_ = count > 0 ? 1.0 / count : 0;
        blockSize_ = 10_000;
        progger.startStage( txt );
    }

    /**
     * Registers the next iteration.
     * This method is cheap.
     */
    public void nextProgress() throws InterruptedException {
        if ( ++index_ % blockSize_ == 0 ) {
            progger_.setLevel( index_ * count1_ );
        }
    }

    /**
     * Signals that the iteration is finished.
     * Must be called to indicate that this tracker will no longer be used.
     */
    public void close() {
        try {
            progger_.setLevel( index_ * count1_ );
        }
        catch ( InterruptedException e ) {
            // never mind
        }
        progger_.endStage();
    }
}
