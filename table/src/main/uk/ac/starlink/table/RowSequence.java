package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Provides sequential access to the data in a table.
 * The data is a sequence of rows which may be processed from the 
 * first to the last.
 * This object has a current row index which starts off at -1,
 * that is before the first row; the <tt>next</tt> method must be
 * invoked before the first row can be accessed.
 * <p>
 * A RowSequence cannot in general be expected to be used safely from 
 * multiple threads.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface RowSequence {

    /**
     * Advances the current row by 1.
     * Since the initial value of the current row is -1, <tt>next</tt>
     * must be called before current row data as accessed by the
     * {@link #getCell(int)} or {@link #getRow()} methods are invoked.
     *
     * @throws  IOException if there is some error in the positioning
     * @throws  IllegalStateException  if there are no more rows
     *          ({@link #hasNext} returns <tt>false</tt>)
     */
    void next() throws IOException;

    /**
     * Indicates whether this table contains any more rows after the current
     * one.
     *
     * @return  <tt>true</tt> iff another row exists
     */
    boolean hasNext();

    /**
     * Moves the current row forward by <tt>offset</tt> rows.
     *
     * @param  nrows  the number of rows to advance the current row by.
     * @throws  IllegalArgumentException if <tt>nrows&lt;0</tt>
     * @throws  IOException if an attempt to advance beyond the end of the
     *          table is made, or if there is some other read error
     */
    void advance( long nrows ) throws IOException;

    /**
     * Returns the contents of a cell in the current row.
     * The class of the returned object should be the same as,
     * or a subclass of, the class returned by
     * <tt>getColumnInfo(icol).getContentClass()</tt>.
     *
     * @return  the contents of cell <tt>icol</tt> in the current row
     * @throws IOException  if there is an error reading the data
     * @throws IllegalStateException if there is no current row (before the
     *         start of the table)
     */
    Object getCell( int icol ) throws IOException;

    /**
     * Returns the contents of the current table row, as an array
     * with the same number of elements as there are columns in this
     * table.
     *
     * @return  an array of the objects in each cell in row <tt>irow</tt>
     * @throws  IOException if there is an error reading the data
     * @throws IllegalStateException if there is no current row (before the
     *         start of the table)
     */
    Object[] getRow() throws IOException;

    /**
     * Returns the index of the current row.  This starts off at -1
     * before the first invocation of <tt>next</tt>.
     *
     * @return  the index of the current row
     */
    long getRowIndex();

}
