package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Provides sequential access to the data in a table.
 * The data is a sequence of rows which may be processed from the 
 * first to the last.
 * A RowSequence iterates over the rows one at a time.  It starts off
 * positioned before the first row, so the <tt>next</tt> method must be
 * invoked before the first row can be accessed.
 *
 * <p>Typical usage might look like this:
 * <pre>
 *     RowSequence rseq = table.getRowSequence();
 *     try {
 *         while ( rseq.hasNext() ) {
 *             rseq.next();
 *             Object[] row = rseq.getRow();
 *                ...
 *         }
 *     }
 *     finally {
 *         rseq.close();
 *     }
 * </pre>
 * 
 * <p>A RowSequence cannot in general be expected to be used safely from 
 * multiple threads.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface RowSequence {

    /**
     * Advances the current row to the next one.  
     * Since the initial position of a RowSequence is before the first row,
     * this method must be called before current row data
     * can be accessed using the 
     * {@link #getCell(int)} or {@link #getRow()} methods.
     * An unchecked exception such as <tt>NoSuchElementException</tt>
     * will be thrown if {@link #hasNext} returns <tt>false</tt>.
     *
     * @throws  IOException if there is some error in the positioning
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
     * Returns the contents of a cell in the current row.
     * The class of the returned object should be the same as,
     * or a subclass of, the class returned by
     * <tt>getColumnInfo(icol).getContentClass()</tt>.
     * An unchecked exception will be thrown if there is no current
     * row (<tt>next</tt> has not yet been called).
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
     * An unchecked exception will be thrown if there is no current
     * row (<tt>next</tt> has not yet been called).
     *
     * @return  an array of the objects in each cell in row <tt>irow</tt>
     * @throws  IOException if there is an error reading the data
     */
    Object[] getRow() throws IOException;

    /**
     * Indicates that this sequence will not be required any more.
     * This should release resources associated with this object.
     * The effect of calling any of the other methods following a 
     * <code>close</code> is undefined.
     */
    void close() throws IOException;

}
