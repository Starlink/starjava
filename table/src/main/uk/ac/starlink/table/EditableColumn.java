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

    private final ColumnData base_;
    private final Map<Long,Object> changedEntries_ = new HashMap<Long,Object>();

    /**
     * Constructs a new EditableColumn based on an existing column.
     */
    @SuppressWarnings("this-escape")
    public EditableColumn( ColumnData base ) {
        super( base );
        base_ = base;

        /* Arrange for a private copy of the ColumnInfo in this object
         * rather than a reference to that of the base. */
        setColumnInfo( new ColumnInfo( base.getColumnInfo() ) );
    }

    /**
     * Returns <code>true</code>.
     *
     * @return  whether cells can be edited.  They can.
     */
    public boolean isWritable() {
        return true;
    }

    public Object readValue( long irow ) throws IOException {
        Long key = Long.valueOf( irow );
        return changedEntries_.containsKey( key ) ? changedEntries_.get( key )
                                                  : base_.readValue( irow );
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
        Long key = Long.valueOf( irow );
        changedEntries_.put( key, value );
    }
}
