package uk.ac.starlink.table.gui;

import java.awt.Component;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Provides a basic implementation of {@link TableConsumer}.
 * This provides a callback routine {@link #tableLoaded} which is called
 * if a successful table load completes.  If the load fails, the user
 * is informed in a popup dialogue.  A {@link #cancel} method is provided
 * to allow loading to be interrupted.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Dec 2004
 */
public abstract class BasicTableConsumer implements TableConsumer {

    private final Component parent_;
    private boolean loading_;
    private String id_;

    /**
     * Constructor.
     *
     * @param  parent  parent component (may be null)
     */
    public BasicTableConsumer( Component parent ) {
        parent_ = parent;
    }

    /**
     * Called from the event dispatch thread if and when a table is
     * successfully loaded.
     *
     * @param  table  the loaded table
     */
    protected abstract void tableLoaded( StarTable table );

    /**
     * Called when the loading sequence is over, because a table load
     * has completed successfully or unsuccessfully.
     * You can also call this method (on any thread) to interrupt loading -
     * this will prevent {@link #tableLoaded} being called in the future 
     * (until after another {@link #loadStarted} call).
     * Calling this method when loading is not in progress has no effect.
     */
    public synchronized void cancel() {
        if ( isLoading() ) {
            setLoading( false );
        }
    }

    /**
     * Determines whether this consumer is currently waiting for a load 
     * to finish.
     *
     * @return true iff we're waiting to do something when the load 
     *         succeeds or fails
     */
    public synchronized boolean isLoading() {
        return loading_;
    }

    /**
     * Called when the loading status changes.
     *
     * @param  isLoading  whether the status is now waiting or not waiting 
     */
    protected synchronized void setLoading( boolean isLoading ) {
        loading_ = isLoading;
    }

    /** 
     * Loading starts.
     *
     * @param   id  load target name
     * @throws  IllegalStateException  if a load is already in progress
     */
    public synchronized void loadStarted( String id ) {
        if ( isLoading() ) {
            throw new IllegalStateException();
        }
        id_ = id;
        setLoading( true );
    }

    /**
     * Loading succeeds.  {@link #tableLoaded} is called unless the operation
     * has already been cancelled.
     *
     * @param  table  table
     */
    public synchronized void loadSucceeded( StarTable table ) {
        if ( isLoading() ) {
            cancel();
            tableLoaded( table );
        }
    }

    /**
     * Loading fails.  The user is informed by a popup dialogue that this
     * has happened.
     *
     * @param  th  error
     */
    public synchronized void loadFailed( Throwable th ) {
        if ( isLoading() ) {
            cancel();
            ErrorDialog.showError( parent_, "Load Error", th,
                                   "Can't load table " + id_ );
        }
    }
}
