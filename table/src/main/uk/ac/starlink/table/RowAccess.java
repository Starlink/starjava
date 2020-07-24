package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Provides random access to table data.
 * An instance of this class is not in general safe for use
 * from multiple threads.
 *
 * @author   Mark Taylor
 * @since    24 Jul 2020
 */
public interface RowAccess extends RowData {

    /**
     * Sets the row index to which subsequent data accesses will refer.
     *
     * <p>This method must be called before the first invocation of
     * {@link #getCell}/{@link #getRow}, otherwise behaviour is undefined
     * (but implementations will likely throw an unchecked exception
     * on subsequent getCell/getRow calls).
     * The effect of setting the value to an out of range value is undefined;
     * it may throw an exception here, or during subsequent calls,
     * or behave otherwise.
     *
     * @param  irow  row index
     */
    void setRowIndex( long irow ) throws IOException;

    /**
     * Returns the contents of a given cell at the current row.
     *
     * <p>Behaviour is undefined if the row index has not been set,
     * or has been set to a value for which no table row exists;
     * however implementations are encouraged to throw suitable
     * <code>RuntimeException</code> in this case.
     *
     * @param  icol   column index
     * @return   contents of given column at current row
     */
    Object getCell( int icol ) throws IOException;

    /**
     * Returns the contents of all the cells at the current row.
     *
     * <p>Note the returned object may be reused between invocations,
     *
     * <p>Behaviour is undefined if the row index has not been set,
     * or has been set to a value for which no table row exists;
     * however implementations are encouraged to throw a suitable
     * <code>RuntimeException</code> in this case.
     *
     * @return   array with one element for each column in the table
     */
    Object[] getRow() throws IOException;

    /**
     * Closes this reader.
     * The effect of calling any of the other methods following a close
     * is undefined.
     */
    void close() throws IOException;
}
