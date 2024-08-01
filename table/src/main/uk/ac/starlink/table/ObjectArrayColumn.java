package uk.ac.starlink.table;

/**
 * A column which provides data storage in a java array of objects.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ObjectArrayColumn extends ArrayColumn {

    private final Object[] data;

    /**
     * Constructs a new column backed by a given array of objects.
     * The <code>contentClass</code> of the given base column info must
     * be compatible with the supplied data array;
     * it should be the class of what the array is an array of.
     * Alternatively, the <code>base</code> column info may have a
     * <code>null</code> content class, in which case the column info for
     * the new column will be set appropriately from the data array.
     *
     * @param  base  the column info on which to base this column's info
     * @param  data  an array of primitives or objects which will form
     *         the storage for this column
     * @throws  IllegalArgumentException if <code>data</code> isn't an array or
     *          <code>base.getContentClass()</code> is incompatible with
     *          <code>data</code>
     */
    @SuppressWarnings("this-escape")
    public ObjectArrayColumn( ColumnInfo base, Object[] data ) {
        super( base, data );
        this.data = data;
        if ( getColumnInfo().getContentClass() == null ) {
            getColumnInfo().setContentClass( data.getClass()
                                                 .getComponentType() );
        }
    }

    void storeValue( int irow, Object val ) {
        data[ irow ] = val;
    }

    Object readValue( int irow ) {
        return data[ irow ];
    }
}
