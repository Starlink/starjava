package uk.ac.starlink.table.storage;

import java.io.IOException;

/**
 * Defines an object which can store the data of a column, that is,
 * an array of homogeneous objects.
 * The store is populated sequentially, and when ready provides random access.
 *
 * <p>The sequence of calls must be as follows:
 * <ol>
 * <li>Zero or more calls of {@link #acceptCell}</li>
 * <li>A call of {@link #endCells}</li>
 * <li>Zero or more calls of {@link #createReader}</li>
 * </ol>
 * Behaviour will be undefined if you violate this sequence.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
public interface ColumnStore {

    /**
     * Writes a datum to this store.
     *
     * @param   value  the value to add
     */
    void acceptCell( Object value ) throws IOException;

    /**
     * Signals that no more calls to <code>acceptCell</code> will be made,
     * and that calls to <code>createReader</code> may be made.
     */
    void endCells() throws IOException;

    /**
     * Returns an object that can provide random access to the
     * cells written to this store.
     *
     * @return   column cell reader
     */
    ColumnReader createReader();
}
