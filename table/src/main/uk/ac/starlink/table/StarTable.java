package uk.ac.starlink.table;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
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
 *
 * <p>All <tt>StarTable</tt>s allow sequential access, provided by 
 * calling {@link #getRowSequence}.  This may in general be
 * called multiple times so that more than one iteration can be made
 * through the rows of the table from start to finish.
 * The {@link #getRowSplittable} method supports multi-threaded
 * sequential access, and is used by {@link RowRunner}.
 * Additionally, if the {@link #isRandom} method returns <tt>true</tt>
 * then the random access methods {@link #getRowAccess},
 * {@link #getRow} and {@link #getCell}
 * may be used to access table contents directly.
 *
 * <p>For random tables, the <tt>getRow</tt> and <tt>getCell</tt> methods
 * should be thread-safe.  Separate <tt>RowSequence</tt>
 * and <tt>RowAccess</tt> objects obtained
 * from the same table should be safely usable from different threads, 
 * but a given <tt>RowSequence</tt>/<tt>RowAccess</tt> in general will not.
 *
 * <p>In general it is preferred to use {@link #getRowAccess} than
 * the equivalent <code>getRow</code>/<code>getCell</code> methods
 * of the table itself, since the assumption of single-threaded use
 * may permit more efficient implementation.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface StarTable extends Closeable {

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
     * Returns the URL of this table, if it has one.  A non-null return
     * from this method indicates that this table is in some sense persistent.
     *
     * @return  the URL of this table, or <tt>null</tt> if none is known
     */
    URL getURL();

    /**
     * Sets the URL of this table.  It ought to be possible in principle
     * to reconstruct this table by reading the resource at <tt>url</tt>.
     * If called, the supplied <tt>url</tt> should provide the return
     * value for subsequent calls of {@link #getURL}.
     *
     * @param  url  table URL
     */
    void setURL( URL url );

    /**
     * Returns the name of this table, if it has one.  The meaning of
     * the name is not defined, but it will typically be a short string of
     * text indicating the identity of this table.
     *
     * @return  a name for this table, or <tt>null</tt> if no suitable one
     *          exists
     */
    String getName();

    /**
     * Sets the name of this table.  If called, the supplied <tt>name</tt>
     * should provide the return value for subsequent calls of 
     * {@link #getName}.
     *
     * @param  name  table name
     */
    void setName( String name );

    /**
     * Returns a list of table parameters, that is metadata items
     * which pertain to the entire table.
     *
     * @return  a <tt>List</tt> of <tt>DescribedValue</tt> objects 
     *          constituting table-wide metadata not covered elsewhere 
     *          in this interface
     */
    List<DescribedValue> getParameters();

    /**
     * Returns a parameter (table-wide metadata item) of this table located
     * by its name.  If more than one parameter with the given name
     * exists, an arbitrary one will be returned.  If no parameter with
     * the given name exists, <tt>null</tt> will be returned.
     *
     * @param   parname  the name of the table parameter required
     */
    default DescribedValue getParameterByName( String parname ) {
        return Tables.getDescribedValueByName( getParameters(), parname );
    }

    /**
     * Adds the given DescribedValue to the list of parameter metadata objects
     * associated with this table.  If an item in the parameter list with
     * the same name as the supplied value already exists, it is removed
     * from the list.
     *
     * @param  dval  the new parameter datum to add
     */
    default void setParameter( DescribedValue dval ) {
        Tables.setDescribedValue( getParameters(), dval );
    }

    /**
     * Returns the object describing the data in a given column.
     *
     * @param   icol  the column for which header information is required
     * @return  a ValueInfo object for column <tt>icol</tt>
     */
    ColumnInfo getColumnInfo( int icol );

    /**
     * Returns an ordered list of ValueInfo objects representing 
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
    List<ValueInfo> getColumnAuxDataInfos();

    /**
     * Returns an object which can iterate over all the rows in the table
     * sequentially.
     * Each such returned object is safe for use within a single thread,
     * but not in general from multiple threads concurrently.
     * 
     * @return  new RowSequence
     * @throws  IOException   if there is an error providing access
     */
    RowSequence getRowSequence() throws IOException;

    /**
     * Returns an object which can provide random access to this
     * table's data, if random access is implemented.
     * Each such returned object is safe for use within a single thread,
     * but not in general from multiple threads concurrently.
     *
     * @return   new RowAccess
     * @throws IOException  if there is an error setting up access
     * @throws UnsupportedOperationException  if <tt>isRandom</tt> returns
     *         <tt>false</tt>
     */
    RowAccess getRowAccess() throws IOException;

    /**
     * Returns an object which can iterate over all the rows in the table,
     * but which may also be requested to split recursively
     * for potentially parallel processing.
     *
     * <p>The return value must be non-null, and may provide splitting
     * arrangements specially appropriate for the implementation.
     * If this table 'wraps' an upstream table, it is usually best to
     * base the implementation on calls to the the upstream
     * <code>getRowSplittable</code> method, so that upstream policy
     * about how to divide up the table is respected.
     * However, implementations without special requirements may return
     * {@link Tables#getDefaultRowSplittable
     *        Tables.getDefaultRowSplittable}<code>(this)</code>.
     *
     * @return   new RowSplittable
     * @see  RowRunner
     */
    RowSplittable getRowSplittable() throws IOException;

    /**
     * Indicates whether random access is provided by this table.
     * Only if the result is <tt>true</tt> may the {@link #getRowAccess},
     * {@link #getRow} and {@link #getCell} methods be used.
     *
     * @return  <tt>true</tt> if table random access methods are available
     */
    boolean isRandom();

    /**
     * Returns the contents of a given table cell.  
     * The class of the returned object should be the same as, 
     * or a subclass of, the class returned by
     * <tt>getColumnInfo(icol).getContentClass()</tt>.
     *
     * <p>This method is safe for concurrent use from multiple threads,
     * but in general it is recommended to use a
     * {@link #getRowAccess RowAccess} instead.
     *
     * @param  irow  the index of the cell's row
     * @param  icol  the index of the cell's column
     * @return  the contents of this cell
     * @throws IOException  if there is an error reading the data
     * @throws UnsupportedOperationException  if <tt>isRandom</tt> returns
     *         <tt>false</tt>
     */
    Object getCell( long irow, int icol ) throws IOException;

    /**
     * Returns the contents of a given table row.  The returned value is
     * equivalent to an array formed of all the objects returned by 
     * <tt>getCell(irow,icol)</tt> for all the columns <tt>icol</tt> 
     * in sequence.
     *
     * <p>This method is safe for concurrent use from multiple threads,
     * but in general it is recommended to use a
     * {@link #getRowAccess RowAccess} instead.
     *
     * @param  irow  the index of the row to retrieve
     * @return  an array of the objects in each cell in row <tt>irow</tt>
     * @throws IOException  if there is an error reading the data
     * @throws UnsupportedOperationException  if <tt>isRandom</tt> returns
     *         <tt>false</tt>
     */
    Object[] getRow( long irow ) throws IOException;

    /**
     * Relinquishes any resources associated with this table.
     * This may do nothing, and calling it is often not required,
     * but it provides an opportunity to force release of file
     * descriptors or other resources that are not well handled by
     * garbage collection if the table itself holds them.
     * It is not intended for release of heap-based resources,
     * for which garbage collection already provides adequate management.
     *
     * <p>Following a call to this method, any attempt to use this table
     * or objects such as RowSequences obtained from it will result
     * in undefined behaviour.
     */
    public void close() throws IOException;
}
