package uk.ac.starlink.table.view;

import gnu.jel.DVMap;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.StarTable;

/**
 * An object which is able to read cell values by column name or number and 
 * <tt>RowSubset</tt> inclusion flags by subset name or number.
 * The values are got from the reader's current row, which is set
 * using the {@link #setRow} method.  Think about thread safety when
 * calling <tt>setRow</tt> and subsequently evaluating an expression
 * which uses this reader.
 * <p>
 * This class currently deals with columns of all the primitive types, 
 * objects of type <tt>String<tt>, and arrays of any of these.  
 * Anything else is treated as an <tt>Object</tt> or <tt>Object[]</tt>.
 * It could be extended to deal with more if necessary.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class JELRowReader extends DVMap {

    private long lrow;
    private StarTable stable;
    private List subsets;

    /* Special value identifiers. */
    private static final byte INDEX_ID = 1;

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
     * Sets the table row to which property evaluations will refer.
     * Note that this row refers to the row in the underlying columns in
     * the StarTable, rather than the row of the model 
     * view itself, which may be under the influence of a row permutation.
     *
     * @param  lrow  the row index
     */
    public void setRow( long lrow ) {
        this.lrow = lrow;
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
        int ispecial = getSpecialId( name );
        if ( ispecial >= 0 ) {
            switch ( ispecial ) {
                case INDEX_ID: return "Long";
                default:       throw new AssertionError( "Unknown special" );
            }
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
        int isub = getSubsetIndex( name );
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
     * <li>a <tt>Short</tt> object (the subset index) if it appears 
     *     to reference a known row subset
     * <li>a <tt>byte</tt> object for one of the defined "special" values
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
     * @return  an Integer corresponding to column number, or a Short 
     *          corresponding to subset number, or null
     * @see    "JEL manual"
     */
    public Object translate( String name ) {

        /* See if it corresponds to a special value. */
        int ispecial = getSpecialId( name );
        if ( ispecial >= 0 ) {
            return new Byte( (byte) ispecial );
        }

        /* See if it corresponds to a column. */
        int icol = getColumnIndex( name );
        if ( icol >= 0 ) {
            return new Integer( icol );
        }

        /* See if it corresponds to a defined subset. */
        int isub = getSubsetIndex( name ); 
        if ( isub >= 0 ) {
            return new Short( (short) isub );
        }

        /* It is an error if the column name doesn't exist, since the
         * variable substitution isn't going to come from anywhere else. */
        return null;
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
    private int getSubsetIndex( String name ) {

        /* Try the '£' + number format. */
        if ( name.charAt( 0 ) == '£' ) {
            try {
                int isub = Integer.parseInt( name.substring( 1 ) ) - 1;
                if ( isub >= 0 && isub < subsets.size() ) {
                    return isub;
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
                return i;
            }
        }

        /* It's not a subset. */
        return -1;
    }

    /**
     * Returns the byte ID of the special quantity which corresponds to
     * a given name, or -1 if it isn't a special.
     * The current specials are:
     * <ul>
     * <li>"$0" or "index" returns INDEX_ID, which will return the
     *     (1-based) row number
     * </ul>
     */
    private byte getSpecialId( String name ) {
        if ( name.equals( "$0" ) || name.equalsIgnoreCase( "Index" ) ) {
            return INDEX_ID;
        }
        return (byte) -1;
    }

    /**
     * Returns the actual value for the special Index column.
     */
    public long getLongProperty( byte ispecial ) {
        switch ( ispecial ) {
            case INDEX_ID:   return lrow + 1;
            default:         throw new AssertionError();
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
     */

    public boolean getBooleanProperty( int icol ) {
        Boolean value = (Boolean) getValue( icol );
        return value.booleanValue();
    }
    public boolean[] getBooleanArrayProperty( int icol ) {
        return (boolean[]) getValue( icol );
    }
    public byte getByteProperty( int icol ) {
        Byte value = (Byte) getValue( icol );
        return value.byteValue();
    }
    public byte[] getByteArrayProperty( int icol ) {
        return (byte[]) getValue( icol );
    }
    public char getCharProperty( int icol ) {
        Character value = (Character) getValue( icol );
        return value.charValue();
    }
    public char[] getCharArrayProperty( int icol ) {
        return (char[]) getValue( icol );
    }
    public short getShortProperty( int icol ) {
        Short value = (Short) getValue( icol );
        return value.shortValue();
    }
    public short[] getShortArrayProperty( int icol ) {
        return (short[]) getValue( icol );
    }
    public int getIntProperty( int icol ) {
        Integer value = (Integer) getValue( icol );
        return value.intValue();
    }
    public int[] getIntArrayProperty( int icol ) {
        return (int[]) getValue( icol );
    }
    public long getLongProperty( int icol ) {
        Long value = (Long) getValue( icol );
        return value.longValue();
    }
    public long[] getLongArrayProperty( int icol ) {
        return (long[]) getValue( icol );
    }
    public float getFloatProperty( int icol ) {
        Float value = (Float) getValue( icol );
        return value.floatValue();
    }
    public float[] getFloatArrayProperty( int icol ) {
        return (float[]) getValue( icol );
    }
    public double getDoubleProperty( int icol ) {
        Double value = (Double) getValue( icol );
        return value.doubleValue();
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
