package uk.ac.starlink.table.load;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.ProgressBarTableSink;
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
        StarTable[] tables = null;
        try {
            tables = loader_.loadTables( tfact_ );
        }
        catch ( final Throwable error ) {
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( ! finished_ ) {
                        client_.loadFailure( error );
                        finish( false );
                    }
                }
            } );
        }
        if ( tables != null ) {
            final StarTable[] tables1 = tables;
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( ! finished_ ) {
                        boolean more = true;
                        for ( int i = 0; i < tables1.length && more; i++ ) {
                            more = more && client_.loadSuccess( tables1[ i ] );
                        }
                        finish( false );
                    }
                }
            } );
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
