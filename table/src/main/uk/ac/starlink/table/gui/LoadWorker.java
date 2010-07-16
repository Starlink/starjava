package uk.ac.starlink.table.gui;

import java.io.IOException;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTable;

/**
 * Handles asynchronous table loading.
 * In conjunction with a {@link TableConsumer}, this can be used to 
 * ensure that a table or tables are loaded out of the event dispatch thread, 
 * so that the GUI remains responsive during what might potentially 
 * be a slow load process.
 *
 * <p>To use it, implement the abstract {@link #attemptLoads} method
 * to load tables, and then call {@link #invoke} from the event
 * dispatch thread.  This will cause tables to be loaded asynchronously
 * and the <tt>TableConsumer</tt> to be notified accordingly.
 *
 * <p>It will often be convenient to extend and use this via an anonymous
 * class.  For example:
 * <pre>
 *     final String location = getFileName();
 *     new LoadWorker(tableEater, location) {
 *         protected StarTable[] attemptLoads() throws IOException {
 *             StarTable table = new StarTableFactory().makeStarTable(location);
 *             return new StarTable[] {table};
 *         }
 *     }.invoke();
 * </pre>
 *
 * @author   Mark Taylor (Starlink)
 * @since    29 Nov 2004
 */
public abstract class LoadWorker {

    private Thread thread_;
    private boolean finished_;
    private final TableConsumer eater_;
    private final String id_;

    /**
     * Constructor for a loader which will attempt to feed loaded tables
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
     * exception is preferred if the requested tables cannot be loaded.
     *
     * @return  loaded tables
     * @throws  IOException   if something goes wrong
     */
    protected abstract StarTable[] attemptLoads() throws IOException;

    /**
     * Causes the {@link #attemptLoads} method to be called from a new
     * thread, and notifies the table consumer accordingly.
     * This method may only be invoked once for each instance of this class.
     * It should be invoked from the event dispatch thread (and will execute
     * quickly).
     */
    public synchronized void invoke() {
        if ( thread_ != null ) {
            throw new IllegalStateException( "Already invoked!" );
        }
        thread_ = new Thread( "Table Loader (" + id_ + ")" ) {
            public void run() {
                Throwable error = null;
                StarTable[] tables = null;
                try {
                    tables = attemptLoads();
                    if ( tables.length == 0 ) {
                        throw new IOException( "No tables present" );
                    }
                }
                catch ( Throwable e ) {
                    error = e;
                }
                synchronized ( LoadWorker.this ) {
                    if ( ! finished_ ) {
                        final Throwable error1 = error;
                        final StarTable[] tables1 = tables;
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                if ( ! finished_ ) {
                                    finished_ = true;
                                    if ( tables1 != null ) {
                                        eater_.loadSucceeded( tables1[ 0 ] );
                                        for ( int i = 1; i < tables1.length;
                                              i++ ) {
                                            eater_.loadStarted( id_ + "-"
                                                              + ( i + 1 ) );
                                            eater_
                                           .loadSucceeded( tables1[ i ] );
                                        }
                                    }
                                    else {
                                        eater_.loadFailed( error1 );
                                    }
                                }
                            }
                        } );
                    }
                }
            }
        };
        eater_.loadStarted( id_ );
        thread_.start();
    }

    /**
     * Interrupts the thread doing the work for this object if it is active.
     *
     * <p>Probably, this whole class ought to be a Thread subclass rather than
     * doing it like this, but it's part of the STIL public interface so 
     * I won't change it now.
     */
    public void interrupt() {
        if ( thread_ != null ) {
            synchronized ( this ) {
                if ( ! thread_.isInterrupted() ) {
                    thread_.interrupt();
                }
                if ( ! finished_ ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            if ( ! finished_ ) {
                                finished_ = true;
                                eater_.loadFailed( null );
                            }
                        }
                    } );
                }
            }
        }
    }
}
