package uk.ac.starlink.table;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple modifiable StarTable implementation.
 * It has a fixed number of columns and a variable number of rows;
 * rows can be added, removed and modified.
 * <p>
 * The current implementation stores the data List of Object[] arrays - 
 * each list element contains the cells of one row of the table.
 * Thus currently you can't store more than Integer.MAX_VALUE rows.
 * <p>
 * Some validation is performed when objects are inserted into the
 * table, but it is possible to subvert this - the table itself can't
 * guarantee that its data structures represent a legal table.
 *
 * @author   Mark Taylor (Starlink)
 */
public class RowListStarTable extends RandomStarTable {

    private final List rows;
    private final ColumnInfo[] colInfos;

    /**
     * Constructs a new RowListStarTable specifying the columns that it
     * will contain.
     *
     * @param colInfos array of objects defining the columns of the table
     */
    public RowListStarTable( ColumnInfo[] colInfos ) {
        this.rows = new ArrayList();
        this.colInfos = (ColumnInfo[]) colInfos.clone();
    }

    /**
     * Constructs a new RowListStarTable with its column and table metadata
     * copied from an existing table.  The data of the <tt>template</tt>
     * is ignored.
     *
     * @param  template  template table supplying column and table metadata
     */
    public RowListStarTable( StarTable template ) {
        this( Tables.getColumnInfos( template ) );
        setName( template.getName() );
        setParameters( new ArrayList( template.getParameters() ) );
    }

    public long getRowCount() {
        return rows.size();
    }

    public int getColumnCount() {
        return colInfos.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos[ icol ];
    }

    public Object getCell( long lrow, int icol ) {
        return getRow( lrow )[ icol ];
    }

    public Object[] getRow( long lrow ) {
        int irow = checkedLongToInt( lrow );
        return (Object[]) rows.get( irow );
    }

    /**
     * Sets the value of a given cell in the table.
     * <tt>value</tt> has to have a class compatible with its column.
     * 
     * @param  lrow  row index
     * @param  icol  column index
     * @param  value  new value for the cell at <tt>lrow</tt>, <tt>icol</tt>
     * @throws IllegalArgumentException  if <tt>value</tt> is not compatible
     *         with column <tt>icol</tt>
     */
    public void setCell( long lrow, int icol, Object value ) {
        validateCell( icol, value );
        getRow( lrow )[ icol ] = value;
    }

    /**
     * Sets the value of a given row in the table.
     * Overwrites the existing values of the cells in that row.
     * <tt>values</tt> has to have the same number of elements as there
     * are columns in this table, and its elements have to have classes
     * compatible with the table columns.
     *
     * @param  lrow  row index
     * @param  values  new values for the cells in row <tt>lrow</tt> 
     * @throws IllegalArgumentException   if <tt>values</tt> has the wrong
     *         number of elements or they are of the wrong class
     */
    public void setRow( long lrow, Object[] values ) {
        validateRow( values );
        int irow = checkedLongToInt( lrow );
        rows.set( irow, values );
    }

    /**
     * Adds a new row to the end of the table.
     * <tt>values</tt> has to have the same number of elements as there
     * are columns in this table, and its elements have to have classes
     * compatible with the table columns.
     *
     * @param  values  values for the cells in the new row
     * @throws IllegalArgumentException   if <tt>values</tt> has the wrong
     *         number of elements or they are of the wrong class
     */
    public void addRow( Object[] values ) {
        validateRow( values );
        rows.add( values );
    }

    /**
     * Adds a new row in the middle of the table.
     * Rows after <tt>lrow</tt> will be shoved down by one.
     * <tt>values</tt> has to have the same number of elements as there
     * are columns in this table, and its elements have to have classes
     * compatible with the table columns.
     *
     * @param  lrow    row index for the new row
     * @param  values  values for the cells in the new row
     * @throws IllegalArgumentException   if <tt>values</tt> has the wrong
     *         number of elements or they are of the wrong class
     */
    public void insertRow( long lrow, Object[] values ) {
        validateRow( values );
        int irow = checkedLongToInt( lrow );
        rows.add( irow, values );
    }

    /**
     * Removes an existing row from the table.
     * Rows after <tt>lrow</tt> will be moved up by one.
     *
     * @param  lrow  index of the row to remove
     */
    public void removeRow( long lrow ) {
        int irow = checkedLongToInt( lrow );
        rows.remove( irow );
    }

    /**
     * Removes all rows from the table.
     */
    public void clearRows() {
        rows.clear();
    }

    /**
     * Throws an unchecked exception if <tt>values</tt> is not a suitable
     * element of the <tt>rows</tt> array.
     *
     * @param  values  potential new row
     * @throws IllegalArgumentException   if <tt>values</tt> is no good
     */
    void validateRow( Object[] values ) {
        int ncol = getColumnCount();
        if ( values.length != ncol ) {
            throw new IllegalArgumentException( 
                "Row has wrong number of columns" +
                " (" + values.length + " not " + ncol + ")" );
        }
        for ( int icol = 0; icol < ncol; icol++ ) {
            validateCell( icol, values[ icol ] );
        }
    }

    /**
     * Throws an unchecked exception if <tt>value</tt> is not a suitable
     * element for column <tt>icol</tt>.
     *
     * @param  icol  column index
     * @param  value  potential new value for column <tt>icol</tt>
     * @throws IllegalArgumentException  if <tt>value</tt> is no good
     */
    void validateCell( int icol, Object value ) {
        if ( value != null ) {
            Class valClazz = value.getClass();
            Class colClazz = colInfos[ icol ].getContentClass();
            if ( ! colClazz.isAssignableFrom( valClazz ) ) {
                throw new IllegalArgumentException(
                    "Value class incompatible with column: " +
                    value + " is " + valClazz + " not " + colClazz );
            }
        }
    }
}
