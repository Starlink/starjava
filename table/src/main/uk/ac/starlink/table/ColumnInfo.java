package uk.ac.starlink.table;

/**
 * Contains information about a table column.
 * This really does the same thing as its superclass,
 * {@link DefaultValueInfo}, but for historical reasons it contains
 * some additional methods for access to the
 * auxiliary metadata items.
 * In earlier versions of the library, columns were allowed to store
 * auxiliary metadata and non-column items (like table parameters)
 * were not, but now they have the same capabilities.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ColumnInfo extends DefaultValueInfo {

    /**
     * Constructs a <tt>ColumnInfo</tt> object.
     *
     * @param  name  the name of the column
     */
    public ColumnInfo( String name ) {
        super( name );
    }

    /**
     * Constructs a new <tt>ColumnInfo</tt> based on a <tt>ValueInfo</tt>
     * object.  All attributes are copied from the template to the new
     * object.
     *
     * @param  base  the template <tt>ValueInfo</tt>
     */
    public ColumnInfo( ValueInfo base ) {
        super( base );
    }

    /**
     * Constructs a new <tt>ColumnInfo</tt> object with a given name,
     * class and description.
     *
     * @param  name  the name applying to described values
     * @param  contentClass  the class of which described values should be
     *         instances
     * @param  description  a textual description of the described values
     */
    public ColumnInfo( String name, Class<?> contentClass,
                      String description ) {
        super( name, contentClass, description );
    }

    /**
     * Gets an item of auxiliary metadata from its specification.
     * Currently this just calls <tt>getAuxDatumByName(vinfo.getName())</tt>,
     * but may be revised in future to match on other attributes.
     *
     * @param  vinfo  the data item to match
     * @return  a <tt>DescribedValue</tt> object representing the 
     *          auxiliary metadata item matching <tt>vinfo</tt> for this column,
     *          or <tt>null</tt> if none exists
     */
    public DescribedValue getAuxDatum( ValueInfo vinfo ) {
        return getAuxDatumByName( vinfo.getName() );
    }

    /**
     * Gets the value of an item of auxiliary metadata using its specification,
     * requiring a particular return type.
     * This convenience method works like {@link #getAuxDatum} 
     * but returns a non-null value
     * only if the named item exists and if its value is an instance of
     * the given type <tt>clazz</tt>.
     *
     * @param  vinfo  the data item to match
     * @param  clazz  required return type
     * @return  value of the auxiliary metadata item matching 
     *          <tt>vinfo</tt> for this 
     *          column if it exists and is an instance of <tt>clazz</tt> or
     *          one of its subtypes, otherwise <tt>null</tt>
     */
    public <T> T getAuxDatumValue( ValueInfo vinfo, Class<T> clazz ) {
        DescribedValue dval = getAuxDatum( vinfo );
        return dval == null ? null : dval.getTypedValue( clazz );
    }

    /**
     * Gets the value of an item of auxiliary metadata by its name,
     * requiring a particular return type.
     * This convenience method works like {@link #getAuxDatumByName},
     * but returns a non-null value only if the named item exists, 
     * and if its value is an instance of the given type <tt>clazz</tt>.
     *
     * @param  name  the name of an auxiliary metadata item
     * @param  clazz  required return type
     * @return  value of the auxiliary metadata item matching 
     *          <tt>vinfo</tt> for this column if it exists and is an
     *          instance of <tt>clazz</tt> or one of its subtypes, 
     *          otherwise <tt>null</tt>
     */
    public <T> T getAuxDatumValueByName( String name, Class<T> clazz ) {
        DescribedValue dval = getAuxDatumByName( name );
        return dval == null ? null : dval.getTypedValue( clazz );
    }
}
