package uk.ac.starlink.table;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A column which can be initialised from a given existing column, but
 * whose cells can be written to.  Whether the initialisation is done
 * by copying the whole lot and creating a new column or by 
 * just keeping track of the changed elements is unspecified and down
 * to the implementation.
 * Modifications to the cell data or the metadata of this column will
 * not affect the data/metadata of the base column.
 *
 * @author   Mark Taylor (Starlink)
 */
public class EditableColumn extends WrapperColumn {

    private final ColumnData base;
    private final Map changedEntries = new HashMap();

    /**
     * Constructs a new EditableColumn based on an existing column.
     */
    public EditableColumn( ColumnData base ) {
        super( base );
        this.base = base;

        /* Arrange for a private copy of the ColumnInfo in this object
         * rather than a reference to that of the base. */
        setColumnInfo( new ColumnInfo( base.getColumnInfo() ) );
    }

    /**
     * Returns <tt>true</tt>.
     *
     * @return  whether cells can be edited.  They can.
     */
    public boolean isWritable() {
        return true;
    }

    public Object readValue( long irow ) throws IOException {
        Object key = new Long( irow );
        return changedEntries.containsKey( key ) ? changedEntries.get( key )
                                                 : base.readValue( irow );
    }

    public void storeValue( long irow, Object value ) throws IOException {
        if ( value == null && ! getColumnInfo().isNullable() ) {
            throw new NullPointerException( 
                "Nulls not permitted in column " + this );
        }
        if ( value != null && ! getColumnInfo().getContentClass()
                               .isAssignableFrom( value.getClass() ) ) {
            throw new ArrayStoreException(
                "Value " + value + " is a " + value.getClass() + " not a " + 
                getColumnInfo().getContentClass() );
        }
        Object key = new Long( irow );
        changedEntries.put( key, value );
    }
}
