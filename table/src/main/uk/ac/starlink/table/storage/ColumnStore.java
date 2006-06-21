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
 * <li>Zero or more calls of {@link #readCell}</li>
 * <li>Optionally, a call to {@link #dispose}</li>
 * <ol>
 * Behaviour will be undefined if you violate this sequence.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
interface ColumnStore {

    /**
     * Writes a datum to this store.
     *
     * @param   value  the value to add
     */
    void acceptCell( Object value ) throws IOException;

    /**
     * Signals that no more calls to <code>acceptCell</code> will be made,
     * and that calls to <code>readCell</code> may be made.
     */
    void endCells() throws IOException;

    /**
     * Retrieves a datum from this store.
     * The supplied index corresponds to the sequence in which the values
     * were written using {@link #acceptCell}.
     *
     * @param  lrow  index of datum to retrieve
     * @return  the <code>lrow</code><sup>th</sup> written value
     */
    Object readCell( long lrow ) throws IOException;

    /**
     * Releases any resources.  This object may not subsequently be used.
     */
    void dispose();
}
