package uk.ac.starlink.table.gui;

import java.awt.event.ActionEvent;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.storage.MonitorStoragePolicy;

/**
 * Thread which passes data from a TableLoader to a TableLoadClient.
 * As well as ensuring that everything happens on sensible threads,
 * and updating a progress bar appropriately, it provides the facility
 * to cancel the load in progress.
 *
 * @author   Mark Taylor
 * @since    13 Sept 2010
 */
public class TableLoadWorker extends Thread {

    private final TableLoader loader_;
    private final TableLoadClient client_;
    private final Action cancelAct_;
    private final StarTableFactory tfact_;
    private final ProgressBarTableSink progSink_;
    private final MonitorStoragePolicy policy_;
    private final JProgressBar progBar_;
    private boolean finished_;

    /**
     * Constructs a TableLoadWorker with a given progress bar.
     *
     * @param  loader  table loader, supplies tables
     * @param  client  table load client, consumes tables into a GUI
     * @param  progBar  progress bar to keep track of loading
     */
    public TableLoadWorker( TableLoader loader, TableLoadClient client,
                            JProgressBar progBar ) {
        loader_ = loader;
        client_ = client;
        progBar_ = progBar;
        cancelAct_ = new AbstractAction( "Cancel" ) {
            public void actionPerformed( ActionEvent evt ) {
                cancel();
            }
        };
        tfact_ = new StarTableFactory( client.getTableFactory() );
        progSink_ = new ProgressBarTableSink( progBar );
        policy_ = new MonitorStoragePolicy( tfact_.getStoragePolicy(),
                                            progSink_ );
        tfact_.setStoragePolicy( policy_ );
    }

    /**
     * Constructs a TableLoadWorker with a default progress bar.
     *
     * @param  loader  table loader, supplies tables
     * @param  client  table load client, consumes tables into a GUI
     */
    public TableLoadWorker( TableLoader loader, TableLoadClient client ) {
        this( loader, client, createDefaultProgressBar() );
    }

    /**
     * Returns the table loader used by this worker.
     *
     * @return  table loader
     */
    public TableLoader getLoader() {
        return loader_;
    }

    /**
     * Returns the table load client used by this worker.
     *
     * @return  load client
     */
    public TableLoadClient getLoadClient() {
        return client_;
    }

    /**
     * Returns an action which will cancel the current load.
     *
     * @return  cancel action
     */
    public Action getCancelAction() {
        return cancelAct_;
    }

    /**
     * Returns the progress bar controlled by this worker.
     *
     * @return  progress bar
     */
    public JProgressBar getProgressBar() {
        return progBar_;
    } 

    /**
     * Performs loading until completed or cancelled.
     */
    public void run() {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                client_.startSequence();
                client_.setLabel( loader_.getLabel() );
            }
        } );
        TableSequence tseq = null;
        boolean ok;
        try {
            tseq = loader_.loadTables( tfact_ );
            ok = true;
        }
        catch ( final Throwable error ) {
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( ! finished_ ) {
                        boolean interrupted = isInterruption( error );
                        if ( ! interrupted ) {
                            client_.loadFailure( error );
                        }
                        finish( interrupted );
                    }
                }
            } );
            ok = false;
        }
        if ( ok ) {
            boolean more = true;
            try {
                while ( ! finished_ && more ) {
                    StarTable table = null;
                    Throwable error = null;
                    boolean endSeq;
                    try {
                        table = tseq.nextTable();
                        endSeq = table == null;
                    }
                    catch ( Throwable e ) {
                        error = e;
                        endSeq = false;
                    }
                    if ( ! endSeq ) {
                        final StarTable table1 = table;
                        final Throwable error1 = error;
                        final Boolean[] moreHolder = new Boolean[ 1 ];
                        SwingUtilities.invokeAndWait( new Runnable() {
                            public void run() {
                                boolean more;
                                if ( table1 != null ) {
                                    more = client_.loadSuccess( table1 );
                                }
                                else {
                                    assert error1 != null;
                                    if ( isInterruption( error1 ) ) {
                                        if ( ! finished_ ) {
                                            finish( true );
                                        }
                                        more = false;
                                    }
                                    else {
                                        more = client_.loadFailure( error1 );
                                    }
                                }
                                moreHolder[ 0 ] = Boolean.valueOf( more );
                            }
                        } );
                        more = more && moreHolder[ 0 ].booleanValue();
                    }
                    else {
                        more = false;
                    }
                }
            }
            catch ( InvocationTargetException e ) {
                throw new RuntimeException( e );
            }
            catch ( InterruptedException e ) {
                throw new RuntimeException( e );
            }
            finally {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( ! finished_ ) {
                            finish( false ); }
                    }
                } );
            }
        }
    }

    /**
     * Cancels the current load.
     * It is not an error to call this multiple times.
     * It must be called on the Event Dispatch Thread.
     */
    public void cancel() {
        if ( ! finished_ ) {
            finish( true );
        }
    }

    /**
     * Tidies up following execution.  Must be called exactly once.
     * It must be called on the Event Dispatch Thread.
     *
     * @param  cancelled  true iff cancel has been called
     */
    protected void finish( boolean cancelled ) {
        assert ! finished_;
        finished_ = true;
        client_.endSequence( cancelled );
        if ( cancelled ) {
            policy_.interrupt();
        }
        progSink_.dispose();
    }

    /**
     * Works out whether a given throwable counts as an interruption.
     * This will generally be the case if the user has taken steps to
     * cancel the load.  When that's happened, it's best to avoid an
     * explicit notification (such as a popup) to tell the user that
     * the cancel has happened.
     *
     * @param  error   throwable to test
     * @return   true iff error apparently originated from a cancellation action
     */
    private static boolean isInterruption( Throwable error ) {
        for ( Throwable err = error; err != null; err = err.getCause() ) {
            if ( err instanceof InterruptedException ||
                 err instanceof InterruptedIOException ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a progress bar suitable for use with a worker of this class.
     *
     * @return  new progress bar
     */
    private static JProgressBar createDefaultProgressBar() {
        JProgressBar progBar = new JProgressBar();
        progBar.setString( "" );
        progBar.setStringPainted( true );
        return progBar;
    }
}
