package uk.ac.starlink.table;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple modifiable StarTable implementation.
 * It has a fixed number of columns and a variable number of rows;
 * rows can be added, removed and modified.
 * <p>
 * The current implementation stores the data in a List of Object[] arrays - 
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

    private final List<Object[]> rows_;
    private final ColumnInfo[] colInfos_;

    /**
     * Constructs a new RowListStarTable specifying the columns that it
     * will contain.
     *
     * @param colInfos array of objects defining the columns of the table
     */
    public RowListStarTable( ColumnInfo[] colInfos ) {
        rows_ = new ArrayList<Object[]>();
        colInfos_ = colInfos.clone();
    }

    /**
     * Constructs a new RowListStarTable with its column and table metadata
     * copied from an existing table.  The data of the <code>template</code>
     * is ignored.
     *
     * @param  template  template table supplying column and table metadata
     */
    @SuppressWarnings("this-escape")
    public RowListStarTable( StarTable template ) {
        this( Tables.getColumnInfos( template ) );
        setName( template.getName() );
        setParameters( new ArrayList<DescribedValue>( template
                                                     .getParameters() ) );
    }

    public long getRowCount() {
        return rows_.size();
    }

    public int getColumnCount() {
        return colInfos_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public Object getCell( long lrow, int icol ) {
        return getRow( lrow )[ icol ];
    }

    public Object[] getRow( long lrow ) {
        int irow = checkedLongToInt( lrow );
        return rows_.get( irow );
    }

    /**
     * Sets the value of a given cell in the table.
     * <code>value</code> has to have a class compatible with its column.
     * 
     * @param  lrow  row index
     * @param  icol  column index
     * @param  value  new value for the cell at
     *                <code>lrow</code>, <code>icol</code>
     * @throws IllegalArgumentException  if <code>value</code> is not
     *         compatible with column <code>icol</code>
     */
    public void setCell( long lrow, int icol, Object value ) {
        validateCell( icol, value );
        getRow( lrow )[ icol ] = value;
    }

    /**
     * Sets the value of a given row in the table.
     * Overwrites the existing values of the cells in that row.
     * <code>values</code> has to have the same number of elements as there
     * are columns in this table, and its elements have to have classes
     * compatible with the table columns.
     *
     * @param  lrow  row index
     * @param  values  new values for the cells in row <code>lrow</code> 
     * @throws IllegalArgumentException   if <code>values</code> has the wrong
     *         number of elements or they are of the wrong class
     */
    public void setRow( long lrow, Object[] values ) {
        validateRow( values );
        int irow = checkedLongToInt( lrow );
        rows_.set( irow, values );
    }

    /**
     * Adds a new row to the end of the table.
     * <code>values</code> has to have the same number of elements as there
     * are columns in this table, and its elements have to have classes
     * compatible with the table columns.
     *
     * @param  values  values for the cells in the new row
     * @throws IllegalArgumentException   if <code>values</code> has the wrong
     *         number of elements or they are of the wrong class
     */
    public void addRow( Object[] values ) {
        validateRow( values );
        rows_.add( values );
    }

    /**
     * Adds a new row in the middle of the table.
     * Rows after <code>lrow</code> will be shoved down by one.
     * <code>values</code> has to have the same number of elements as there
     * are columns in this table, and its elements have to have classes
     * compatible with the table columns.
     *
     * @param  lrow    row index for the new row
     * @param  values  values for the cells in the new row
     * @throws IllegalArgumentException   if <code>values</code> has the wrong
     *         number of elements or they are of the wrong class
     */
    public void insertRow( long lrow, Object[] values ) {
        validateRow( values );
        int irow = checkedLongToInt( lrow );
        rows_.add( irow, values );
    }

    /**
     * Removes an existing row from the table.
     * Rows after <code>lrow</code> will be moved up by one.
     *
     * @param  lrow  index of the row to remove
     */
    public void removeRow( long lrow ) {
        int irow = checkedLongToInt( lrow );
        rows_.remove( irow );
    }

    /**
     * Removes all rows from the table.
     */
    public void clearRows() {
        rows_.clear();
    }

    /**
     * Throws an unchecked exception if <code>values</code> is not a suitable
     * element of the <code>rows</code> array.
     *
     * @param  values  potential new row
     * @throws IllegalArgumentException   if <code>values</code> is no good
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
     * Throws an unchecked exception if <code>value</code> is not a suitable
     * element for column <code>icol</code>.
     *
     * @param  icol  column index
     * @param  value  potential new value for column <code>icol</code>
     * @throws IllegalArgumentException  if <code>value</code> is no good
     */
    void validateCell( int icol, Object value ) {
        if ( value != null ) {
            Class<?> valClazz = value.getClass();
            Class<?> colClazz = colInfos_[ icol ].getContentClass();
            if ( ! colClazz.isAssignableFrom( valClazz ) ) {
                throw new IllegalArgumentException(
                    "Value class incompatible with column: " +
                    value + " is " + valClazz + " not " + colClazz );
            }
        }
    }
}
