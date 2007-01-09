package uk.ac.starlink.table;

/**
 * Contains a value (an <tt>Object</tt>) as well as a 
 * {@link ValueInfo} object which provides metadata about that value
 * (name, class, shape, units and so on). 
 *
 * @author   Mark Taylor (Starlink)
 */
public class DescribedValue {

    private final ValueInfo vinfo;
    private Object value;

    /**
     * Constructs a new <tt>DescribedValue</tt> object to hold values
     * described by a given <tt>ValueInfo</tt> object.
     *
     * @param  vinfo  the metadata handler for this value
     */
    public DescribedValue( ValueInfo vinfo ) {
        this.vinfo = vinfo;
    }

    /**
     * Constructs a new <tt>DescribedValue</tt> object to hold values
     * described by a given <tt>ValueInfo</tt> object and with a
     * given initial value.
     *
     * @param  vinfo  the metadata describing this object's value
     * @param  value  the value of this object
     * @throws  IllegalArgumentException  if <tt>value.getClass()</tt>
     *          is not compatible with <tt>vinfo.getContentClass()</tt>
     */
    public DescribedValue( ValueInfo vinfo, Object value ) {
        this( vinfo );
        setValue( value );
    }

    /**
     * Returns the <tt>ValueInfo</tt> object which describes the value
     * held by this object.
     *
     * @return  the metadata describing this object's value
     */
    public ValueInfo getInfo() {
        return vinfo;
    }

    /**
     * Sets the actual value content of this object.
     *
     * @param   value  the value
     * @throws  IllegalArgumentException  if <tt>value.getClass()</tt> is not
     *          compatible with <tt>getValueInfo().getContentClass()</tt>
     */
    public void setValue( Object value ) {
        Class cclass = vinfo.getContentClass();
        if ( cclass == null ) {
            throw new IllegalArgumentException(
                "ValueInfo " + vinfo + " has no contentClass set" );
        }
        else if ( value != null && ! cclass.isInstance( value ) ) {
            throw new IllegalArgumentException( 
                "Value " + value + " is not a " + cclass.getName() );
        }
        this.value = value;
    }

    /**
     * Returns the actual value content of this object.
     *
     * @return  the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Returns a string representation of the value of this object, 
     * no longer than a given maximum length.
     *
     * @param   maxLength  the maximum number of characters in the returned
     *          string
     */
    public String getValueAsString( int maxLength ) {
        return vinfo.formatValue( getValue(), maxLength );
    }

    /**
     * Sets the value of this object from a string representation.
     *
     * @param  sval string representation of the new value
     */
    public void setValueFromString( String sval ) {
        setValue( vinfo.unformatString( sval ) );
    }

    /**
     * Returns a string representation of this object, no longer than a
     * given maximum length.  The result indicates the object's
     * name, class, shape and value.
     *
     * @param   maxLength  the maximum number of characters in the returned
     *          string
     * @return  a string representation of this object
     */
    public String toString( int maxLength ) {
        StringBuffer buf = new StringBuffer( vinfo.toString() );
        buf.append( "=" )
           .append( getValueAsString( maxLength - buf.length() ) );
        if ( buf.length() > maxLength ) {
            buf.setLength( maxLength - 3 );
            buf.append( "..." );
        }
        return buf.toString();
    }

    /**
     * Returns a string representation of this object no longer than a 
     * default maximum length.  The result indictes the object's 
     * name, class, shape and value.
     *
     * @return  a string representation of this object
     */
    public String toString() {
        return toString( 70 );
    }

}
