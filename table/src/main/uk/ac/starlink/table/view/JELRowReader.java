package uk.ac.starlink.table.view;

import gnu.jel.DVMap;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * An object which is able to read cell values by column name.
 * The values are got from the reader's current row, which is set
 * using the {@link #setRow} method.  Think about thread safety when
 * calling <tt>setRow</tt> and subsequently evaluating an expression
 * which uses this reader.
 * <p>
 * This class currently deals with all the primitive types, objects
 * of type <tt>String<tt>, and arrays of any of these.  Anything else
 * is treated as an <tt>Object</tt> or <tt>Object[]</tt>.
 * It could be extended to deal with more if necessary.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class JELRowReader extends DVMap {

    private long lrow;
    private StarTable stable;

    /**
     * Constructs a new row reader for a given StarTable. 
     * Note that this reader cannot become aware of changes to the 
     * column model used by this model; in the event of changes to the
     * column usage in the table this object should be discarded and
     * a new one created.
     *
     * @param  stabld  the StarTable this reader will read from
     */
    public JELRowReader( StarTable stable ) {
        this.stable = stable;
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

    public String getTypeName( String name ) {
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
            else if ( clazz.getComponentType() != null ) {
                return "ObjectArray";
            }
            else {
                return "Object";
            }
        }
        else {
            return null;
        }
    }

    public Object translate( String name ) {

        /* Doesn't have to be fast - only called at compile time. */
        int icol = getColumnIndex( name );
        if ( icol >= 0 ) {
            return new Integer( icol );
        }

        /* It is an error if the column name doesn't exist, since the
         * variable substitution isn't going to come from anywhere else. */
        else {
            return null;
        }
   }

   /**
    * Returns the column index in the table model which corresponds to 
    * a given name.  The current formats are
    * <ul>
    * <li> column name (case insensitive, first occurrence used)
    * <li> "$"+(index+1) (so first column would be "$1")
    * </ul>
    * <p>
    * Note this method is only called during expression compilation,
    * so it doesn't need to be particularly efficient.
    *
    * @param  name  column identifier
    * @return  column index, or -1 if the column was not known
    */
   private int getColumnIndex( String name ) {
        for ( int icol = 0; icol < stable.getColumnCount(); icol++ ) {
            if ( stable.getColumnInfo( icol ).getName()
                       .equalsIgnoreCase( name ) ) {
                return icol;
            }
        }
        if ( name.charAt( 0 ) == '$' ) {
            try {
                int icol1 = Integer.parseInt( name.substring( 1 ) );
                return icol1 - 1;
            }
            catch ( NumberFormatException e ) {
                // no good
            }
        }
        return -1;
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
     * Methods for returning the actual values.  
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
    public Object getObjectProperty( int icol ) {
        return getValue( icol );
    }
    public Object[] getObjectArrayProperty( int icol ) {
        return (Object[]) getValue( icol );
    }
    
}
