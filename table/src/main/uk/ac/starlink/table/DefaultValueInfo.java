package uk.ac.starlink.table;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of the <code>ValueInfo</code> interface.
 * Additionally provides mutator methods for the accessors defined in 
 * <code>ValueInfo</code>.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DefaultValueInfo implements ValueInfo {

    private String name_;
    private String unitString_;
    private String ucd_;
    private String utype_;
    private String xtype_;
    private String description_;
    private Class<?> contentClass_;
    private DomainMapper[] domainMappers_;
    private boolean isNullable_;
    private int[] shape_;
    private int elementSize_;
    private List<DescribedValue> auxData_;
    private Function<String,?> fromString_;

    private static Pattern trailDigits = Pattern.compile( "\\.([0-9]+)$" );
    private static Pattern trailSpaces = Pattern.compile( "( +)$" );

    /**
     * Constructs a new generic <code>DefaultValueInfo</code> object
     * without a name.
     */
    public DefaultValueInfo() {
        this( (String) null );
    }

    /**
     * Constructs a new generic <code>DefaultValueInfo</code> object 
     * with a given name.
     *
     * @param  name  the name applying to described values
     */
    public DefaultValueInfo( String name ) {
        this( name, Object.class );
    }

    /**
     * Constructs a new <code>DefaultValueInfo</code> object with a given
     * name and class.
     *
     * @param  name  the name applying to described values
     * @param  contentClass  the class of which described values should be
     *         instances
     */
    public DefaultValueInfo( String name, Class<?> contentClass ) {
        this( name, contentClass, "" );
    }

    /**
     * Constructs a new <code>DefaultValueInfo</code> object with a given
     * name, class and description.
     *
     * @param  name  the name applying to described values
     * @param  contentClass  the class of which described values should be
     *         instances
     * @param  description  a textual description of the described values
     */
    public DefaultValueInfo( String name, Class<?> contentClass,
                             String description ) {
        name_ = name;
        description_ = description;
        domainMappers_ = new DomainMapper[ 0 ];
        isNullable_ = true;
        shape_ = new int[] { -1 };
        elementSize_ = -1;
        auxData_ = new ArrayList<DescribedValue>();
        configureContentClass( contentClass );
    }

    /**
     * Constructs a DefaultValueInfo object which is a copy of an existing one.
     * The fields of the new object are copies (where possible not 
     * references to) those of the base one.
     *
     * @param   base  the object to copy
     */
    public DefaultValueInfo( ValueInfo base ) {
        this( base.getName(), base.getContentClass(), base.getDescription() );
        unitString_ = base.getUnitString();
        ucd_ = base.getUCD();
        utype_ = base.getUtype();
        xtype_ = base.getXtype();
        shape_ = base.getShape() == null ? null : base.getShape().clone();
        elementSize_ = base.getElementSize();
        isNullable_ = base.isNullable();
        domainMappers_ = base.getDomainMappers() == null
                       ? null
                       : base.getDomainMappers().clone();
        auxData_ = new ArrayList<DescribedValue>( base.getAuxData() );
    }

    /**
     * Sets the name for this object.
     *
     * @param   name  the name
     */
    public void setName( String name ) {
        name_ = name;
    }

    public String getName() {
        return name_;
    }

    /**
     * Sets the string representing the units for the values described by
     * this object.
     *
     * @param  unitString  a string giving the units, or <code>null</code> if
     *         units are unknown
     */
    public void setUnitString( String unitString ) {
        unitString_ = unitString;
    }

    public String getUnitString() {
        return unitString_;
    }

    /**
     * Sets the Unified Content Descriptor string applying to values
     * described by this object.
     *
     * @param  ucd  the UCD, or <code>null</code> if none is known
     */
    public void setUCD( String ucd ) {
        ucd_ = ucd;
    }

    public String getUCD() {
        return ucd_;
    }

    /**
     * Sets the Utype string applying to values described by this object.
     *
     * @param  utype  the Utype, or <code>null</code> if none is known
     */
    public void setUtype( String utype ) {
        utype_ = utype;
    }

    public String getUtype() {
        return utype_;
    }

    /**
     * Sets the Xtype string applying to values described by this object.
     *
     * @param  xtype  the Xtype, or <code>null</code> if none is known
     */
    public void setXtype( String xtype ) {
        xtype_ = xtype;
    }

    public String getXtype() {
        return xtype_;
    }

    /**
     * Sets a textual description of the values described by this object.
     *
     * @param  description  a texttual description of this column, 
     *         or the empty string "" if there is nothing to be said
     */
    public void setDescription( String description ) {
        description_ = description;
    }

    public String getDescription() {
        return description_;
    }

    public Class<?> getContentClass() {
        return contentClass_;
    }

    /**
     * Sets the java class of objects contained in this column.
     *
     * @param  contentClass  the class of items in this column
     * @throws  IllegalArgumentException if <code>contentClass</code>
     *          is primitive
     */
    public void setContentClass( Class<?> contentClass ) {
        configureContentClass( contentClass );
    }

    /**
     * Does the work for setting the content class of this object.
     * This is a final method that is safe to call from the constructor.
     *
     * @param  contentClass  the class of items in this column
     * @throws  IllegalArgumentException if <code>contentClass</code>
     *                                   is primitive
     */
    private final void configureContentClass( Class<?> contentClass ) {
        if ( contentClass.isPrimitive() ) {
            throw new IllegalArgumentException( 
                "Primitive content class " + contentClass + " not permitted" );
        }

        /* Set the class. */
        contentClass_ = contentClass;

        /* If the content class is not an array type, set the shape to null. */
        if ( ! isArray() ) {
            shape_ = null;
        }

        /* If it is an array, ensure that the shape array is consistent
         * with this. */
        else {
            if ( shape_ == null ) {
                shape_ = new int[] { -1 };
            }
        }

        /* Prepare for conversion from string values. */
        fromString_ = createUnformatter( contentClass );
    }

    public boolean isArray() {
        return contentClass_.isArray();
    }

    public int[] getShape() {
        return shape_ == null ? null : shape_.clone();
    }

    /**
     * Sets the shape of values described by this object.
     *
     * @param   shape  the shape
     * @throws  IllegalArgumentException if <code>shape</code>
     *          has elements apart from the last one which are &lt;=0
     */
    public void setShape( int[] shape ) {
        if ( shape != null ) {
            for ( int i = 0; i < shape.length - 1; i++ ) {
                if ( shape[ i ] <= 0 ) {
                    throw new IllegalArgumentException( "Bad shape" );
                }
            }
        }
        shape_ = shape;
    }

    public int getElementSize() {
        return elementSize_;
    }

    /**
     * Sets the element size of values described by this object.
     *
     * @param  size  the element size
     */
    public void setElementSize( int size ) {
        elementSize_ = size;
    }

    public boolean isNullable() {
        return isNullable_;
    }

    /**
     * Sets whether values described by this object may have the value
     * <code>null</code>.
     * By setting this to <code>false</code> you assert that
     * no <code>null</code> objects will be returned from this column.
     *
     * @param  isNullable <code>false</code> if objects in this column are
     *         guaranteed non-<code>null</code>
     */
    public void setNullable( boolean isNullable ) {
        isNullable_ = isNullable;
    }

    public DomainMapper[] getDomainMappers() {
        return domainMappers_;
    }

    /**
     * Sets the domain mappers known for this object.
     *
     * @param  domainMappers  new domain mapper array
     */
    public void setDomainMappers( DomainMapper[] domainMappers ) {
        domainMappers_ = domainMappers;
    }

    /**
     * Returns a list of auxiliary metadata objects
     * pertaining to this column.
     * The returned value may, if mutable, be modified to change
     * the aux data of this object.
     *
     * @return   a List of <code>DescribedValue</code> items
     */
    public List<DescribedValue> getAuxData() {
        return auxData_;
    }

    /**
     * Sets the list of auxiliary metadata items for this column.
     *
     * @param   auxData  a list of <code>DescribedValue</code> objects
     */
    public void setAuxData( List<DescribedValue> auxData ) {
        auxData_ = auxData;
    }

    /**
     * Returns a <code>ValueInfo</code> object which is sufficiently general
     * to cover every object described by either of two given 
     * <code>ValueInfo</code> objects.  For most of the info attributes this
     * entails setting to null any attribute which is not the same for
     * both, though for contentClass it involves finding the most 
     * specific common ancestor class.
     *
     * @param   vi1  one <code>ValueInfo</code> object
     * @param   vi2  the other <code>ValueInfo</code> object
     * @return  a generalised <code>ValueInfo</code> object
     */
    public static ValueInfo generalise( ValueInfo vi1, ValueInfo vi2 ) {

        /* If they are the same, either will do. */
        if ( vi1.equals( vi2 ) ) {
            return vi1;
        }

        /* Otherwise we will need to create a new one with characteristics
         * build up from the supplied ones. */
        DefaultValueInfo vi = new DefaultValueInfo( vi1 ) {
            public String formatValue( Object obj, int leng ) {
                try {
                    return super.formatValue( obj, leng );
                }
                catch ( RuntimeException e ) {
                    String rep = String.valueOf( obj );
                    return rep.length() > leng ? rep.substring( 0, leng )
                                               : rep;
                }
            }
            public Object unformatString( String rep ) {
                try {
                    return super.unformatString( rep );
                }
                catch ( RuntimeException e ) {
                    return null;
                }
            }
        };

        /* Cancel the units if not consistent. */
        if ( vi1.getUnitString() != null &&
             ! vi1.getUnitString().equals( vi2.getUnitString() ) ) {
            vi.setUnitString( null );
        }
     
        /* Cancel the UCDs if not consistent. */
        if ( vi1.getUCD() != null &&
             ! vi1.getUCD().equals( vi2.getUCD() ) ) {
            vi.setUCD( null );
        }

        /* Cancel the descriptions if not consistent. */
        if ( vi1.getDescription() != null &&
             ! vi1.getDescription().equals( vi2.getDescription() ) ) {
            vi.setDescription( "" );
        }

        /* Set the classes to be consistent. */
        Class<?> c2 = vi2.getContentClass();
        vi.setContentClass( Object.class );
        for ( Class<?> c1 = vi1.getContentClass(); c1 != null;
              c1 = c1.getSuperclass() ) {
            if ( c1.isAssignableFrom( c2 ) ) {
                vi.setContentClass( c1 );
                break;
            }
        }

        /* Ensure that the shapes are consistent. */
        if ( vi.isArray() ) {
            int[] s1 = vi1.getShape();
            int[] s2 = vi2.getShape();
            if ( s1 != null && s2 != null ) {
                if ( s1.length != s2.length ) {
                    vi.setShape( new int[] { -1 } );
                }
                else {
                    int ndim = s1.length;
                    boolean same = true;
                    for ( int i = 0; i < ndim - 1 && same; i++ ) {
                        if ( s1[ i ] != s2[ i ] ) {
                            same = false;
                        }
                    }
                    if ( same ) { 
                        if ( ndim > 1 && s1[ ndim - 1 ] != s2[ ndim - 1 ] ) {
                            s1[ ndim - 1 ] = -1;
                        }
                        vi.setShape( s1 );
                    }
                    else {
                        vi.setShape( new int[] { -1 } );
                    }
                }
            }
        }

        /* Ensure that nullability is consistent. */
        if ( vi1.isNullable() || vi2.isNullable() ) {
            vi.setNullable( true );
        }

        /* Return the consistent object. */
        return vi;
    }

    public String formatValue( Object value, int maxLength ) {

        /* If zero length string is required, return that. */
        if ( maxLength <= 0 ) {
            return "";
        }

        /* If it's a null value, return the empty string. */
        if ( Tables.isBlank( value ) ) {
            return "";
        }
    
        /* Scalar value is straightforward. */
        StringBuffer buf = new StringBuffer();
        if ( ! isArray() ) {
            buf.append( value == null ? "" : value.toString() );
        }
    
        /* Otherwise, it's an array, output it in a multidimensional array
         * if necessary. */
        else {
            int[] ashape = getActualShape( shape_, Array.getLength( value ) );
            appendElements( buf, value, 0, ashape, maxLength );
        }
    
        /* Truncate if required. */
        if ( buf.length() > maxLength &&
             ( value instanceof Float || value instanceof Double ) ) {
            Matcher tmatch = trailDigits.matcher( buf );
            if ( tmatch.find() ) {
                int over = buf.length() - maxLength;
                if ( tmatch.group( 1 ).length() > over ) {
                    buf.setLength( maxLength );
                }
            }
        }
        if ( buf.length() > maxLength ) {
            Matcher tmatch = trailSpaces.matcher( buf );
            if ( tmatch.lookingAt() ) {
                int over = buf.length() - maxLength;
                if ( tmatch.group( 1 ).length() > buf.length() - maxLength ) {
                    buf.setLength( maxLength );
                }
            }
        }
        if ( buf.length() > maxLength ) {
            buf.setLength( Math.max( 0, maxLength - 3 ) );
            buf.append( "..." );
        }
    
        /* Return the result. */
        return buf.toString();
    }

    /**
     * Recursive routine for printing out elements of a multidimensional
     * array with brackets in the right place.
     */
    private static int appendElements( StringBuffer sb, Object array,
                                       int pos, int[] dims, int maxChars ) {
        int leng = Array.getLength( array );
        int ndim = dims.length;
        int limit = dims[ ndim - 1 ];
        if ( sb.length() < maxChars ) {
            sb.append( '(' );
            if ( ndim == 1 ) {
                for ( int i = 0; i < limit && sb.length() < maxChars; i++ ) {
                    if ( pos < leng ) {
                        sb.append( Array.get( array, pos++ ) );
                    }
                    if ( i < limit - 1 ) {
                        sb.append( ", " );
                    }
                    else {
                        sb.append( ")" );
                    }
                }
            }
            else {
                int[] subdims = new int[ ndim - 1 ];
                System.arraycopy( dims, 0, subdims, 0, ndim - 1 );
                for ( int i = 0; i < limit && sb.length() < maxChars; i++ ) {
                    pos = appendElements( sb, array, pos, subdims, maxChars );
                    if ( i < limit - 1 ) {
                        sb.append( ", " );
                    }
                    else {
                        sb.append( ")" );
                    }
                }
            }
        }
        return pos;
    }

    /**
     * Returns a human-friendly string indicating the class and shape
     * of a given class object.  This will read something like
     * "Integer" or "byte[][]" or "uk.ac.starlink.FrameSet".
     *
     * @param  clazz  the class
     * @return  a string showing the class and shape of <code>clazz</code>
     */
    public static String formatClass( Class<?> clazz ) {
        String cname = clazz.getName();
        int pos = -1;
        int ndim = 0;
        while ( cname.charAt( ++pos ) == '[' ) {
            ndim++;
        }
        String basename;
        if ( cname.length() == pos + 1 ) {
            switch ( cname.charAt( pos ) ) {
                case 'B':  basename = "byte";
                           break;
                case 'C':  basename = "char";
                           break;
                case 'D':  basename = "double";
                           break;
                case 'F':  basename = "float";
                           break;
                case 'I':  basename = "int";
                           break;
                case 'J':  basename = "long";
                           break;
                case 'S':  basename = "short";
                           break;
                case 'Z':  basename = "boolean";
                           break;
                case 'V':  basename = "void";  // hope not
                           break;
                default:   throw new AssertionError( 
                               "What class is " + cname + "??" );
            }
        }
        else if ( ndim == 0 ) {
            basename = cname;
        }
        else {
            assert cname.charAt( pos ) == 'L';
            assert cname.charAt( cname.length() - 1 ) == ';';
            basename = cname.substring( pos + 1, cname.length() - 1 );
        }
        basename = basename.replaceFirst( "^.*\\.", "" );
        StringBuffer buf = new StringBuffer( basename );
        for ( int i = 0; i < ndim; i++ ) {
            buf.append( "[]" );
        }
        return buf.toString();
    }

    /**
     * Returns a string representing the shape of this object, if it
     * is array-like.  This will look something like "1024,1024,3"
     * or "2,*".
     *
     * @param  shape the shape to format
     * @return  a human-readable representation of the value shape
     * @see    #unformatShape(String)
     */
    public static String formatShape( int[] shape ) {
        if ( shape == null || shape.length == 0 ) {
            return "";
        }
        else {
            StringBuffer buf = new StringBuffer();
            for ( int i = 0; i < shape.length; i++ ) {
                if ( i > 0 ) {
                    buf.append( ',' );
                }
                if ( shape[ i ] <= 0 ) {
                    buf.append( '*' );
                }
                else {
                    buf.append( shape[ i ] );
                }
            }
            return buf.toString();
        }
    }

    /**
     * Takes a stringified version of the shape and turns it into
     * an array.  This performs the opposite of {@link #formatShape(int[])},
     * so values are comma-separated, and the last one only may be "*".
     *
     * @param  txt  string representation of value shape
     * @return   array representation of value shape
     * @throws  IllegalArgumentException if the argument is not in a
     *          comprehensible format
     * @see   #formatShape(int[])
     */
    public static int[] unformatShape( String txt ) {
        if ( txt == null || txt.trim().length() == 0 ) {
            return null;
        }
        String[] els = txt.split( ",", -1 );
        int nel = els.length;
        int[] shape = new int[ nel ];
        for ( int i = 0; i < nel; i++ ) {
            String el = els[ i ].trim();
            try {
                shape[ i ] = i == nel - 1 && "*".equals( el )
                           ? -1
                           : Integer.parseInt( el );
            }
            catch ( NumberFormatException e ) {
                throw new IllegalArgumentException( "Bad shape format: "
                                                  + txt );
            }
        }
        return shape;
    }

    public Object unformatString( String rep ) {
        return rep == null || rep.length() == 0
             ? null
             : fromString_.apply( rep );
    }

    /**
     * Returns a string representation of this object.
     * The result indicates the object's name, class and shape. 
     *
     * @return  a string representation of this object
     */
    public String toString() {
        StringBuffer typeRep = new StringBuffer();
        typeRep.append( formatClass( getContentClass() ) );
        int trlen = typeRep.length();
        if ( typeRep.charAt( trlen - 2 ) == '[' &&
             typeRep.charAt( trlen - 1 ) == ']' ) {
            typeRep.insert( trlen - 1, formatShape( getShape() ) );
        }

        StringBuffer buf = new StringBuffer();
        buf.append( getName() )
           .append( "(" )
           .append( typeRep )
           .append( ")" );
        if ( getUnitString() != null && getUnitString().trim().length() > 0 ) {
            buf.append( "/" )
               .append( getUnitString().trim() );
        }
        return buf.toString();
    }

    /**
     * Returns the shape of an array given a template shape and the number
     * of elements.  This will be the same as the input shape except
     * that if the last element of the input shape array is &lt;=0
     * that value will be adjusted according to the number of elements.
     */
    private static int[] getActualShape( int[] basicShape, int nel ) {
        int[] ashape = basicShape.clone();
        int ndim = ashape.length;
        if ( ashape[ ndim - 1 ] <= 0 ) {
            int slice = 1;
            for ( int i = 0; i < ndim - 1; i++ ) {
                slice *= ashape[ i ];
            }
            ashape[ ndim - 1 ] = ( nel + slice - 1 ) / slice;
        }
        return ashape;
    }

    /**
     * Returns a function that can convert from a string to a given class.
     * The output function has the same output type as the parameterised
     * type of the input class, but that is not not enforced by the
     * signature becuase it's a pain to implement like that.
     *
     * @param  clazz   required function output class
     * @return  function that converts a string to the required class
     */
    private static Function<String,?> createUnformatter( Class<?> clazz ) {
        if ( Boolean.class.equals( clazz ) ) {
            return Boolean::valueOf;
        }
        else if ( Character.class.equals( clazz ) ) {
            return txt -> {
                if ( txt.length() == 1 ) {
                    return Character.valueOf( txt.charAt( 0 ) );
                }
                else if ( txt.trim().length() == 1 ) {
                    return Character.valueOf( txt.trim().charAt( 0 ) );
                }
                else {
                    throw new IllegalArgumentException( "Not char" );
                }
            };
        }
        else if ( Byte.class.equals( clazz ) ) {
            return Byte::valueOf;
        }
        else if ( Short.class.equals( clazz ) ) {
            return Short::valueOf;
        }
        else if ( Integer.class.equals( clazz ) ) {
            return Integer::valueOf;
        }
        else if ( Long.class.equals( clazz ) ) {
            return Long::valueOf;
        }
        else if ( Float.class.equals( clazz ) ) {
            return Float::valueOf;
        }
        else if ( Double.class.equals( clazz ) ) {
            return Double::valueOf;
        }
        else if ( BigInteger.class.equals( clazz ) ) {
            return BigInteger::new;
        }
        else if ( BigDecimal.class.equals( clazz ) ) {
            return BigDecimal::new;
        }
        else if ( String.class.equals( clazz ) ) {
            return Function.identity();
        }
        else if ( Number.class.isAssignableFrom( clazz ) ) {
            return Double::valueOf;
        }
        else {
            return txt -> {
                throw new UnsupportedOperationException( "No unformatter"
                                                       + " available" );
            };
        }
    }
}
