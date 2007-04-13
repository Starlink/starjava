package uk.ac.starlink.ttools;

import gnu.jel.CompiledExpression;
import gnu.jel.DVMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * An object which is able to read cell values by column name or number.
 * The values are got using the {@link #evaluate} method.
 * <p>
 * This class currently deals with columns of all the primitive types, 
 * objects of type {@link java.lang.String} or {@link java.util.Date},
 * and arrays of any of these.  
 * Anything else is treated as an <tt>Object</tt> or <tt>Object[]</tt>.
 * It could be extended to deal with more if necessary.
 * <p>
 * Expressions of the following types are understood:
 * <dl>
 * <dt>"null":
 * <dd>the <tt>null</tt> value (this is not provided as part of the JEL 
 *     engine).
 *
 * <dt>"NULL":
 * <dd>if this expression is evaluated at any point in the expression 
 *     evaluation, then the result of the whole evaluation will be 
 *     <tt>null</tt>.  This has the same effect as throwing a 
 *     <tt>NullPointerException</tt> during evaluation.
 *     The NULL token is syntactically of type <tt>byte</tt>, which can
 *     be promoted implicitly to any numeric value; this means it can be
 *     used anywhere a primitive (other than <tt>boolean</tt>) can be used.
 *
 * <dt>"$0" or "Index" (case insensitive):
 * <dd>the 1-based index of the current row
 *
 * <dt>Column $ID identifiers:
 * <dd>The letter '$' followed by the 1-based index of the column refers
 *     to the contents of that column in the current row (as a primitive,
 *     if applicable).
 * 
 * <dt>Column names:
 * <dd>The name of a column (case-insensitive) refers to the contents of
 *     that column in the current row (as a primitive, if applicable) - 
 *     this can only work if the column name is a legal java identifier.
 * 
 * <dt>Column null-queries:
 * <dd>The string {@link #NULL_QUERY_PREFIX} followed by a column name or 
 *     $ID identifier (see above) returns a boolean value which is 
 *     <tt>true</tt> iff the value in that column at the current row
 *     has a blank value.
 *
 * <dt>Parameter names:
 * <dd>The name of a table parameter (case-insensitive) is constant for
 *     the table (as a primitive, if applicable) - this can only work
 *     if the parameter name is a legal java identifier.
 *
 * <dt>Parameter null-queries:
 * <dd>The string {@link #NULL_QUERY_PREFIX} followed by a parameter name
 *     returns a boolean value which is <tt>true</tt> iff that parameter
 *     has a blank value.
 *
 * <dt>"RANDOM":
 * <dd>The special token "RANDOM" evaluates to a double-precision random
 *     number <code>0<=x<1</code> which is constant for a given row
 *     within this reader.  The quality of the random numbers may not
 *     be particularly good.
 * </dl>
 * 
 * @author   Mark Taylor (Starlink)
 */
public abstract class JELRowReader extends DVMap {

    private final StarTable table_;
    private final Object[] args_;
    private boolean isNullExpression_;
    private List constantList_;
    private final long HASH_LONG = System.identityHashCode( this );

    /**
     * The string which, when prefixed to a column identifier, indicates
     * that the null-ness of the column should be queried.
     */
    public static final String NULL_QUERY_PREFIX = "NULL_";

    /** Prefix identifying a unique column identifier. */
    public static final char COLUMN_ID_CHAR = '$';

    /* Special value identifiers. */
    private static final byte INDEX_ID = (byte) 1;
    private static final byte NULL_VALUE_ID = (byte) 2;
    private static final byte NULL_EXPRESSION_ID = (byte) 3;
    private static final byte RANDOM_ID = (byte) 4;
    private static final byte FALSE_ID = (byte) 5;
    private static final byte TRUE_ID = (byte) 6;

    /**
     * Constructs a new row reader for a given StarTable. 
     * Note that this reader cannot become aware of changes to the 
     * columns of the table; in the event of 
     * such changes this object should be dicarded and and a new one
     * used for any new expressions.
     *
     * @param  table  the StarTable this reader will read from
     */
    public JELRowReader( StarTable table ) {
        table_ = table;
        args_ = new Object[] { this };
        constantList_ = new ArrayList();
    }

    /**
     * Returns the value for a given column in this reader's table at
     * the current row.
     *
     * @param  icol  column index
     * @return  contents of column <tt>icol</tt> at the current row
     */
    protected abstract Object getCell( int icol ) throws IOException;

    /**
     * Returns the index of the row on which evaluations are currently
     * taking place.
     *
     * @return   row index (first row is 0)
     */
    public abstract long getCurrentRow();

    /**
     * Evaluates a given compiled expression at the current row.
     * The returned value is wrapped up as an object if the result of
     * the expression is a primitive.
     *
     * @param  compEx  compiled expression
     */
    public synchronized Object evaluate( CompiledExpression compEx )
             throws Throwable {
         try {
             isNullExpression_ = false;
             Object result = compEx.evaluate( args_ );
             return isNullExpression_ ? null : result;
         }
         catch ( NullPointerException e ) {
             return null;
         }
    }

    /**
     * Returns the type name of the quantity which is referenced in 
     * expressions with a given name.  The significance of this return
     * value is that it appears in the names of the 
     * corresponding <tt>getXXXProperty</tt> methods in this class.
     *
     * @param   name  the variable name
     * @return  the corresponding method name fragment
     * @see   "JEL manual"
     */
    public String getTypeName( String name ) {

        /* See if it's a known special, and treat it specially if so. */
        byte ispecial = getSpecialId( name );
        if ( ispecial >= 0 ) {
            switch ( ispecial ) {
                case INDEX_ID: return "Long";
                case NULL_VALUE_ID: return "Object";
                case NULL_EXPRESSION_ID: return "Byte";
                case RANDOM_ID: return "Double";
                case FALSE_ID: return "Boolean";
                case TRUE_ID: return "Boolean";
                default: throw new AssertionError( "Unknown special" );
            }
        }

        /* See if it's a null column indicator, and return a Boolean value type
         * if so. */
        int inul = getNullColumnIndex( name );
        if ( inul >= 0 ) {
            return "Boolean";
        }

        /* See if it's a known column, and get the return value type by
         * looking at the column info if so. */
        int icol = getColumnIndex( name );
        if ( icol >= 0 ) {
            return getTypeName( table_.getColumnInfo( icol )
                                      .getContentClass() );
        }

        /* See if it's a null constant indicator, and return a Boolean value
         * type if so. */
        int inulconst = getNullConstantIndex( name );
        if ( inulconst >= 0 ) {
            return "Boolean";
        }

        /* See if it's a known constant, and get the type by looking at
         * the actual value, or if null the value metadata, if so. */
        int iconst = getConstantIndex( name );
        if ( iconst >= 0 ) {
            DescribedValue dval = getConstant( iconst );
            Object value = dval.getValue();
            Class clazz = value != null ? value.getClass()
                                        : dval.getInfo().getContentClass();
            return getTypeName( clazz );
        }

        /* If we haven't got it yet, we don't know what it is. */
        return null;
    }

    /**
     * Returns the type name corresponding to a given class.
     * The significance of this return value is that it appears in
     * the names of the corresponding <tt>getXXXProperty</tt>
     * methods in this class.
     *
     * @param   name  the value class
     * @return  the corresponding method name fragment
     * @see   "JEL manual"
     */
    private static String getTypeName( Class clazz ) {
        if ( clazz.equals( Boolean.class ) ) {
            return "Boolean";
        }
        else if ( clazz.equals( boolean[].class ) ) {
            return "BooleanArray";
        }
        else if ( clazz.equals( Byte.class ) ) {
            return "Byte";
        }
        else if ( clazz.equals( byte[].class ) ) {
            return "ByteArray";
        }
        else if ( clazz.equals( Character.class ) ) {
            return "Char";
        }
        else if ( clazz.equals( char[].class ) ) {
            return "CharArray";
        }
        else if ( clazz.equals( Short.class ) ) {
            return "Short";
        }
        else if ( clazz.equals( short[].class ) ) {
            return "ShortArray";
        }
        else if ( clazz.equals( Integer.class ) ) {
            return "Int";
        }
        else if ( clazz.equals( int[].class ) ) {
            return "IntArray";
        }
        else if ( clazz.equals( Long.class ) ) {
            return "Long";
        }
        else if ( clazz.equals( long[].class ) ) {
            return "LongArray";
        }
        else if ( clazz.equals( Float.class ) ) {
            return "Float";
        }
        else if ( clazz.equals( float[].class ) ) {
            return "FloatArray";
        }
        else if ( clazz.equals( Double.class ) ) {
            return "Double";
        }
        else if ( clazz.equals( double[].class ) ) {
            return "DoubleArray";
        }
        else if ( clazz.equals( Number.class ) ) {
            return "Number";
        }
        else if ( clazz.equals( String.class ) ) {
            return "String";
        }
        else if ( clazz.equals( String[].class ) ) {
            return "StringArray";
        }
        else if ( clazz.equals( Date.class ) ) {
            return "Date";
        }
        else if ( clazz.equals( Date[].class ) ) {
            return "DateArray";
        }
        else if ( clazz.getComponentType() != null ) {
            return "ObjectArray";
        }
        else {
            return "Object";
        }
    }

    /**
     * Turns a value specification into a constant object which can be
     * used at evaluation time to reference a particular quantity to
     * evaluate.  Currently this routine returns 
     * <ul>
     * <li>a non-negative <tt>Integer</tt> object (the column index) if 
     *     <tt>name</tt> appears to reference a known column 
     * <li>a negative <tt>Integer</tt> object (-1-constIndex) if
     *     <tt>name</tt> appears to reference a known constant
     * <li>a <tt>Long</tt> object if it is a null query on a known column
     * <li>a <tt>Byte</tt> object for one of the defined "special" values
     * <li><tt>null</tt> otherwise
     * </ul>
     * The different integral types are only used to separate the namespaces,
     * there is no other significance in these types.
     * <p>
     * This method is only called at expression compilation time, not
     * evaluation time, so it doesn't need to be particularly fast.
     *
     * @param  name  the name of the variable-like object to evaluate
     * @return  a numeric object corresponding to an object which we
     *          know how to evaluate
     * @see    "JEL manual"
     */
    public Object translate( String name ) {

        /* See if it corresponds to a special value. */
        byte ispecial = getSpecialId( name );
        if ( ispecial >= 0 ) {
            return new Byte( ispecial );
        }

        /* See if it corresponds to a null column indicator. */
        int inul = getNullColumnIndex( name );
        if ( inul >= 0 ) {
            return new Long( inul );
        }

        /* See if it corresponds to a column. */
        int icol = getColumnIndex( name );
        if ( icol >= 0 ) {
            return new Integer( icol );
        }

        /* See if it corresponds to a constant (parameter) value. */
        int iconst = getConstantIndex( name );
        if ( iconst >= 0 ) {
            return new Integer( -1 - iconst );
        }

        /* It is an error if the column name doesn't exist, since the
         * variable substitution isn't going to come from anywhere else. */
        return null;
    }

    /**
     * Returns the column index in the table model which corresponds to
     * the column name as a null query.  If <tt>name</tt> has the form
     * "{@link #NULL_QUERY_PREFIX}<i>column-name</i>" where 
     * <i>column-name</i> is as 
     * recognised by the {@link #getColumnIndex} method, then the return
     * value will be the index of the column corresponding to 
     * <i>column-name</i>.
     * Otherwise (if it doesn't start with the NULL_QUERY_PREFIX string or 
     * the <tt>name</tt> part
     * doesn't correspond to a known column) the value -1 will be returned.
     * <p>
     * Note this method is only called during expression compilation,
     * so it doesn't need to be particularly efficient.
     *
     * @param   name  null-query column identifier
     * @return  column index for which <tt>name</tt> is a null-query, 
     *          or -1
     */
    private int getNullColumnIndex( String name ) {
        if ( name.startsWith( NULL_QUERY_PREFIX ) ) {
            String colname = name.substring( NULL_QUERY_PREFIX.length() );
            return getColumnIndex( colname );
        }
        return -1;
    }

    /**
     * Returns the constant index which corresponds to the given name as a 
     * null query.
     *
     * @param  name  null-query identifier
     * @return  constant index for which <code>name</code> is a null-query
     *          or -1
     * @see  #getNullColumnIndex
     * @see  #getConstantIndex
     */
    private int getNullConstantIndex( String name ) {
        if ( name.startsWith( NULL_QUERY_PREFIX ) ) {
            String constname = name.substring( NULL_QUERY_PREFIX.length() );
            return getConstantIndex( constname );
        }
        return -1;
    }

    /**
     * Returns the column index in the table model which corresponds to 
     * a given name.  The current formats are
     * <ul>
     * <li> column name (case insensitive, first occurrence used)
     * <li> "$"+(index+1) (so first column would be "$1")
     * </ul>
     * Note that the name '$0' is reserved for the special index column.
     * <p>
     * Note this method is only called during expression compilation,
     * so it doesn't need to be particularly efficient.
     *
     * @param  name  column identifier
     * @return  column index, or -1 if the column was not known
     */
    public int getColumnIndex( String name ) {

        /* Blank name, unknown column. */
        if ( name.length() == 0 ) {
            return -1;
        }

        /* Try the '$' + number format. */
        else if ( name.charAt( 0 ) == COLUMN_ID_CHAR ) {
            try {
                int icol = Integer.parseInt( name.substring( 1 ) ) - 1;
                if ( icol >= 0 && icol < table_.getColumnCount() ) {
                    return icol;
                }
            }
            catch ( NumberFormatException e ) {
                // no good
            }
        }

        /* Try the column name. */
        for ( int icol = 0; icol < table_.getColumnCount(); icol++ ) {
            if ( table_.getColumnInfo( icol ).getName()
                       .equalsIgnoreCase( name ) ) {
                return icol;
            }
        }

        /* It's not a column. */
        return -1;
    }

    /**
     * Returns the index into this reader's list of known constants which
     * corresponds to a given name.  Matching is case insensitive and
     * the list of table parameters is searched.
     * <p>
     * Note this method is only called during expression compilation,
     * so it doesn't need to be particularly efficient.
     *
     * @param   name  parameter name
     * @return  index into constants list, or -1 if no such constant was found
     * @see     #getConstant
     */
    private int getConstantIndex( String name ) {
 
         /* Null name - no match. */
         if ( name == null ) {
             return -1;
         }
 
         /* Search the existing list of known constants for a name match.
          * If one is found return the index. */
         for ( int i = 0; i < constantList_.size(); i++ ) {
             DescribedValue dval = (DescribedValue) constantList_.get( i );
             if ( name.equalsIgnoreCase( dval.getInfo().getName() ) ) {
                 return i;
             }
        }

        /* Search the table's parameter list for a name match.  If one is
         * found add it to the constants list and return the index. */
        for ( Iterator it = table_.getParameters().iterator(); it.hasNext(); ) {
            DescribedValue dval = (DescribedValue) it.next();
            if ( name.equalsIgnoreCase( dval.getInfo().getName() ) ) {
                constantList_.add( dval );
                return constantList_.size() - 1;
            }
        } 

        /* No luck. */
        return -1;
    }

    /**
     * Returns the byte ID of the special quantity which corresponds to
     * a given name, or -1 if it isn't a special.
     * The current specials are:
     * <ul>
     * <li>"$0" or "index" returns INDEX_ID, which will return the
     *     (1-based) row number
     * <li>"null" returns the <tt>null</tt> value (this is not built in
     *     to the JEL evaluator)
     * <li>"NULL" flags that an attempt has been made to evaluate a 
     *     primitive with no value, and thus invalidates the rest of the
     *     evaluation
     * <li>"RANDOM" returns a random value between 0 and 1; for a given 
     *     row index, this value is always the same for this RowReader
     * </ul>
     */
    private byte getSpecialId( String name ) {
        if ( name.equals( COLUMN_ID_CHAR + "0" ) ||
             name.equalsIgnoreCase( "Index" ) ) {
            return INDEX_ID;
        }
        else if ( name.equals( "null" ) ) {
            return NULL_VALUE_ID;
        }
        else if ( name.equals( "NULL" ) ) {
            return NULL_EXPRESSION_ID;
        }
        else if ( name.equals( "RANDOM" ) ) {
            return RANDOM_ID;
        }
        else if ( getNullConstantIndex( name ) >= 0 ) {
            DescribedValue dval = getConstant( getNullConstantIndex( name ) );
            return Tables.isBlank( dval.getValue() ) ? TRUE_ID
                                                     : FALSE_ID;
        }
        return (byte) -1;
    }

    /**
     * Returns one of the constants established for this reader object.
     *
     * @param  iconst  constant index as returned by {@link #getConstantIndex}
     * @return  constant value+metadata object
     */
    private DescribedValue getConstant( int iconst ) {
        return (DescribedValue) constantList_.get( iconst );
    }

    /**
     * Return the value of a constant with a given index.
     *
     * @param  iconst  constant index as returned by {@link #getConstantIndex}
     * @return  value as an Object
     */
    private Object getConstantValue( int iconst ) {
        return getConstant( iconst ).getValue();
    }

    /**
     * Returns the values for long-typed special variables.
     *
     * @param  ispecial  the identifier for the special
     * @return the special's value
     */
    public long getLongProperty( byte ispecial ) {
        switch ( ispecial ) {
            case INDEX_ID:   return getCurrentRow() + 1;
            default:         throw new AssertionError();
        }
    }

    /**
     * Returns the values for Object-typed special variables.
     *
     * @param  ispecial  the identifier for the special
     * @return the special's value
     */
    public Object getObjectProperty( byte ispecial ) {
        switch ( ispecial ) {
            case NULL_VALUE_ID:      return null;
            default:                 throw new AssertionError();
        }
    }

    /**
     * Returns the values for byte-typed special variables.
     *
     * @param  ispecial  the identifier for the special
     * @return  the special's value
     */
    public byte getByteProperty( byte ispecial ) {
        switch ( ispecial ) {
            case NULL_EXPRESSION_ID:
                isNullExpression_ = true;
                return (byte) 0;
            default:
                throw new AssertionError();
        }
    }

    /**
     * Returns the values for double-typed special variables.
     *
     * @param  ispecial  the identifier for the special
     * @return  the special's value
     */
    public double getDoubleProperty( byte ispecial ) {
        switch ( ispecial ) {
            case RANDOM_ID:
                long seed = HASH_LONG + ( getCurrentRow() * 2000000011L );
                return new Random( seed ).nextDouble();
            default:
                throw new AssertionError();
        }
    }

    /**
     * Return the values for boolean-typed special variables.
     *
     * @param  ispecial  the identifier for the special
     * @param  the special's value
     */
    public boolean getBooleanProperty( byte ispecial ) {
        switch ( ispecial ) {
            case FALSE_ID:
                return false;
            case TRUE_ID:
                return true;
            default:
                throw new AssertionError();
        }
    }

    /**
     * Indicates whether the cell at the current row in a given column
     * has a blank value.  This is the case if the value is the
     * java <tt>null</tt> reference, or if it is a Float or Double
     * with a NaN value.
     *
     * @param  inul column index (as a <tt>long</tt>)
     * @return whether the cell is null
     */
    public boolean getBooleanProperty( long inul ) {
        try {
            return Tables.isBlank( getCell( (int) inul ) );
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return true;
        }
    }

    /**
     * Indicates whether the constant with the given index has a blank value.
     * This is the case if the value is the java <tt>null</tt> reference,
     * or if it is a Float or Double with a NaN value.
     *
     * @param  inul  constant index as returned by {@link #getConstantIndex}
     * @return  whether the constant has a null value
     */
    public boolean getBooleanProperty( short inul ) {
        return Tables.isBlank( getConstant( inul ).getValue() );
    }

    /**
     * Returns the cell value for the current row and a given column.
     *
     * @param  icol  column index
     * @return  cell value as an Object
     */
    private Object getValue( int icol ) {
        if ( icol >= 0 ) {
            try {
                return getCell( icol );
            }
            catch( IOException e ) {
                e.printStackTrace();
                return null;
            }
        }
        else {
            return getConstantValue( -1 - icol );
        }
    }

    /*
     * Methods for returning the actual column values.  
     * These must be of the form 'getXXXProperty(int)', where XXX is one
     * of the strings returned by the getTypeName method.
     * 
     * Those methods which return primitives check explicitly for null
     * values and set a flag (isNullExpression_) while returning a dummy value;
     * the isNullExpression_ flag is checked by the evaluate method and if set,
     * null is returned.
     * It would be a bit simpler for these methods to throw a 
     * NullPointerException instead, which would percolate up to be 
     * thrown from the evaluate method, but handling it like that
     * can cause a big performance hit (exceptions are expensive).
     */

    public boolean getBooleanProperty( int icol ) {
        Boolean value = (Boolean) getValue( icol );
        if ( value == null ) {
            isNullExpression_ = true;
            return false;
        }
        else {
            return value.booleanValue();
        }
    }
    public boolean[] getBooleanArrayProperty( int icol ) {
        return (boolean[]) getValue( icol );
    }
    public byte getByteProperty( int icol ) {
        Byte value = (Byte) getValue( icol );
        if ( value == null ) {
            isNullExpression_ = true;
            return (byte) 0;
        }
        else {
            return value.byteValue();
        }
    }
    public byte[] getByteArrayProperty( int icol ) {
        return (byte[]) getValue( icol );
    }
    public char getCharProperty( int icol ) {
        Character value = (Character) getValue( icol );
        if ( value == null ) {
            isNullExpression_ = true;
            return (char) 0;
        }
        else {
            return value.charValue();
        }
    }
    public char[] getCharArrayProperty( int icol ) {
        return (char[]) getValue( icol );
    }
    public short getShortProperty( int icol ) {
        Short value = (Short) getValue( icol );
        if ( value == null ) {
            isNullExpression_ = true;
            return (short) 0;
        }
        else {
            return value.shortValue();
        }
    }
    public short[] getShortArrayProperty( int icol ) {
        return (short[]) getValue( icol );
    }
    public int getIntProperty( int icol ) {
        Integer value = (Integer) getValue( icol );
        if ( value == null ) {
            isNullExpression_ = true;
            return 0;
        }
        else {
            return value.intValue();
        }
    }
    public int[] getIntArrayProperty( int icol ) {
        return (int[]) getValue( icol );
    }
    public long getLongProperty( int icol ) {
        Long value = (Long) getValue( icol );
        if ( value == null ) {
            isNullExpression_ = true;
            return 0L;
        }
        else {
            return value.longValue();
        }
    }
    public long[] getLongArrayProperty( int icol ) {
        return (long[]) getValue( icol );
    }
    public float getFloatProperty( int icol ) {
        Float value = (Float) getValue( icol );
        return value == null ? Float.NaN : value.floatValue();
    }
    public float[] getFloatArrayProperty( int icol ) {
        return (float[]) getValue( icol );
    }
    public double getDoubleProperty( int icol ) {
        Double value = (Double) getValue( icol );
        return value == null ? Double.NaN : value.doubleValue();
    }
    public double[] getDoubleArrayProperty( int icol ) {
        return (double[]) getValue( icol );
    }
    public double getNumberProperty( int icol ) {
        Number value = (Number) getValue( icol );
        return value == null ? Double.NaN : value.doubleValue();
    }
    public String getStringProperty( int icol ) {
        return (String) getValue( icol );
    }
    public String[] getStringArrayProperty( int icol ) {
        return (String[]) getValue( icol );
    }
    public Date getDateProperty( int icol ) {
        return (Date) getValue( icol );
    }
    public Date[] getDateArrayProperty( int icol ) {
        return (Date[]) getValue( icol );
    }
    public Object getObjectProperty( int icol ) {
        return getValue( icol );
    }
    public Object[] getObjectArrayProperty( int icol ) {
        return (Object[]) getValue( icol );
    }
}
