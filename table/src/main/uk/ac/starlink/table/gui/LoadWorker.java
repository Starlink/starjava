package uk.ac.starlink.table.gui;

import java.io.IOException;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTable;

/**
 * Handles asynchronous table loading.
 * In conjunction with a {@link TableConsumer}, this can be used to 
 * ensure that a table is loaded out of the event dispatch thread, 
 * so that the GUI remains responsive during what might potentially 
 * be a slow load process.
 *
 * <p>To use it, implement the abstract {@link #attemptLoad} method
 * to load a table, and then call {@link #invoke} from the event
 * dispatch thread.  This will cause a table to be loaded asynchronously
 * and the <tt>TableConsumer</tt> to be notified accordingly.
 *
 * <p>It will often be convenient to extend and use this via an anonymous
 * class.  For example:
 * <pre>
 *     final String location = getFileName();
 *     new LoadWorker( tableEater, location ) {
 *         protected StarTable attemptLoad() throws IOException {
 *             return new StarTableFactory().makeStarTable( location );
 *         }
 *     }.invoke();
 * </pre>
 *
 * @author   Mark Taylor (Starlink)
 * @since    29 Nov 2004
 */
public abstract class LoadWorker {

    private boolean done_;
    private final TableConsumer eater_;
    private final String id_;

    /**
     * Constructor for a loader which will attempt to feed a loaded table
     * to a given consumer.
     *
     * @param  eater  table consumer which will be notified about the
     *                table load process
     * @param  id     string identifier for the table such as its filename.
     *                This may be used in messages to the user
     */
    public LoadWorker( TableConsumer eater, String id ) {
        eater_ = eater;
        id_ = id;
    }

    /**
     * Performs a table load.  This method will be invoked from a thread
     * other than the event dispatch thread.
     * It is permitted to return null from this method, but throwing an
     * exception is preferred if the requested table cannot be loaded.
     *
     * @return  loaded table
     * @throws  IOException   if something goes wrong
     */
    protected abstract StarTable attemptLoad() throws IOException;

    /**
     * Causes the {@link #attemptLoad} method to be called from a new
     * thread, and notifies the table consumer accordingly.
     * This method may only be invoked once for each instance of this class.
     * It should be invoked from the event dispatch thread (and will execute
     * quickly).
     */
    public void invoke() {
        if ( done_ ) {
            throw new IllegalStateException( "Already invoked!" );
        }
        done_ = true;
        eater_.loadStarted( id_ );
        new Thread( "Table Loader (" + id_ + ")" ) {
            public void run() {
                IOException error = null;
                StarTable table = null;
                try {
                    table = attemptLoad();
                }
                catch ( IOException e ) {
                    error = e;
                }
                final IOException error1 = error;
                final StarTable table1 = table;
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( table1 != null ) {
                            eater_.loadSucceeded( table1 );
                        }
                        else {
                            eater_.loadFailed( error1 );
                        }
                    }
                } );
            }
        }.start();
    }
}
