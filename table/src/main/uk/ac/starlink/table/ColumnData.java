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
     * Constructs a new <tt>ColumnData</tt> with metadata supplied by a 
     * given <tt>ColumnInfo</tt> object.
     *
     * @param  colinfo  the column metadata
     */
    public ColumnData( ColumnInfo colinfo ) {
        this.colinfo = colinfo;
    }

    /**
     * Performs <tt>ColumnData</tt> initialisation based on template
     * <tt>ValueInfo</tt> object.
     *
     * @param  base  template
     */
    public ColumnData( ValueInfo base ) {
        this( new ColumnInfo( base ) );
    }

    /**
     * Returns the <tt>ColumnInfo</tt> which provides the metadata for this 
     * column.
     *
     * @return  the column metadata
     */
    public ColumnInfo getColumnInfo() {
        return colinfo;
    }

    /**
     * Sets the <tt>ColumnInfo</tt> which provides the metadata for this
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
     * @return  the value stored at <tt>irow</tt>
     * @throws  IOException  if there is some problem reading
     */
    public abstract Object readValue( long irow ) throws IOException;

    /**
     * Stores a given value in a given row for this column.
     * Will only work if the <tt>isWritable</tt> method returns true.
     * The implementation in the <tt>ColumnData</tt> class throws
     * an <tt>UnsupportedOperationException</tt>.
     *
     * @param   val  the object to store
     * @param   irow  the row to store it in
     * @throws  UnsupportedOperationException  if !{@link #isWritable}
     * @throws  NullPointerException  if <tt>val==null</tt> and 
     *          this column is not nullable
     * @throws  ArrayStoreException  if <tt>val</tt> is not compatible
     *          with the content class of this column
     * @throws  IOException  if there is some problem writing
     */
    public void storeValue( long irow, Object val ) throws IOException {
        throw new UnsupportedOperationException( "Not writable" );
    }

    /**
     * Indicates whether this object can store values.
     * The implementation in the <tt>ColumnData</tt> class returns 
     * <tt>false</tt>
     *
     * @return  true  iff {@link #storeValue} can be used 
     */
    public boolean isWritable() {
        return false;
    }
}
