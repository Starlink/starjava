package uk.ac.starlink.topcat.activate;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import uk.ac.starlink.topcat.Outcome;

/**
 * Manages the GUI and behaviour for one entry in the list of
 * activation options displayed in the ActivationWindow.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2018
 */
public class ActivationEntry {

    private final ActivationType atype_;
    private final ActivatorConfigurator configurator_;
    private final ActivationLogPanel logPanel_;
    private boolean isBlocked_;
    private ExecutorService queue_;
    private Job lastJob_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.activate" );

    /**
     * Constructor.
     *
     * @param   atype   activation type
     * @param   tinfo   information about TopcatModel for which this entry
     *                  will work
     */
    public ActivationEntry( ActivationType atype, TopcatModelInfo tinfo ) {
        atype_ = atype;
        configurator_ = atype.createConfigurator( tinfo );
        logPanel_ = new ActivationLogPanel( 100 );
    }

    /**
     * Returns the activation type managed by this entry.
     *
     * @return  activation type
     */
    public ActivationType getType() {
        return atype_;
    }

    /**
     * Returns the GUI configuration component associated with this entry.
     *
     * @return  configurator panel
     */
    public ActivatorConfigurator getConfigurator() {
        return configurator_;
    }

    /**
     * Returns the GUI component that displays the results of activation
     * actions associated with this entry.
     *
     * <p>The returned component should manage its own scrolling if required;
     * that is, it will not be wrapped in a JScrollPane.
     *
     * @return   results panel
     */
    public JComponent getLogPanel() {
        return logPanel_;
    }

    /**
     * Indicates whether this entry is considered a potential security risk.
     * This class maintains this flag, but doesn't take any notice of it.
     * The default status is unblocked.
     *
     * @return  true  iff setBlocked has been called with a true argument
     *                (more recently than with a false argument)
     */
    public boolean isBlocked() {
        return isBlocked_;
    }

    /**
     * Sets whether this entry is considered a potential security risk.
     * This class maintains this flag, but doesn't take any notice of it.
     * The default status is unblocked.
     *
     * @param  isBlocked  blocking flag
     */
    public void setBlocked( boolean isBlocked ) {
        isBlocked_ = isBlocked;
    }

    /**
     * Invokes the currently configured activation action for this entry,
     * scheduling it on an appropriate thread and directing the output
     * to a suitable destination for display.
     *
     * <p>This method should be called on the EDT.
     * It should return in a short amount of time.
     *
     * @param  lrow   row index to activate
     * @param  meta   activation metadata
     */
    public void activateRowAsync( final long lrow, final ActivationMeta meta ) {
        assert SwingUtilities.isEventDispatchThread();
        final Activator activator = configurator_.getActivator();
        if ( activator == null ) {
            return;
        }
        if ( lastJob_ != null ) {
            if ( lastJob_.future_.cancel( true ) ) {
                logPanel_.updateItem( lastJob_.item_,
                                      ActivationLogPanel.Status.CANCELLED,
                                      null );
            }
        }
        if ( activator.invokeOnEdt() ) {
            Outcome outcome = activator.activateRow( lrow, meta );
            String msg = outcome.getMessage();
            logPanel_.addItem( lrow, getStatus( outcome ), msg );
        }
        else {
            final ActivationLogPanel.Item item = logPanel_.addItem( lrow );
            lastJob_ = new Job( item, getQueue().submit( new Runnable() {
                public void run() {
                    executeAndUpdate( activator, lrow, meta, item );
                }
            } ) );
        }
    }

    /**
     * Invokes the currently configured activation action for this entry,
     * returning only when it has completed.
     * The output is directed the output to a suitable destination.
     *
     * <p>This method should not be called on the EDT.  It may take
     * some time to execute.
     *
     * @param  activator  activator
     * @param  lrow   row index to activate
     * @param  meta   activation metadata
     */
    public void activateRowSync( final Activator activator,
                                 final long lrow, final ActivationMeta meta ) {
        assert ! SwingUtilities.isEventDispatchThread();
        if ( activator == null ) {
            return;
        }
        if ( activator.invokeOnEdt() ) {
            try {
                SwingUtilities.invokeAndWait( new Runnable() {
                    public void run() {
                        Outcome outcome = activator.activateRow( lrow, meta );
                        String msg = outcome.getMessage();
                        logPanel_.addItem( lrow, getStatus( outcome ), msg );
                    }
                } );
            }
            catch ( Exception e ) {
                logger_.log( Level.WARNING,
                             "Synchronous activation failed: " + e, e );
            }
        }
        else {
            final AtomicReference<ActivationLogPanel.Item> itemRef =
                new AtomicReference<ActivationLogPanel.Item>();
            try {
                SwingUtilities.invokeAndWait( new Runnable() {
                    public void run() {
                        itemRef.set( logPanel_.addItem( lrow ) );
                    }
                } );
            }
            catch ( Exception e ) {
                logger_.log( Level.WARNING,
                             "Synchronous activation failed: " + e, e );
            }
            final ActivationLogPanel.Item item = itemRef.get();
            if ( item == null ) {
                return;
            }
            executeAndUpdate( activator, lrow, meta, item );
        }
    }

    /**
     * Synchronously performs the activation action, and then
     * schedules a log panel update.
     * This method should not be called on the EDT.
     *
     * @param  activator  activator
     * @param  lrow    row index
     * @param  meta   additional activation metadata, or null
     * @param  item   destination for status updates
     */
    private void executeAndUpdate( Activator activator, long lrow,
                                   ActivationMeta meta,
                                   final ActivationLogPanel.Item item ) {
        Outcome outcome;
        try {
            outcome = activator.activateRow( lrow, meta );
        }
        catch ( Throwable e ) {
            logger_.log( Level.WARNING, "Activation failure: " + e, e );
            outcome = Outcome.failure( e );
        }
        final ActivationLogPanel.Status status = getStatus( outcome );
        final String msg = outcome.getMessage();
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                logPanel_.updateItem( item, status, msg );
            }
        } );
    }

    /**
     * Returns an execution queue on which potentially time-consuming
     * activation actions can be executed.
     *
     * <p>The current implementation returns a single-threaded queue,
     * so only one activation action for this entry is active at any one time.
     *
     * @return  execution service
     */
    private ExecutorService getQueue() {
        if ( queue_ == null ) {
            queue_ = Executors.newSingleThreadExecutor( new ThreadFactory() {
                public Thread newThread( Runnable r ) {
                    Thread thread = new Thread( r, "Activation for " + atype_ );
                    thread.setDaemon( true );
                    return thread;
                }
            } );
        }
        return queue_;
    }

    /**
     * Converts the success flag in an outcome to an ActivationLogPanel.Status.
     *
     * @param  outcome  outcome
     * @return  panel status flag
     */
    private static ActivationLogPanel.Status getStatus( Outcome outcome ) {
        return outcome.isSuccess() ? ActivationLogPanel.Status.OK
                                   : ActivationLogPanel.Status.FAIL;
    }

    /**
     * Aggregates an execution item with its Future.
     */
    private static class Job {
        final ActivationLogPanel.Item item_;
        final Future<?> future_;
        Job( ActivationLogPanel.Item item, Future<?> future ) {
            item_ = item;
            future_ = future;
        }
    }
}
