package uk.ac.starlink.table;

/**
 * Represents a column which has the same value in every row.
 *
 * @author   Mark Taylor
 * @since    19 Sep 2006
 */
public class ConstantColumn extends ColumnData {

    private final Object value_;

    /**
     * Constructs a new column with a given metadata object and constant
     * datum.
     *
     * @param   colinfo  column metadata
     * @param   value    value to be found in every cell of this column
     */
    public ConstantColumn( ColumnInfo colinfo, Object value ) {
        super( colinfo );
        Class clazz = colinfo.getContentClass();
        if ( value != null &&
             ! clazz.isAssignableFrom( value.getClass() ) ) {
            throw new IllegalArgumentException( value + " is not a "
                                              + clazz.getName() );
        }
        value_ = value;
    }

    public Object readValue( long irow ) {
        return value_;
    }
}
