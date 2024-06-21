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
     * Constructs a <code>ColumnInfo</code> object.
     *
     * @param  name  the name of the column
     */
    public ColumnInfo( String name ) {
        super( name );
    }

    /**
     * Constructs a new <code>ColumnInfo</code> based on
     * a <code>ValueInfo</code> object.
     * All attributes are copied from the template to the new object.
     *
     * @param  base  the template <code>ValueInfo</code>
     */
    public ColumnInfo( ValueInfo base ) {
        super( base );
    }

    /**
     * Constructs a new <code>ColumnInfo</code> object with a given name,
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
     * Currently this just calls
     * <code>getAuxDatumByName(vinfo.getName())</code>,
     * but may be revised in future to match on other attributes.
     *
     * @param  vinfo  the data item to match
     * @return  a <code>DescribedValue</code> object representing the 
     *          auxiliary metadata item matching <code>vinfo</code>
     *          for this column, or <code>null</code> if none exists
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
     * the given type <code>clazz</code>.
     *
     * @param  vinfo  the data item to match
     * @param  clazz  required return type
     * @return  value of the auxiliary metadata item matching 
     *          <code>vinfo</code> for this 
     *          column if it exists and is an instance of <code>clazz</code>
     *          or one of its subtypes, otherwise <code>null</code>
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
     * and if its value is an instance of the given type <code>clazz</code>.
     *
     * @param  name  the name of an auxiliary metadata item
     * @param  clazz  required return type
     * @return  value of the auxiliary metadata item matching 
     *          <code>vinfo</code> for this column if it exists and is an
     *          instance of <code>clazz</code> or one of its subtypes, 
     *          otherwise <code>null</code>
     */
    public <T> T getAuxDatumValueByName( String name, Class<T> clazz ) {
        DescribedValue dval = getAuxDatumByName( name );
        return dval == null ? null : dval.getTypedValue( clazz );
    }
}
