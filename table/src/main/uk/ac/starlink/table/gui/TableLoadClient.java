package uk.ac.starlink.table.gui;

import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Interface for a GUI-based consumer of tables which are being loaded.
 * All its methods are called on the Event Dispatch Thread, and must be
 * called in sequence:
 * <ol>
 * <li>{@link #startSequence}<li>
 * <li>{@link #setLabel}, {@link #loadSuccess}, {@link #loadFailure}
 *     (any number of times, in any order)</li>
 * <li>{@link #endSequence}</li>
 * </ol>
 
 *
 * @author   Mark Taylor
 * @since    13 Sept 2010
 */
public interface TableLoadClient {

    /**
     * Returns the table factory via which all loaded tables will be produced.
     *
     * @return   table factory
     */
    StarTableFactory getTableFactory();

    /**
     * Called before any table load attempts are made.
     */
    void startSequence();

    /**
     * Sets a short text string suitable for presenting to the user 
     * to indicate what is being loaded.  May be invoked one or more times
     * during the load sequence.
     */
    void setLabel( String label );

    /**
     * Presents a table which has been successfully loaded.
     * The return value indicates whether this client is interested in
     * attempts to load more tables, if there are more.
     *
     * @param  table  loaded table
     * @return  true  iff more loadSuccess/loadFailure calls are acceptable
     */
    boolean loadSuccess( StarTable table );  // true if want more

    /**
     * Presents a failure which has resulted from a table load attempt.
     * The return value indicates whether this client is interested in
     * attempts to load more tables, if there are more.
     *
     * @param  error  error
     * @return  true  iff more loadSuccess/loadFailure calls are acceptable
     */
    boolean loadFailure( Throwable error );

    /**
     * Indicates that no more loadSuccess/loadFailure methods will be invoked.
     * The <code>cancelled</code> argument indicates whether the sequence
     * finished naturally, or was cancelled by a deliberate act of the user.
     *
     * @param   cancelled  true iff the sequence was cancelled by a user action
     */
    void endSequence( boolean cancelled );
}
