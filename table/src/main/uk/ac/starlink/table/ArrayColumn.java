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
public class ArrayColumn extends ColumnData {

    private final int nrow;

    /**
     * Constructs a new header based on an existing <tt>ColumnInfo</tt> object
     * and with a given number of rows.
     *
     * @param   base   the template <tt>ColumnInfo</tt>
     * @param   nrow   the number of rows
     */
    private ArrayColumn( ColumnInfo base, int nrow, final boolean nullable ) { 
        super( new ColumnInfo( base ) {
            public boolean isNullable() {
                return nullable;
            }
            public void setContentClass() {
                throw new UnsupportedOperationException();
            }
        } );
        this.nrow = nrow;
    }

    /**
     * Returns true, since this class can store cell values.
     *
     * @return  true
     */
    public boolean isWritable() {
        return true;
    }

    public void storeValue( long lrow, Object val ) {
        storeValue( (int) lrow, val );
    }

    public Object readValue( long lrow ) {
        return readValue( (int) lrow );
    }

    /**
     * Obtains an <tt>ArrayColumn</tt> object based on a template 
     * object with a given number of rows.  A new ColumnInfo object
     * will be constructed based on the given one.
     *
     * @param   base  the template <tt>ColumnInfo</tt> - note this is
     *          not the actual ColumnInfo object which will be returned
     *          by the <tt>getColumnInfo</tt> method of the returned 
     *          <tt>ArrayColumn</tt>
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
            return new ArrayColumn( base, nrow, false ) {
                byte[] data = new byte[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Byte) val).byteValue();
                } 
                public Object readValue( int irow ) {
                    return new Byte( data[ irow ] );
                }
            };
        }
        else if ( clazz == Short.class ) {
            return new ArrayColumn( base, nrow, false ) {
                short[] data = new short[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Short) val).shortValue();
                }
                public Object readValue( int irow ) {
                    return new Short( data[ irow ] );
                }
            };
        }
        else if ( clazz == Integer.class ) {
            return new ArrayColumn( base, nrow, false ) {
                int[] data = new int[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Integer) val).intValue();
                }
                public Object readValue( int irow ) {
                    return new Integer( data[ irow ] );
                }
            };
        }
        else if ( clazz == Long.class ) {
            return new ArrayColumn( base, nrow, false ) {
                long[] data = new long[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Long) val).longValue();
                }
                public Object readValue( int irow ) {
                    return new Long( data[ irow ] );
                }
            };
        }
        else if ( clazz == Float.class ) {
            return new ArrayColumn( base, nrow, false ) {
                float[] data = new float[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Float) val).floatValue();
                }
                public Object readValue( int irow ) {
                    return new Float( data[ irow ] );
                }
            };
        }
        else if ( clazz == Double.class ) {
            return new ArrayColumn( base, nrow, false ) {
                double[] data = new double[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Double) val).doubleValue();
                }
                public Object readValue( int irow ) {
                    return new Double( data[ irow ] );
                }
            };
        }
        else if ( clazz == Character.class ) {
            return new ArrayColumn( base, nrow, false ) {
                char[] data = new char[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Character) val).charValue();
                }
                public Object readValue( int irow ) {
                    return new Character( data[ irow ] );
                }
            };
        }
        else if ( clazz == Boolean.class ) {
            return new ArrayColumn( base, nrow, false ) {
                boolean[] data = new boolean[ nrow ];
                public void storeValue( int irow, Object val ) {
                    data[ irow ] = ((Boolean) val).booleanValue();
                }
                public Object readValue( int irow ) {
                    return data[ irow ] ? Boolean.TRUE : Boolean.FALSE;
                }
            };
        }
        else {
            return new ArrayColumn( base, nrow, true ) {
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
