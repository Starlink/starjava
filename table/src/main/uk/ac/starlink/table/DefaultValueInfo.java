package uk.ac.starlink.table;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.table.TableCellRenderer;
import uk.ac.starlink.table.gui.NumericCellRenderer;

/**
 * Default implementation of the <tt>ValueInfo</tt> interface.
 * Additionally provides mutator methods for the accessors defined in 
 * <tt>ValueInfo</tt>.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DefaultValueInfo implements ValueInfo {

    private String name;
    private String unitString = null;
    private String ucd = null;
    private String description = "";
    private Class contentClass = Object.class;
    private boolean isNullable;
    private int[] shape = new int[] { -1 };

    /**
     * Constructs a new generic <tt>DefaultValueInfo</tt> object
     * without a name.
     */
    public DefaultValueInfo() {
    }

    /**
     * Constructs a new generic <tt>DefaultValueInfo</tt> object 
     * with a given name.
     *
     * @param  name  the name applying to described values
     */
    public DefaultValueInfo( String name ) {
        this.name = name;
    }

    /**
     * Constructs a new <tt>DefaultValueInfo</tt> object with a given
     * name and class.
     *
     * @param  name  the name applying to described values
     * @param  contentClass  the class of which described values should be
     *         instances
     */
    public DefaultValueInfo( String name, Class contentClass ) {
        this( name );
        setContentClass( contentClass );
    }

    /**
     * Constructs a new <tt>DefaultValueInfo</tt> object with a given
     * name, class and description.
     *
     * @param  name  the name applying to described values
     * @param  contentClass  the class of which described values should be
     *         instances
     * @param  description  a textual description of the described values
     */
    public DefaultValueInfo( String name, Class contentClass,
                             String description ) {
        this( name );
        setContentClass( contentClass );
        setDescription( description );
    }

    /**
     * Constructs a DefaultValueInfo object which is a copy of an existing one.
     * The fields of the new object are copies (where possible not 
     * references to) those of the base one.
     *
     * @param   base  the object to copy
     */
    public DefaultValueInfo( ValueInfo base ) {
        this( base.getName() );
        setUnitString( base.getUnitString() );
        setUCD( base.getUCD() );
        setDescription( base.getDescription() );
        setContentClass( base.getContentClass() );
        setShape( base.getShape() );
        setNullable( base.isNullable() );
    }

    /**
     * Sets the name for this object.
     *
     * @param   name  the name
     */
    public void setName( String name ) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Sets the string representing the units for the values described by
     * this object.
     *
     * @param  unitString  a string giving the units, or <tt>null</tt> if
     *         units are unknown
     */
    public void setUnitString( String unitString ) {
        this.unitString = unitString;
    }

    public String getUnitString() {
        return unitString;
    }

    /**
     * Sets the Unified Content Descriptor string applying to values
     * described by this object.
     *
     * @param  ucd  the UCD, or <tt>null</tt> if none is known
     */
    public void setUCD( String ucd ) {
        this.ucd = ucd;
    }

    public String getUCD() {
        return ucd;
    }

    /**
     * Sets a textual description of the values described by this object.
     *
     * @param  description  a texttual description of this column, 
     *         or the empty string "" if there is nothing to be said
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public Class getContentClass() {
        return contentClass;
    }

    /**
     * Sets the java class of objects contained in this column.
     *
     * @param  contentClass  the class of items in this column
     * @throws  IllegalArgumentException if <tt>contentClass</tt> is primitive
     */
    public void setContentClass( Class contentClass ) {
        if ( contentClass.isPrimitive() ) {
            throw new IllegalArgumentException( 
                "Primitive content class " + contentClass + " not permitted" );
        }
        this.contentClass = contentClass;
    }

    public boolean isArray() {
        return contentClass.getComponentType() != null;
    }

    public int[] getShape() {
        return shape == null ? null : (int[]) shape.clone();
    }

    /**
     * Sets the shape of values described by this object.
     *
     * @param   shape  the shape
     * @throws  IllegalArgumentException if <tt>shape</tt> has elements apart
     *          from the last one which are &lt;=0
     */
    public void setShape( int[] shape ) {
        if ( shape != null ) {
            for ( int i = 0; i < shape.length - 1; i++ ) {
                if ( shape[ i ] <= 0 ) {
                    throw new IllegalArgumentException( "Bad shape" );
                }
            }
        }
        this.shape = shape;
    }

    public boolean isNullable() {
        return isNullable;
    }

    /**
     * Sets whether values described by this object may have the value
     * <tt>null</tt>.  By setting this to <tt>false</tt> you assert that
     * no <tt>null</tt> objects will be returned from this column.
     *
     * @param  isNullable <tt>false</tt> if objects in this column are
     *         guaranteed non-<tt>null</tt>
     */
    public void setNullable( boolean isNullable ) {
        this.isNullable = isNullable;
    }

    /**
     * Returns a <tt>ValueInfo</tt> object which is sufficiently general
     * to cover every object described by either of two given 
     * <tt>ValueInfo</tt> objects.  For most of the info attributes this
     * entails setting to null any attribute which is not the same for
     * both, though for contentClass it involves finding the most 
     * specific common ancestor class.
     *
     * @param   vi1  one <tt>ValueInfo</tt> object
     * @param   vi2  the other <tt>ValueInfo</tt> object
     * @return  a generalised <tt>ValueInfo</tt> object
     */
    public static DefaultValueInfo generalise( ValueInfo vi1, ValueInfo vi2 ) {
        DefaultValueInfo vi = new DefaultValueInfo( vi1 );

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
        Class c2 = vi2.getContentClass();
        vi.setContentClass( Object.class );
        for ( Class c1 = vi1.getContentClass(); c1 != null;
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

    /**
     * Returns a custom renderer for the numeric wrapper types, and 
     * <tt>null</tt> for others.  Subclasses should override this method
     * if they can format their values in a component better than
     * allowing the <tt>formatValue</tt> text to be put into a cell.
     *
     * @return  a custom renderer or <tt>null</tt>, depending on the value
     *          of <tt>getContentClass</tt>
     */
    public TableCellRenderer getCellRenderer() {
        Class clazz = getContentClass();
        if ( Number.class.isAssignableFrom( clazz ) ) {
            return new NumericCellRenderer( clazz );
        }
        else {
            return null;
        }
    }

    public String formatValue( Object value, int maxLength ) {

        /* If zero length string is required, return that. */
        if ( maxLength <= 0 ) {
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
            int[] ashape = getActualShape( shape, Array.getLength( value ) );
            appendElements( buf, value, 0, ashape, maxLength );
        }
    
        /* Truncate if required. */
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
     * of a given ValueInfo object.  This will read something like
     * "Integer" or "byte[][]" or "uk.ac.starlink.FrameSet".
     *
     * @param  info  the <tt>ValueInfo</tt> object whose class is required
     * @return  a string showing the class and shape of <tt>info</tt>
     */
    public static String formatClass( Class clazz ) {
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
        basename.replaceFirst( "^java.lang.", "" );
        StringBuffer buf = 
            new StringBuffer( basename.replaceFirst( "^java\\.lang\\.", "" ) );
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
     */
    public static String formatShape( int[] shape ) {
        if ( shape == null || shape.length == 0 ) {
            return "*";
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
        int[] ashape = (int[]) basicShape.clone();
        int ndim = ashape.length;
        if ( ashape[ ndim - 1 ] <= 0 ) {
            for ( int i = 0; i < ndim - 1; i++ ) {
                nel /= ashape[ i ];
            }
            ashape[ ndim - 1 ] = nel;
        }
        return ashape;
    }

}
