package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Defines a set of callbacks to consume the information held in a 
 * StarTable.  This may be used to transmit a table from one place to
 * another in one go (passing a {@link StarTable} argument may be unsuitable
 * in that it requires the row sequence to be accessible multiple times).
 * Any source which uses this interface must do so in the following sequence:
 * <ol>
 * <li>Call {@link #acceptMetadata} once
 * <li>Call {@link #acceptRow} once for each row in the table
 * <li>Call {@link #endRows} once
 * </ol>
 * Implementations are under no obligation to behave sensibly if this
 * sequence is not observed.
 *
 * @author   Mark Taylor
 */
public interface TableSink {

    /**
     * Takes delivery of a row-less <tt>StarTable</tt> object which defines
     * the metadata of the table to be transmitted. 
     * If the number of rows that will be transmitted via subsequent
     * calls to <tt>acceptRow</tt> is known, this value should be made
     * available as the row count of <tt>meta</tt> 
     * ({@link StarTable#getRowCount}); if it is not known, the row count
     * should be -1.  However, this object should not attempt to read
     * any of <tt>meta</tt>'s cell data.
     * <p> 
     * The data to be transmitted in subsequent calls of <tt>acceptRow</tt>
     * must match the metadata transmitted in this call in the same way
     * that rows of a StarTable must match its own metadata (number and
     * content clases of columns etc).
     * If this sink cannot dispose of a table corresponding to <tt>meta</tt>
     * then it may throw a TableFormatException - this may be the case
     * if for instance <tt>meta</tt> has columns with types that this
     * sink can't deal with.
     *
     * @param   meta   table metadata object
     * @throws  TableFormatException  if this sink cannot accept table rows
     *          matching the given metadata
     */
    void acceptMetadata( StarTable meta ) throws TableFormatException;

    /**
     * Takes delivery of one row of data.  <tt>row</tt> is an array of
     * objects comprising the contents of one row of the table being
     * transmitted.  The number and classes of the elements of <tt>row</tt>
     * are described by the metadata object previously accepted.
     *
     * @param   row  table data row
     */
    void acceptRow( Object[] row ) throws IOException;

    /**
     * Signals that there are no more rows to be transmitted.
     */
    void endRows() throws IOException;
}
