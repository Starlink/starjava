package uk.ac.starlink.table;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import uk.ac.starlink.table.gui.NumericCellRenderer;
import uk.ac.starlink.table.gui.ValueInfoCellEditor;
import uk.ac.starlink.table.gui.ValueInfoCellRenderer;

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
    private String utype = null;
    private String description = "";
    private Class contentClass = Object.class;
    private boolean isNullable = true;
    private int[] shape = new int[] { -1 };
    private int elementSize = -1;
    private TableCellRenderer cellRenderer;
    private TableCellEditor cellEditor;

    private static Pattern trailDigits = Pattern.compile( "\\.([0-9]+)$" );
    private static Pattern trailSpaces = Pattern.compile( "( +)$" );

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
        setUtype( base.getUtype() );
        setDescription( base.getDescription() );
        setContentClass( base.getContentClass() );
        setShape( base.getShape() );
        setElementSize( base.getElementSize() );
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
     * Sets the Utype string applying to values described by this object.
     *
     * @param  utype  the Utype, or <code>null</code> if none is known
     */
    public void setUtype( String utype ) {
        this.utype = utype;
    }

    public String getUtype() {
        return utype;
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

        /* If the content class has changed, reset any cached state which
         * depends on it. */
        if ( contentClass != this.contentClass ) {
            cellRenderer = null;
            cellEditor = null;
        }

        /* Set the class. */
        this.contentClass = contentClass;

        /* If the content class is not an array type, set the shape to null. */
        if ( ! isArray() ) {
            setShape( null );
        }

        /* If it is an array, ensure that the shape array is consistent
         * with this. */
        else {
            if ( shape == null ) {
                setShape( new int[] { -1 } );
            }
        }
    }

    public boolean isArray() {
        return contentClass.isArray();
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

    public int getElementSize() {
        return elementSize;
    }

    /**
     * Sets the element size of values described by this object.
     *
     * @param  size  the element size
     */
    public void setElementSize( int size ) {
        this.elementSize = size;
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
    public static ValueInfo generalise( ValueInfo vi1, ValueInfo vi2 ) {

        /* If they are the same, either will do. */
        if ( vi1.equals( vi2 ) ) {
            return vi1;
        }

        /* Otherwise we will need to create a new one with characteristics
         * build up from the supplied ones. */
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
     * Returns a renderer suitable for rendering the data described by 
     * this info.  Subclasses should override this method
     * if they can format their values in a component better than
     * allowing the <tt>formatValue</tt> text to be put into a cell.
     *
     * @return  a custom renderer
     */
    public TableCellRenderer getCellRenderer() {
        if ( cellRenderer == null ) {
            Class clazz = getContentClass();
            if ( Number.class.isAssignableFrom( clazz ) ) {
                cellRenderer = new NumericCellRenderer( clazz );
            }
            else if ( clazz.equals( Boolean.class ) ) {
                cellRenderer = BooleanCellRenderer.getInstance();
            }
            else {
                cellRenderer = new ValueInfoCellRenderer( this );
            }
        }
        return cellRenderer;
    }

    /**
     * Returns a cell editor suitable for editing the data described by
     * this info, or <tt>null</tt> if no user editing is possible.
     * Subclasses should override this method if they can do better than
     * calling the <tt>unformatValue</tt> method to turn user text 
     * into a cell value (or if they don't even want to attempt that,
     * in which case they should return null).
     *
     * @return  a custom editor, or <tt>null</tt>
     */
    public TableCellEditor getCellEditor() {
        if ( cellEditor == null ) {
            Class clazz = getContentClass();
            cellEditor = ValueInfoCellEditor.makeEditor( this );
        }
        return cellEditor;
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
            int[] ashape = getActualShape( shape, Array.getLength( value ) );
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
     * @return  a string showing the class and shape of <tt>clazz</tt>
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
        basename = basename.replaceFirst( "^java\\.lang\\.", "" );
        if ( basename.equals( "java.net.URL" ) ) {
            basename = "URL";
        }
        if ( basename.equals( "java.net.URI" ) ) {
            basename = "URI";
        }
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

    public Object unformatString( String rep ) {

        /*
         * This is a not-very-OO stopgap solution to this problem.
         * the proper solution requires some significant rejigging of
         * the class hierarchy in the tables package. 
         * I am writing demos that need to be working in a few days
         * and don't have time for it now.  mbt.
         */
        if ( rep == null || rep.length() == 0 ) {
            return null;
        }
        Class clazz = getContentClass();
        if ( clazz == Boolean.class ) {
            return Boolean.valueOf( rep );
        }
        else if ( clazz == Character.class ) {
            if ( rep.length() == 1 ) {
                return new Character( rep.charAt( 0 ) );
            }
            else if ( rep.trim().length() == 1 ) {
                return new Character( rep.trim().charAt( 0 ) );
            }
            else {
                throw new IllegalArgumentException();
            }
        }
        else if ( clazz == Byte.class ) {
            return Byte.valueOf( rep );
        }
        else if ( clazz == Short.class ) {
            return Short.valueOf( rep );
        }
        else if ( clazz == Integer.class ) {
            return Integer.valueOf( rep );
        }
        else if ( clazz == Long.class ) {
            return Long.valueOf( rep );
        }
        else if ( clazz == Float.class ) {
            if ( rep.trim().length() == 0 ) {
                return new Float( Float.NaN );
            }
            else {
                return Float.valueOf( rep );
            }
        }
        else if ( clazz == Double.class || clazz == Number.class ) {
            if ( rep.trim().length() == 0 ) {
                return new Double( Double.NaN );
            }
            else {
                return Double.valueOf( rep );
            }
        }
        else if ( clazz == String.class ) {
            return rep;
        }
        else {
            throw new IllegalArgumentException(
                "No unformatter available for " + clazz.getName() );
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
            int slice = 1;
            for ( int i = 0; i < ndim - 1; i++ ) {
                slice *= ashape[ i ];
            }
            ashape[ ndim - 1 ] = ( nel + slice - 1 ) / slice;
        }
        return ashape;
    }

}
