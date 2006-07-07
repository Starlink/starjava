package uk.ac.starlink.table;

/**
 * Represents a column with metadata but no data.  Every cell returns 
 * null, and is not writable.
 *
 * @author   Mark Taylor (Starlink)
 * @since    28 Oct 2004
 */
public class BlankColumn extends ColumnData {

    /**
     * Constructs a new column based on a given column metadata object.
     *
     * @param  colinfo  metadata for column
     */
    public BlankColumn( ColumnInfo colinfo ) {
        super( colinfo );
    }

    /**
     * Returns column value, which is always null.
     *
     * @param   irow  row index
     * @return  null
     */
    public Object readValue( long irow ) {
        return null;
    }
}
