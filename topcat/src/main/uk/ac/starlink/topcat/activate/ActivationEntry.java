package uk.ac.starlink.topcat.activate;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
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
     * Invokes the currently configured activation action for this entry,
     * scheduling it on an appropriate thread and directing the output
     * to a suitable destination for display.
     *
     * <p><strong>Note:</strong> This method should be called on the EDT.
     *
     * @param  lrow   row index to activate
     * @param  meta   activation metadata
     */
    public void activateRow( final long lrow, final ActivationMeta meta ) {
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
                    try {
                        final Outcome outcome =
                            activator.activateRow( lrow, meta );
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                logPanel_
                               .updateItem( item, getStatus( outcome ),
                                            outcome.getMessage() );
                            }
                        } );
                    }
                    catch ( Throwable e ) {
                        scheduleLogFailure( item, e.toString() );
                        logger_.log( Level.WARNING, "Activation failure: " + e,
                                     e );
                    }
                }
            } ) );
        }
    }

    /**
     * Causes an asynchronous update of the given item's statues to failed.
     *
     * @param  item  item whose state is to be updated
     * @param  msg   failure detail message
     */
    private void scheduleLogFailure( final ActivationLogPanel.Item item,
                                     final String msg ) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                logPanel_.updateItem( item, ActivationLogPanel.Status.FAIL,
                                      msg );
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
