package uk.ac.starlink.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A random-access StarTable that manages its data in columns.
 * The data in each column
 * is managed by a <code>ColumnData</code> object which can be accessed
 * directly using the {@link #getColumnData} method.  Columns can be
 * added and substituted.  If the columns permit it then table cells
 * can be written to as well as read from.
 * <p>
 * Concrete subclasses of this abstract class must implement 
 * {@link #getRowCount}. 
 * If you just need a <code>ColumnStarTable</code> with a fixed number of rows
 * you can use the static convenience method {@link #makeTableWithRows}.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class ColumnStarTable extends RandomStarTable {

    public List<ColumnData> columns_ = new ArrayList<ColumnData>();

    /**
     * Default constructor.
     */
    public ColumnStarTable() {
    }

    /**
     * Initialises a <code>ColumnStarTable</code> using a template 
     * <code>StarTable</code> to provide per-table metadata.
     * The newly constructed object will have
     * copies of the <code>template</code>'s name, parameters etc.
     *
     * @param   template  the template StarTable
     */
    public ColumnStarTable( StarTable template ) {
        setName( template.getName() );
        setParameters( new ArrayList<DescribedValue>( template
                                                     .getParameters() ) );
    }

    /**
     * Gets the number of rows in the table (which must be applicable to
     * all the columns).  Since this is a <code>RandomStarTable</code> the
     * return value must be non-negative.
     *
     * @return  number of rows
     */
    public abstract long getRowCount();

    public int getColumnCount() {
        return columns_.size();
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return getColumnData( icol ).getColumnInfo();
    }

    public Object getCell( long lrow, int icol ) throws IOException {
        return getColumnData( icol ).readValue( lrow );
    }

    /**
     * Stores an object in a given cell of the table.
     *
     * @param  lrow  the row index
     * @param  icol  the column index
     * @param  value  the value to store
     * @throws  IOException if an I/O error occurs
     * @throws  UnsupportedOperationException  if column <code>icol</code>
     *          is not writable
     *          (<code>!getColumnData(icol).isWritable()</code>);
     */
    public void setCell( long lrow, int icol, Object value )
            throws IOException {
        ColumnData coldata = getColumnData( icol );
        if ( coldata.isWritable() ) {
            coldata.storeValue( lrow, value );
        }
        else {
            throw new UnsupportedOperationException( 
                "Column " + coldata + " not writable" );
        }
    }

    /**
     * Returns the <code>ColumnData</code> object for a given column.
     *
     * @param  icol  the index of the column for which the result is required
     * @return the ColumnData for column <code>icol</code>
     */
    public ColumnData getColumnData( int icol ) {
        return columns_.get( icol );
    }

    /**
     * Appends a new column to the end of this model.
     *
     * @param  coldata the new column object to add
     */
    public void addColumn( ColumnData coldata ) {
        columns_.add( coldata );
    }

    /**
     * Substitutes a new column for the one which is currently in a given
     * position.  The old one is discarded.
     *
     * @param  icol  the column index to change
     * @param  coldata  the new column data object
     */
    public void setColumn( int icol, ColumnData coldata ) {
        columns_.set( icol, coldata );
    }

    /**
     * Convenience method to return a <code>ColumnStarTable</code> 
     * with a fixed number of rows.
     * 
     * @param  nrow  the number of rows this table will have
     */
    public static ColumnStarTable makeTableWithRows( final long nrow ) {
        return new ColumnStarTable() {
            public long getRowCount() {
                return nrow;
            }
        };
    }

}
