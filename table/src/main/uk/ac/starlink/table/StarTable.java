package uk.ac.starlink.table;

import java.io.IOException;
import java.util.Map;
import java.util.List;

/**
 * Defines basic table functionality.
 * A table has a fixed number of columns, and a sequence of rows, each of
 * which has one entry for each column.
 * The entry in each column is of the same type (or at least a subtype
 * of a given type) for each row; this type can be determined using
 * the {@link ColumnInfo} objects returned by {@link #getColumnInfo}.
 * The first row and the first column are numbered 0.
 * <p>
 * A <tt>StarTable</tt> has a current row, which is used for certain
 * data access methods.  All <tt>StarTables</tt> allow this current
 * row to move forward in a monotonic fashion.  If the {@link #isRandom}
 * method returns true then random access is also available, which
 * means that the current row may be set to any value.
 * Various relative and absolute positioning and data access methods
 * are provided, but invoking any which would cause the current row
 * to decrease (move backwards in the table) will result in an 
 * <tt>IOException</tt> for non-random-access tables.
 * When the object is created, there is no current row.
 * <p>
 * A <tt>StarTable</tt>, like an SQL {@link java.sql.ResultSet}, is more
 * a representation of a particular view of tabular data than of
 * a persistent table object, though it may be used to represent that too.
 * In particular, not all access patterns will be thread-safe, so 
 * separate clients may wish to obtain different <tt>StarTable</tt>
 * objects to access the same dataset.  The idea is similar to a
 * {@link uk.ac.starlink.array.ArrayAccess} object rather than 
 * {@link uk.ac.starlink.array.NDArray}.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface StarTable {

    /**
     * Returns the number of columns in this table.
     *
     * @return  the number of columns
     */
    int getColumnCount();

    /**
     * Returns the number of rows in this table, if known.  If the number
     * of rows cannot be (easily) determined, a value of -1 will be returned.
     *
     * @return  the number of rows, or -1
     */
    long getRowCount();

    /**
     * Returns an ordered list of {@link ValueInfo} objects representing 
     * the auxiliary metadata returned by 
     * <tt>getColumnInfo(int).getAuxData()</tt> calls.
     * The idea is that the resulting list can be used to find out 
     * the kind of per-column metadata which can be expected to be found 
     * in some or all columns of this table.  Each item in the returned
     * list should have a unique name, and other characteristics which are
     * applicable to auxData items which may be returned from any of
     * the columns in this table.
     * <p>
     * The order of the list may indicate some sort of natural ordering
     * of these keys.  The returned list is not guaranteed to be complete;
     * it is legal to return an empty list if nothing is known about
     * auxiliary metadata.  The list ought not to contain duplicate elements.
     *
     * @return  an unmodifiable ordered set of known metadata keys
     * @see  ColumnInfo#getAuxData
     */
    List getColumnAuxDataInfos();

    /**
     * Indicates whether random access is provided by this table.
     * If the result is <tt>true</tt> then the current row may be set
     * at any time to any existing row in the table.  If <tt>false</tt>,
     * then only advancing the row forward is permitted.
     *
     * @return  <tt>true</tt> if the current row may be moved backwards
     *          as well as forwards
     */
    boolean isRandom();

    /**
     * Returns the object describing the data in a given column.
     *
     * @param   icol  the column for which header information is required
     * @return  a ValueInfo object for column <tt>icol</tt>
     */
    ColumnInfo getColumnInfo( int icol );

    /**
     * Returns a list of table parameters, that is items which pertain to
     * the entire table.  Each element of this list must be a 
     * {@link DescribedValue} object.
     *
     * @return  a <tt>List</tt> of <tt>DescribedValue</tt> objects 
     *          constituting table-wide metadata not covered elsewhere 
     *          in this interface
     */
    List getParameters();

    /**
     * Returns a parameter (table-wide metadata item) of this table located
     * by its name.  If more than one parameter with the given name
     * exists, an arbitrary one will be returned.  If no parameter with
     * the given name exists, <tt>null</tt> will be returned.
     *
     * @param   parname  the name of the table parameter required
     */
    DescribedValue getParameterByName( String parname );

    /**
     * Advances the current row by 1.  
     * Equivalent to <tt>advanceCurrent(1)</tt>.
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
     * Positions the current row to a given index.
     * This will cause an <tt>IOException</tt> if this table does not
     * offer random access and <tt>irow</tt> is earlier than the current
     * row.
     *
     * @param  irow  the index of the new current row
     * @throws  IOException  if there is some error in the positioning,
     *          in particular if <tt>irow</tt> is earlier than the current
     *          row and random access is not available
     */
    void setCurrent( long irow ) throws IOException;

    /**
     * Returns the index of the current row.
     * The initial value of the current row is -1, indicating that there
     * is no current row.
     *
     * @param  current row of this table
     */
    long getCurrent();

    /**
     * Moves the current row forward by <tt>offset</tt> rows.
     * This will fail with an <tt>IOException</tt> if <tt>offset&lt;0</tt>
     * and this table does not offer random access.
     *
     * @param  offset  the number of rows to advance the current row by.
     *         May be negative as long as <tt>isRandom</tt> returns true
     * @throws  IOException  if <tt>offset&lt;0</tt> and random access is
     *          not available
     */
    void advanceCurrent( long offset ) throws IOException;

    /**
     * Returns the contents of a cell in the current row.
     * The class of the returned object should be the same as, 
     * or a subclass of, the class returned by 
     * <tt>getColumnInfo(icol).getContentClass()</tt>.
     *
     * @return  the contents of cell <tt>icol</tt> in the current row
     * @throws IOException  if there is an error reading the data
     * @throws IllegalStateException if there is no current row (before the
     *         start or after the end of the table)
     */
    Object getCell( int icol ) throws IOException;

    /**
     * Returns the contents of a given table cell.  
     * The class of the returned object should be the same as, 
     * or a subclass of, the class returned by
     * <tt>getColumnInfo(icol).getContentClass()</tt>.
     * The current row is reset to <tt>irow</tt>.
     *
     * @param  irow  the index of the cell's row
     * @param  icol  the index of the cell's column
     * @return  the contents of this cell
     * @throws IOException  if there is an error reading the data, or
     *         if <tt>irow</tt> is less than the current row and
     *         this table does not offer random access
     * @throws IllegalStateException if there is no current row (before the
     *         start or after the end of the table)
     */
    Object getCell( long irow, int icol ) throws IOException;

    /**
     * Returns the contents of the current table row, as an array
     * with the same number of elements as there are columns in this
     * table.
     *
     * @return  an array of the objects in each cell in row <tt>irow</tt>
     * @throws  IOException if there is an error reading the data
     */
    Object[] getRow() throws IOException;

    /**
     * Returns the contents of a given table row.  The returned value is
     * equivalent to an array formed of all the objects returned by 
     * <tt>getValueAt(irow,icol)</tt> for all the columns <tt>icol</tt> 
     * in sequence.
     * The current row is reset to <tt>irow</tt>.
     *
     * @param  irow  the index of the row to retrieve
     * @return  an array of the objects in each cell in row <tt>irow</tt>
     * @throws IOException  if there is an error reading the data, or
     *         if <tt>irow</tt> is less than the current row and
     *         this table does not offer random access
     */
    Object[] getRow( long irow ) throws IOException;

}
