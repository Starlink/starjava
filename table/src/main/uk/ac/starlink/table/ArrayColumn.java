package uk.ac.starlink.table;

import java.util.BitSet;

/**
 * A column which provides data storage in java arrays.
 * One of the static <tt>makeColumn</tt> methods should be used to obtain an
 * instance of this class based on the characteristics of the data it is
 * to store.  Storage of primitive values is done in primitive arrays
 * where possible, so that <tt>float[]</tt> arrays are used in preference
 * to <tt>Float[]</tt> arrays, for efficiency.  Null values can be stored.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class ArrayColumn extends ColumnData {

    private final Object data;

    /**
     * Constructs a new column based on an existing <tt>ColumnInfo</tt> object
     * and with a given number of rows.
     *
     * @param   base   the template <tt>ColumnInfo</tt>
     * @param   data   the array used to hold the data
     */
    private ArrayColumn( ColumnInfo base, Object data ) {
        super( new ColumnInfo( base ) {
            public void setContentClass() {
                throw new UnsupportedOperationException();
            }
        } );
        this.data = data;
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

    abstract void storeValue( int irow, Object val );
    abstract Object readValue( int irow );

    /**
     * Returns the array object which holds the array data for this
     * column.
     *
     * @return   data array
     */
    public Object getArray() {
        return data;
    }

    /**
     * Constructs a new ArrayColumn based on a given data array.
     * The <tt>contentClass</tt> of the given base column info must
     * be compatible with the supplied data array; in the case of
     * a primitive <tt>data</tt> array it should be that of the 
     * corresponding wrapper class, and for non-primitive classes
     * it should be the class of what the array is an array of.
     * Alternatively, the <tt>base</tt> column info may have a 
     * <tt>null</tt> content class, in which case the column info for
     * the new column will be set appropriately from the data array.
     *
     * @param  base  the column info on which to base this column's info
     * @param  data  an array of primitives or objects which will form
     *         the storage for this column
     * @throws  IllegalArgumentException if <tt>data</tt> isn't an array or 
     *          <tt>base.getContentClass()</tt> is incompatible with 
     *          <tt>data</tt>
     */
    public static ArrayColumn makeColumn( ColumnInfo base, Object data ) {

        /* Find out what it's an array of. */
        Class eclazz = data.getClass().getComponentType();

        /* Bail out if it's not an array. */
        if ( eclazz == null ) {
            throw new IllegalArgumentException( "Data object is not an array "
                                              + " (it's a " + data.getClass() );
        }

        /* Return a custom class instance for primitive arrays. */
        else if ( eclazz == boolean.class ) {
            return new BooleanArrayColumn( base, (boolean[]) data );
        }
        else if ( eclazz == char.class ) {
            return new CharacterArrayColumn( base, (char[]) data );
        }
        else if ( eclazz == byte.class ) {
            return new ByteArrayColumn( base, (byte[]) data );
        }
        else if ( eclazz == short.class ) {
            return new ShortArrayColumn( base, (short[]) data );
        }
        else if ( eclazz == int.class ) {
            return new IntegerArrayColumn( base, (int[]) data );
        }
        else if ( eclazz == long.class ) {
            return new LongArrayColumn( base, (long[]) data );
        }
        else if ( eclazz == float.class ) {
            return new FloatArrayColumn( base, (float[]) data );
        }
        else if ( eclazz == double.class ) {
            return new DoubleArrayColumn( base, (double[]) data );
        }

        /* Return a generic one for object arrays. */
        else {
            assert ! eclazz.isPrimitive();
            return new ObjectArrayColumn( base, (Object[]) data );
        }
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
        int nrow = (int) rowCount;
        assert (long) nrow == rowCount;

        /* Get the base class. */
        Class clazz = base.getContentClass();

        /* Bail out if there isn't one. */
        if ( clazz == null ) {
            throw new IllegalArgumentException( "No class defined" );
        }

        /* Primitive subclass instances. */
        else if ( clazz == Boolean.class ) {
            return new BooleanArrayColumn( base, new boolean[ nrow ] );
        }
        else if ( clazz == Character.class ) {
            return new CharacterArrayColumn( base, new char[ nrow ] );
        }
        else if ( clazz == Byte.class ) {
            return new ByteArrayColumn( base, new byte[ nrow ] );
        }
        else if ( clazz == Short.class ) {
            return new ShortArrayColumn( base, new short[ nrow ] );
        }
        else if ( clazz == Integer.class ) {
            return new IntegerArrayColumn( base, new int[ nrow ] );
        }
        else if ( clazz == Long.class ) {
            return new LongArrayColumn( base, new long[ nrow ] );
        }
        else if ( clazz == Float.class ) {
            return new FloatArrayColumn( base, new float[ nrow ] );
        }
        else if ( clazz == Double.class ) {
            return new DoubleArrayColumn( base, new double[ nrow ] );
        }

        /* Generic instance. */
        else {
            return new ObjectArrayColumn( base, new Object[ nrow ] );
        }
    }

    /**
     * Ensures that this column's info is compatible with the given class.
     * If this column's info has a null class, it will be set from the
     * given one.  In case of incompatibility a runtime exception will
     * be thrown.
     *
     * @param  clazz  the class to check for compatibility
     * @throws  IllegalArgumentException  if not compatible
     */
    void checkContentClass( Class clazz ) {
        ColumnInfo colinfo = getColumnInfo();
        if ( colinfo.getContentClass() == null ) {
             colinfo.setContentClass( clazz );
        }
        else if ( ! colinfo.getContentClass().isAssignableFrom( clazz ) ) {
            throw new IllegalArgumentException( 
                "Incompatible content class in column info: " +
                colinfo.getContentClass() + " not assignable from " + clazz );
        }
    }


    /*
     * Define classes for the different array types.
     */

    /**
     * ArrayColumn subclass which keeps a separate record of whether each
     * element of its array represents a null value or not.  This enables
     * it to cope with nullable values.  Since it uses a <tt>BitSet</tt>
     * for this purpose it should be quite cheap and takes no space 
     * if no nulls are in fact used.
     */
    private static abstract class PrimitiveArrayColumn extends ArrayColumn {
        private final BitSet hasValue = new BitSet();
        PrimitiveArrayColumn( ColumnInfo base, Object data ) {
            super( base, data );
        }
        void storeValue( int irow, Object val ) {
            if ( val == null ) {
                hasValue.clear( irow );
            }
            else {
                hasValue.set( irow );
                storeElement( irow, val );
            }
        }
        Object readValue( int irow ) {
            return hasValue.get( irow ) ? readElement( irow ) : null;
        }
        abstract void storeElement( int irow, Object val );
        abstract Object readElement( int irow );
    }

    private static class BooleanArrayColumn extends PrimitiveArrayColumn {
        boolean[] data;
        BooleanArrayColumn( ColumnInfo base, boolean[] data ) {
            super( base, data );
            checkContentClass( Boolean.class );
            this.data = data;
        }
        void storeElement( int irow, Object val ) { 
            data[ irow ] = ((Boolean) val).booleanValue();
        }
        Object readElement( int irow ) {
            return data[ irow ] ? Boolean.TRUE : Boolean.FALSE;
        }
    }

    private static class CharacterArrayColumn extends PrimitiveArrayColumn {
        char[] data;
        CharacterArrayColumn( ColumnInfo base, char[] data ) {
            super( base, data );
            checkContentClass( Character.class );
            this.data = data;
        }
        void storeElement( int irow, Object val ) {
            data[ irow ] = ((Character) val).charValue();
        }
        Object readElement( int irow ) {
            return new Character( data[ irow ] );
        }
    }

    private static class ByteArrayColumn extends PrimitiveArrayColumn {
        byte[] data;
        ByteArrayColumn( ColumnInfo base, byte[] data ) {
            super( base, data );
            checkContentClass( Byte.class );
            this.data = data;
        }
        void storeElement( int irow, Object val ) {
            data[ irow ] = ((Byte) val).byteValue();
        }
        Object readElement( int irow ) {
            return new Byte( data[ irow ] );
        }
    }

    private static class ShortArrayColumn extends PrimitiveArrayColumn {
        short[] data;
        ShortArrayColumn( ColumnInfo base, short[] data ) {
            super( base, data );
            checkContentClass( Short.class );
            this.data = data;
        }
        void storeElement( int irow, Object val ) {
            data[ irow ] = ((Short) val).shortValue();
        }
        Object readElement( int irow ) {
            return new Short( data[ irow ] );
        }
    }

    private static class IntegerArrayColumn extends PrimitiveArrayColumn {
        int[] data;
        IntegerArrayColumn( ColumnInfo base, int[] data ) {
            super( base, data );
            checkContentClass( Integer.class );
            this.data = data;
        }
        void storeElement( int irow, Object val ) {
            data[ irow ] = ((Integer) val).intValue();
        }
        Object readElement( int irow ) {
            return new Integer( data[ irow ] );
        }
    }

    private static class LongArrayColumn extends PrimitiveArrayColumn {
        long[] data;
        LongArrayColumn( ColumnInfo base, long[] data ) {
            super( base, data );
            checkContentClass( Long.class );
            this.data = data;
        }
        void storeElement( int irow, Object val ) {
            data[ irow ] = ((Long) val).longValue();
        }
        Object readElement( int irow ) {
            return new Long( data[ irow ] );
        }
    }

    private static class FloatArrayColumn extends PrimitiveArrayColumn {
        float[] data;
        FloatArrayColumn( ColumnInfo base, float[] data ) {
            super( base, data );
            checkContentClass( Float.class );
            this.data = data;
        }
        void storeElement( int irow, Object val ) {
            data[ irow ] = ((Float) val).floatValue();
        }
        Object readElement( int irow ) {
            return new Float( data[ irow ] );
        }
    }

    private static class DoubleArrayColumn extends PrimitiveArrayColumn {
        double[] data;
        DoubleArrayColumn( ColumnInfo base, double[] data ) {
            super( base, data );
            checkContentClass( Double.class );
            this.data = data;
        }
        void storeElement( int irow, Object val ) {
            data[ irow ] = ((Double) val).doubleValue();
        }
        Object readElement( int irow ) {
            return new Double( data[ irow ] );
        }
    }

    private static class ObjectArrayColumn extends ArrayColumn {
        Object[] data;
        ObjectArrayColumn( ColumnInfo base, Object[] data ) {
            super( base, data );
            if ( getColumnInfo().getContentClass() == null ) {
                getColumnInfo().setContentClass( data.getClass()
                                                     .getComponentType() );
            }
            this.data = data;
        }
        void storeValue( int irow, Object val ) {
            data[ irow ] = val;
        }
        Object readValue( int irow ) {
            return data[ irow ];
        }
    }

}
