package uk.ac.starlink.table;

/**
 * A column which provides data storage in java arrays.
 * The static {@link #makeColumn} method should be used to obtain an
 * instance of this class based on the characteristics of the data it is
 * to store.  Storage of primitive values is done in primitive arrays
 * where possible, so that <tt>float[]</tt> arrays are used in preference
 * to <tt>Float[]</tt> arrays, for efficiency.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ArrayColumn extends ColumnInfo implements ColumnData {

    private final Class clazz;
    private final int nrow;

    /**
     * Constructs a new header based on an existing <tt>ColumnInfo</tt> object
     * and with a given number of rows.
     *
     * @param   base   the template <tt>ColumnInfo</tt>
     * @param   nrow   the number of rows
     */
    private ArrayColumn( ColumnInfo base, int nrow ) { 
        super( base );
        this.clazz = base.getContentClass();
        this.nrow = nrow;
    }


    public Class getContentClass() {
        return clazz;
    }

    /**
     * The class cannot be changed during the life of an <tt>ArrayColumn</tt>; 
     * an <tt>UnsupportedOperationException</tt> will be thrown.
     *
     * @throws  UnsupportedOperationException  unconditionally
     */
    public void setContentClass( Class clazz ) {
        throw new UnsupportedOperationException( 
            "Cannot change the content class of an ArrayColumn" );
    }

    public void storeValue( long lrow, Object val ) {
        storeValue( (int) lrow, val );
    }

    public Object readValue( long lrow ) {
        return readValue( (int) lrow );
    }

    /**
     * Obtains an <tt>ArrayColumn</tt> object based on a template 
     * object with a given number of rows.
     *
     * @param   base  the template <tt>ColumnInfo</tt>
     * @param   rowCount  the number of rows it is to hold
     * @return  a new <tt>ArrayColumn</tt> based on <tt>base</tt> with
     *          storage for <tt>rowCount</tt> elements
     */
    public static ArrayColumn makeColumn( ColumnInfo base, long rowCount ) {

        /* Validate the number of rows. */
        if ( rowCount > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException( "Too many rows requested: " 
                                              + rowCount
                                              + " > Integer.MAX_VALUE" );
        }
        final int nrow = (int) rowCount;
        assert (long) nrow == rowCount;

        /* Get the immutable class. */
        Class clazz = base.getContentClass();
        if ( clazz == null ) {
            throw new IllegalArgumentException( "No class defined" );
        }

        if ( clazz == Byte.class ) {
            return new ArrayColumn( base, nrow ) {
                byte[] data = new byte[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Byte) val).byteValue();
                } 
                public Object readValue( int irow ) {
                    return new Byte( data[ irow ] );
                }
                public boolean isNullable() {
                    return false;
                }
            };
        }
        else if ( clazz == Short.class ) {
            return new ArrayColumn( base, nrow ) {
                short[] data = new short[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Short) val).shortValue();
                }
                public Object readValue( int irow ) {
                    return new Short( data[ irow ] );
                }
                public boolean isNullable() {
                    return false;
                }
            };
        }
        else if ( clazz == Integer.class ) {
            return new ArrayColumn( base, nrow ) {
                int[] data = new int[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Integer) val).intValue();
                }
                public Object readValue( int irow ) {
                    return new Integer( data[ irow ] );
                }
                public boolean isNullable() {
                    return false;
                }
            };
        }
        else if ( clazz == Long.class ) {
            return new ArrayColumn( base, nrow ) {
                long[] data = new long[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Long) val).longValue();
                }
                public Object readValue( int irow ) {
                    return new Long( data[ irow ] );
                }
                public boolean isNullable() {
                    return false;
                }
            };
        }
        else if ( clazz == Float.class ) {
            return new ArrayColumn( base, nrow ) {
                float[] data = new float[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Float) val).floatValue();
                }
                public Object readValue( int irow ) {
                    return new Float( data[ irow ] );
                }
                public boolean isNullable() {
                    return false;
                }
            };
        }
        else if ( clazz == Double.class ) {
            return new ArrayColumn( base, nrow ) {
                double[] data = new double[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Double) val).doubleValue();
                }
                public Object readValue( int irow ) {
                    return new Double( data[ irow ] );
                }
                public boolean isNullable() {
                    return false;
                }
            };
        }
        else if ( clazz == Character.class ) {
            return new ArrayColumn( base, nrow ) {
                char[] data = new char[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Character) val).charValue();
                }
                public Object readValue( int irow ) {
                    return new Character( data[ irow ] );
                }
                public boolean isNullable() {
                    return false;
                }
            };
        }
        else if ( clazz == Boolean.class ) {
            return new ArrayColumn( base, nrow ) {
                boolean[] data = new boolean[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Boolean) val).booleanValue();
                }
                public Object readValue( int irow ) {
                    return data[ irow ] ? Boolean.TRUE : Boolean.FALSE;
                }
                public boolean isNullable() {
                    return false;
                }
            };
        }
        else {
            return new ArrayColumn( base, nrow ) {
                Object[] data = new Object[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = val;
                }
                public Object readValue( int irow ) {
                    return data[ irow ];
                }
            };
        }
    }
}
