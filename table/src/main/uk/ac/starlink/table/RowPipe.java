package uk.ac.starlink.table;

import java.io.IOException;

/**
 * TableSink implementation whose returned table reads concurrently from rows
 * written into it.  Some of the methods may block, and the reading and
 * writing have to be done in different threads.
 *
 * <p>This serves almost the same purpose as a
 * {@link uk.ac.starlink.table.RowStore}, but has a subtly different contract.
 * Instead of RowStore's {@link uk.ac.starlink.table.RowStore#getStarTable}
 * method, which must be called after the <code>endRows</code> method,
 * it provides a {@link #waitForStarTable} method which may be called
 * before any or all rows have been written, but on a different thread.
 * This blocks until the metadata has been supplied.
 *
 * @author   Mark Taylor
 * @since    9 Aug 2007
 */
public interface RowPipe extends TableSink {

    /**
     * Returns a table whose data is supplied by that written into this sink.
     * Reads from this table should be called on a separate thread from 
     * the one which is writing into this sink.
     * It will block until {@link #acceptMetadata} has been called.
     *
     * @return   table
     */
    StarTable waitForStarTable() throws IOException;

    /**
     * May be called by the writing stream to set an I/O error on the pipe.
     * This error should be passed on to the reading end by throwing an
     * error with <code>e</code> as its cause from one of the read methods.
     * If an error has already been set by a previous call of this method,
     * this has no effect (only the first error is set).
     *
     * @param   e   exception to pass to readers
     */
    void setError( IOException e );
}
