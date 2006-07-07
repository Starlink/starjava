package uk.ac.starlink.table.gui;

import uk.ac.starlink.table.StarTable;

/**
 * Interface which provides callback methods for a table load.
 * The correct sequence must be observed when an instance of this 
 * class is used: you must either call 
 * <blockquote>
 *    {@link #loadStarted} followed by {@link #loadSucceeded} or
 * </blockquote>
 * or
 * <blockquote>
 *    {@link #loadStarted} followed by {@link #loadFailed}.
 * </blockquote>
 * You can't nest these.
 * All these calls must be performed from the event dispatch thread.
 *
 * @see   TableLoadChooser
 * @author   Mark Taylor (Starlink)
 * @since    29 Nov 2004
 */
public interface TableConsumer {

    /**
     * Called when an attempt to load a table has been initiated.
     *
     * @param   id  identifier for the source of the table (such as a filename)
     */
    void loadStarted( String id );

    /**
     * Called when a table has successfully been loaded.
     *
     * @param   table  the table that has been acquired
     */
    void loadSucceeded( StarTable table );

    /**
     * Called when a table load has failed for some reason.
     *
     * @param  th  exception describing what went wrong (may be null)
     */
    void loadFailed( Throwable th );
}
