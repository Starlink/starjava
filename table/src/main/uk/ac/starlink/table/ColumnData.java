package uk.ac.starlink.table;

import java.io.IOException;

/**
 * A column which can supply and possibly store cells in array-like storage
 * as well as supply column metadata.
 * Note there is nothing in this class which describes the number of
 * elements it contains (length of the column).  Columns are intended to be 
 * managed by tables, and it is the table which should keep track of
 * this information.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class ColumnData {

    private ColumnInfo colinfo;

    /**
     * Constructs a new ColumnData with no metadata.
     */
    public ColumnData() {
    }

    /**
     * Constructs a new <code>ColumnData</code> with metadata supplied by a 
     * given <code>ColumnInfo</code> object.
     *
     * @param  colinfo  the column metadata
     */
    public ColumnData( ColumnInfo colinfo ) {
        this.colinfo = colinfo;
    }

    /**
     * Performs <code>ColumnData</code> initialisation based on template
     * <code>ValueInfo</code> object.
     *
     * @param  base  template
     */
    public ColumnData( ValueInfo base ) {
        this( new ColumnInfo( base ) );
    }

    /**
     * Returns the <code>ColumnInfo</code> which provides the metadata for this 
     * column.
     *
     * @return  the column metadata
     */
    public ColumnInfo getColumnInfo() {
        return colinfo;
    }

    /**
     * Sets the <code>ColumnInfo</code> which provides the metadata for this
     * column.
     *
     * @param   colinfo  the column metadata
     */
    public void setColumnInfo( ColumnInfo colinfo ) {
        this.colinfo = colinfo;
    }

    /**
     * Returns the value stored at a given row in this column.
     *
     * @param   irow  the row from which to retrieve the value
     * @return  the value stored at <code>irow</code>
     * @throws  IOException  if there is some problem reading
     */
    public abstract Object readValue( long irow ) throws IOException;

    /**
     * Stores a given value in a given row for this column.
     * Will only work if the <code>isWritable</code> method returns true.
     * The implementation in the <code>ColumnData</code> class throws
     * an <code>UnsupportedOperationException</code>.
     *
     * @param   val  the object to store
     * @param   irow  the row to store it in
     * @throws  UnsupportedOperationException  if !{@link #isWritable}
     * @throws  NullPointerException  if <code>val==null</code> and 
     *          this column is not nullable
     * @throws  ArrayStoreException  if <code>val</code> is not compatible
     *          with the content class of this column
     * @throws  IOException  if there is some problem writing
     */
    public void storeValue( long irow, Object val ) throws IOException {
        throw new UnsupportedOperationException( "Not writable" );
    }

    /**
     * Indicates whether this object can store values.
     * The implementation in the <code>ColumnData</code> class returns 
     * <code>false</code>
     *
     * @return  true  iff {@link #storeValue} can be used 
     */
    public boolean isWritable() {
        return false;
    }
}
