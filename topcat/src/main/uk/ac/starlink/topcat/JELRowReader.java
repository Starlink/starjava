package uk.ac.starlink.topcat;

import gnu.jel.CompiledExpression;
import gnu.jel.DVMap;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * An object which is able to read cell values by column name or number and 
 * <tt>RowSubset</tt> inclusion flags by subset name or number.
 * The values are got using the {@link #evaluateAtRow} method.
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
 * <dt>Row Subset £ID identifiers:
 * <dd>The character {@link #CURRENCY_SIGN} (pound sign) 
 *     followed by the 1-based index of a defined row subset
 *     returns true iff the current column is part of the subset.
 *     Clients of this class are recommended to translate the
 *     <tt>CURRENCY_SIGN</tt> character to '#' for presentation 
 *     to users, as a more familiar symbol.
 *
 * <dt>Row Subset names:
 * <dd>The name of a subset (case-insensitive) returns true iff the current
 *     column is part of the named subset.
 *
 * <dt>Column null-queries:
 * <dd>The string {@link #NULL_QUERY_PREFIX} followed by a column name or 
 *     $ID identifier (see above) returns a boolean value which is 
 *     <tt>true</tt> iff the value in that column at the current row
 *     is the <tt>null</tt> value.
 * </dl>
 * 
 * @author   Mark Taylor (Starlink)
 */
public class JELRowReader extends DVMap {

    private long lrow;
    private boolean isNullExpression;
    private StarTable stable;
    private List subsets;

    /** Pound sign. */
    public static final char CURRENCY_SIGN = '\u00a3';

    /**
     * The string which, when prefixed to a column ideentifier, indicates
     * that the null-ness of the column should be queried.
     */
    public static final String NULL_QUERY_PREFIX = "NULL_";

    /* Special value identifiers. */
    private static final byte INDEX_ID = (byte) 1;
    private static final byte NULL_VALUE_ID = (byte) 2;
    private static final byte NULL_EXPRESSION_ID = (byte) 3;
    

    /**
     * Constructs a new row reader for a given StarTable. 
     * Note that this reader cannot become aware of changes to the 
     * columns of the table or to the subset list; in the event of 
     * such changes this object should be dicarded and and a new one
     * used for any new expressions.
     *
     * @param  stable  the StarTable this reader will read from
     * @param  subsets  the list of {@link RowSubset} objects which 
     *                  this reader will recognise
     */
    public JELRowReader( StarTable stable, List subsets ) {
        this.stable = stable;
        this.subsets = subsets;
    }

    /**
     * Evaluates a given compiled expression at a given row.
     * The returned value is wrapped up as an object if the result of
     * the expression is a primitive.
     *
     * @param  compEx  compiled expression
     * @param  dl      array of the instance references to the objects 
     *                 in dynamic library, as for 
     *                 {@link gnu.jel.CompiledExpression#evaluate}
     * @param  lrow    row index at which the expression is to be evaluated
     */
    public synchronized Object evaluateAtRow( CompiledExpression compEx,
                                              Object[] dl, long lrow )
             throws Throwable {
         this.lrow = lrow;
         try {
             isNullExpression = false;
             Object result = compEx.evaluate( dl );
             return isNullExpression ? null : result;
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
                default: throw new AssertionError( "Unknown special" );
            }
        }

        /* See if it's a null indicator, and return a Boolean value type
         * if so. */
        int inul = getNullColumnIndex( name );
        if ( inul >= 0 ) {
            return "Boolean";
        }

        /* See if it's a known column, and get the return value type by
         * looking at the column info if so. */
        int icol = getColumnIndex( name );
        if ( icol >= 0 ) {
            Class clazz = stable.getColumnInfo( icol ).getContentClass();
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

        /* See if it's a known subset, in which case it will have a 
         * boolean return type. */
        short isub = getSubsetIndex( name );
        if ( isub >= 0 ) {
            return "Boolean";
        }

        /* If we haven't got it yet, we don't know what it is. */
        return null;
    }

    /**
     * Turns a column specification into a constant object which can be
     * used at evaluation time to reference a particular quantity to
     * evaluate.  Currently this routine returns 
     * <ul>
     * <li>an <tt>Integer</tt> object (the column index) if 
     *     <tt>name</tt> appears to reference a known column 
     * <li>a <tt>Long</tt> object if it is a null query on a known column
     * <li>a <tt>Short</tt> object (the subset index) if it appears 
     *     to reference a known row subset
     * <li>a <tt>Byte</tt> object for one of the defined "special" values
     * <li><tt>null</tt> otherwise
     * </ul>
     * The different integral types are only used to separate the namespaces,
     * (and Short is bound to be big enough), there is no other significance
     * in these types.
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

        /* See if it corresponds to a null indicator. */
        int inul = getNullColumnIndex( name );
        if ( inul > 0 ) {
            return new Long( inul );
        }

        /* See if it corresponds to a column. */
        int icol = getColumnIndex( name );
        if ( icol >= 0 ) {
            return new Integer( icol );
        }

        /* See if it corresponds to a defined subset. */
        short isub = getSubsetIndex( name ); 
        if ( isub >= 0 ) {
            return new Short( isub );
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
   private int getColumnIndex( String name ) {

        /* Try the '$' + number format. */
        if ( name.charAt( 0 ) == '$' ) {
            try {
                int icol = Integer.parseInt( name.substring( 1 ) ) - 1;
                if ( icol >= 0 && icol < stable.getColumnCount() ) {
                    return icol;
                }
            }
            catch ( NumberFormatException e ) {
                // no good
            }
        }

        /* Try the column name. */
        for ( int icol = 0; icol < stable.getColumnCount(); icol++ ) {
            if ( stable.getColumnInfo( icol ).getName()
                       .equalsIgnoreCase( name ) ) {
                return icol;
            }
        }

        /* It's not a column. */
        return -1;
    }

    /**
     * Returns the index into the subsets list which corresponds to a given
     * subset name.  The current formats are
     * <ul>
     * <li> subset name (case insensitive, first occurrence used)
     * <li> "£"+(index+1) (so first subset in list would be "£1")
     * </ul>
     * Note this method is only called during expression compilation,
     * so it doesn't need to be particularly efficient.
     *
     * @param  name  subset identifier
     * @return  subset index into <tt>subsets</tt> list, or -1 if the
     *          subset was not known
     */
    private short getSubsetIndex( String name ) {

        /* Try the '£' + number format. */
        if ( name.charAt( 0 ) == CURRENCY_SIGN ) {
            try {
                int isub = Integer.parseInt( name.substring( 1 ) ) - 1;
                if ( isub >= 0 && isub < subsets.size() ) {
                    return (short) isub;
                }
            }
            catch ( NumberFormatException e ) {
                // no good
            }
        }

        /* Try the subset name. */
        int i = 0;
        for ( Iterator it = subsets.iterator(); it.hasNext(); i++ ) {
            RowSubset rset = (RowSubset) it.next();
            if ( rset.getName().equalsIgnoreCase( name ) ) {
                return (short) i;
            }
        }

        /* It's not a subset. */
        return (short) -1;
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
     * </ul>
     */
    private byte getSpecialId( String name ) {
        if ( name.equals( "$0" ) || name.equalsIgnoreCase( "Index" ) ) {
            return INDEX_ID;
        }
        else if ( name.equals( "null" ) ) {
            return NULL_VALUE_ID;
        }
        else if ( name.equals( "NULL" ) ) {
            return NULL_EXPRESSION_ID;
        }
        return (byte) -1;
    }

    /**
     * Returns the values for long-typed special variables.
     *
     * @param  ispecial  the identifier for the special
     * @return the special's value
     */
    public long getLongProperty( byte ispecial ) {
        switch ( ispecial ) {
            case INDEX_ID:   return lrow + 1;
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
                isNullExpression = true;
                return (byte) 0;
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
            return Tables.isBlank( stable.getCell( lrow, (int) inul ) );
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return true;
        }
    }

    /**
     * Returns the cell value for the current row and a given column.
     *
     * @param  icol  column index
     * @return  cell value as an Object
     */
    private Object getValue( int icol ) {
        try {
            return stable.getCell( lrow, icol );
        }
        catch( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * Methods for returning the actual column values.  
     * These must be of the form 'getXXXProperty(int)', where XXX is one
     * of the strings returned by the getTypeName method.
     * 
     * Those methods which return primitives check explicitly for null
     * values and set a flag (isNullExpression) while returning a dummy value;
     * the isNullExpression flag is checked by the evaluate method and if set,
     * null is returned.
     * It would be a bit simpler for these methods to throw a 
     * NullPointerException instead, which would percolate up to be 
     * thrown from the evaluate method, but handling it like that
     * can cause a big performance hit (exceptions are expensive).
     */

    public boolean getBooleanProperty( int icol ) {
        Boolean value = (Boolean) getValue( icol );
        if ( value == null ) {
            isNullExpression = true;
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
            isNullExpression = true;
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
            isNullExpression = true;
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
            isNullExpression = true;
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
            isNullExpression = true;
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
            isNullExpression = true;
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

    /**
     * Returns the actual subset value for the current row and a given
     * column.  
     *
     * @param  isub  index of the subset to evaluate at the current row
     * @return result of the <tt>isIncluded</tt> method of the 
     *         <tt>RowSubset</tt> indicated at the current row
     */
    public boolean getBooleanProperty( short isub ) {
        return ((RowSubset) subsets.get( (int) isub ))
              .isIncluded( lrow );
    }

}
