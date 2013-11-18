package uk.ac.starlink.topcat.plot2;

import javax.swing.BoundedRangeModel;
import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages use of a JProgressBar model.
 * The methods of this class may be called from any thread.
 *
 * @author   Mark Taylor
 * @since    18 Nov 2013
 */
public class Progresser {

    private final BoundedRangeModel progModel_;
    private final long count_;
    private final long step_;
    private final long countScale_;
    private final long minStartMillis_;
    private final long minUpdateMillis_;
    private final AtomicLong index_;
    private final long start_;
    private long lastUpdate_;

    /**
     * Constructs a progresser with default step values.
     *
     * @param  progModel  progress bar model
     * @param  count    number of increments expected for progress completion
     */
    public Progresser( BoundedRangeModel progModel, long count ) {
        this( progModel, count, 200, 1000, 500, 80 );
    }

    /**
     * Constructs a progresser with step value configuration.
     *
     * @param  progModel  progress bar model
     * @param  count    number of increments expected for progress completion
     * @param  maxStepCount   maximum number of steps that will be recorded
     *                        in the GUI for progress completion
     * @param  minStepSize   minimum number of increments before a step is
     *                       recorded in the GUI
     * @param  minStartMillis  minimum interval in milliseconds after
     *                         initialisation before the first update is made
     * @param  minUpdateMillis  minimum interval in milliseconds between
     *                          updates
     */
    public Progresser( BoundedRangeModel progModel, final long count,
                       int maxStepCount, int minStepSize,
                       long minStartMillis, long minUpdateMillis ) {
        progModel_ = progModel;
        count_ = count;
        step_ = Math.max( count / maxStepCount, minStepSize );
        countScale_ = 1 + ( count / Integer.MAX_VALUE );
        minStartMillis_ = minStartMillis;
        minUpdateMillis_ = minUpdateMillis;
        index_ = new AtomicLong();
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                progModel_.setMinimum( 0 );
                progModel_.setMaximum( getProgValue( count ) );
                progModel_.setValue( 0 );
            }
        } );
        start_ = System.currentTimeMillis();
        lastUpdate_ = start_;
    }

    /**
     * Records a single increment contributing to the progress.
     */
    public void increment() {

        /* The modulo here serves two purposes: first if the supplied
         * limit is too low for some reason, the progress bar will wrap
         * around, which is visually reasonable.  Second, when the last
         * (expected) increment has been made, the progress bar will be
         * reset to the start. */
        long ix = index_.incrementAndGet() % count_;

        /* Perform a GUI update only once every few increments. */
        long now = System.currentTimeMillis();
        if ( ix % step_ == 0 &&
             now - start_ > minStartMillis_ &&
             now - lastUpdate_ > minUpdateMillis_ ) {
            lastUpdate_ = now;
            final int value = getProgValue( ix );
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    progModel_.setValue( value );
                }
            } );
        }
    }

    /**
     * Resets this progresser and its GUI to its initial state (no progress).
     */
    public void reset() {
        index_.set( 0L );
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                progModel_.setValue( 0 );
            }
        } );
    }

    /**
     * Maps a value giving the increment count to a value
     * used by the progress bar.
     *
     * @param   index   number in the range 0..count
     * @return   number in the range progMin..progMax
     */
    private int getProgValue( long index ) {
        return (int) ( index / countScale_ );
    }
}
