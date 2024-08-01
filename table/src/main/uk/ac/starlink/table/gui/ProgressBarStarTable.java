package uk.ac.starlink.table.gui;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import uk.ac.starlink.table.ProgressRowSplittable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * A WrapperStarTable which behaves the same as its base, except that 
 * UI intervention is permitted on any RowSequence which is taken out on it.
 * It provides two services:
 * <ol>
 * <li>Iteration using {@link #getRowSequence} or {@link #getRowSplittable}
 *     will update a supplied {@link javax.swing.JProgressBar} as it goes.
 * <li>Iteration can be forced to terminate based on the behaviour of
 *     a given BooleanSupplier.
 * <li>
 * </ol>
 *
 * <p>You might think this should be based on a 
 * {@link javax.swing.BoundedRangeModel} (JProgressBar's model) instead,
 * but unfortunately that doesn't allow you use of 
 * indeterminate progress states.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ProgressBarStarTable extends WrapperStarTable {

    private JProgressBar progBar_;
    private BooleanSupplier isCancelled_;
    private String label_;
    private static final int INTERVAL = 250;

    /**
     * Constructor with default cancellation control.
     * Iteration is cancelled if the calling thread is interrupted.
     * This may be appropriate for single-threaded iteration
     * (using a <code>RowSequence</code>),
     * but is unlikely to achieve cancellation of multi-threaded iteration
     * (using a <code>RowSplittable</code>).
     *
     * @param  baseTable  table supplying data
     * @param  progBar   progress bar to update
     */
    public ProgressBarStarTable( StarTable baseTable, JProgressBar progBar ) {
        this( baseTable, progBar, Thread::interrupted );
    }

    /**
     * Constructor with supplied cancellation control.
     *
     * @param  baseTable  table supplying data
     * @param  progBar   progress bar to update
     * @param  isCancelled  object controlling cancellation:
     *                      if it supplies a true value,
     *                      the <code>next</code> method of any
     *                      <code>RowSequence</code> or
     *                      <code>RowSplittable</code> taken out on
     *                      this table will (probably, at some point)
     *                      throw IOExceptions
     */
    @SuppressWarnings("this-escape")
    public ProgressBarStarTable( StarTable baseTable, JProgressBar progBar,
                                 BooleanSupplier isCancelled ) {
        super( baseTable );
        setProgressBar( progBar );
        isCancelled_ = isCancelled;
    }

    /**
     * Sets the progress bar which this table controls.
     *
     * @param  progBar  progress bar
     */
    public void setProgressBar( JProgressBar progBar ) {
        progBar_ = progBar;
    }

    /**
     * Returns the progress bar which this table controls.
     *
     * @return  progress bar
     */
    public JProgressBar getProgressBar() {
        return progBar_;
    }

    /**
     * Sets a label which will be visible in the progress bar when a
     * RowSequence acquired from this table is being iterated over.
     *
     * @param  label  label text
     */
    public void setActiveLabel( String label ) {
        label_ = label;
        progBar_.setStringPainted( label != null && label.trim().length() > 0 );
    }

    /**
     * Returns the text of the label which is visible in the progress bar
     * when a RowSequence acquired from this table is active.
     *
     * @return  label text
     */
    public String getActiveLabel() {
        return label_;
    }

    public RowSequence getRowSequence() throws IOException {
        setZero( true );
        return new WrapperRowSequence( baseTable.getRowSequence() ) {
            long irow;
            Timer timer = startProgressTimer( () -> irow );
            public boolean next() throws IOException {
                if ( isCancelled_.getAsBoolean() ) {
                    timer.stop();
                    throw new IOException( "Cancelled" );
                }
                else {
                    irow++;
                    return super.next();
                }
            }
            public void close() throws IOException {
                timer.stop();
                setZero( false );
                super.close();
            }
        };
    }

    public RowSplittable getRowSplittable() throws IOException {
        setZero( true );
        ProgressRowSplittable.Target target =
                new ProgressRowSplittable.Target() {
            AtomicLong irow = new AtomicLong();
            Timer timer = startProgressTimer( () -> irow.get() );
            public void updateCount( long count ) throws IOException {
                if ( isCancelled_.getAsBoolean() ) {
                    timer.stop();
                    throw new IOException( "Cancelled" );
                }
                irow.set( count );
            }
            public void done( long count ) {
                timer.stop();
                setZero( false );
            }
        };
        return new ProgressRowSplittable( super.getRowSplittable(), target );
    }

    /**
     * Returns a running timer that will update the progress bar periodically.
     *
     * @param  counter  yields the current row count
     * @return  started timer
     */
    private Timer startProgressTimer( final LongSupplier counter ) {
        Timer timer = new Timer( INTERVAL, evt -> {
            if ( progBar_ != null ) {
                progBar_.setValue( (int) counter.getAsLong() );
            }
        } );
        timer.setInitialDelay( 0 );
        timer.setCoalesce( true );
        timer.start();
        return timer;
    }

    /**
     * Resets the progress bar to its minimum.
     *
     * @param  labelOn  if true, display the active label in the progress bar;
     *                  if false, remove it
     */
    private void setZero( final boolean labelOn ) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                if ( progBar_ != null ) {
                    progBar_.setMinimum( 0 );
                    progBar_.setValue( 0 );
                    long nrow = getRowCount();
                    boolean determinate = nrow > 0 && nrow < Integer.MAX_VALUE;
                    progBar_.setIndeterminate( ! determinate );
                    if ( determinate ) {
                        progBar_.setMaximum( (int) nrow );
                    }
                    progBar_.setString( labelOn ? label_ : "" );
                }
            }
        } );
    }
}
