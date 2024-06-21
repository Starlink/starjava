package uk.ac.starlink.table;

import java.util.BitSet;

/**
 * A column which provides data storage in a java array of primitives.
 * Thus a <code>float[]</code> array is used rather than a <code>Float[]</code>
 * array, which should be more efficient on memory.
 * Null values may be stored in the column; a {@link java.util.BitSet}
 * is used to keep track of which elements are <code>null</code>.
 * By default (on column construction),
 * none of the values are <code>null</code>.
 * <p>
 * Obtain an instance of this class using one of the 
 * <code>makePrimitiveColumn</code> methods.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class PrimitiveArrayColumn extends ArrayColumn {

    private final Object data;
    private boolean trueMeansNull = true;
    private final BitSet flags = new BitSet();

    private PrimitiveArrayColumn( ColumnInfo base, Object data ) {
        super( base, data );
        this.data = data;
    }

    protected void storeValue( int irow, Object val ) {
        if ( val == null ) {
            flags.set( irow, trueMeansNull );
        }
        else {
            flags.set( irow, ! trueMeansNull );
            storeElement( irow, val );
        }
    }

    protected Object readValue( int irow ) {
        return ( flags.get( irow ) == trueMeansNull ) ? null 
                                                      : readElement( irow );
    }

    /**
     * Sets all the elements in this column to <code>null</code>.
     * Each will remain <code>null</code> until it is explicitly set (to a
     * non-null value) using {@link #storeValue} or until 
     * {@link #setNoNulls} is called.
     */
    public void setAllNulls() {
        trueMeansNull = false;
        flags.clear();
    }

    /**
     * Sets all the elements in this column to non-<code>null</code> values.
     * The value of each cell will be determined by the value of the
     * underlying data array, until it is set using
     * {@link #storeValue} with a <code>null</code> argument, or 
     * {@link #setAllNulls} is called.
     */
    public void setNoNulls() {
        trueMeansNull = true;
        flags.clear();
    }

    abstract void storeElement( int irow, Object val );
    abstract Object readElement( int irow );

    /**
     * Constructs a new PrimitiveArrayColumn based on a given data array.
     * The <code>contentClass</code> of the given base column info must
     * be compatible with the supplied data array; it should be that of 
     * the corresponding wrapper class.
     * Alternatively, the <code>base</code> column info may have a
     * <code>null</code> content class, in which case the column info for
     * the new column will be set appropriately from the data array.
     *
     * @param  base  the column info on which to base this column's info
     * @param  data  an array of primitives which will form
     *         the storage for this column
     * @return  a new <code>PrimitiveArrayColumn</code> based on
     *          <code>base</code> backed by <code>data</code>
     * @throws  IllegalArgumentException if <code>data</code> isn't an array or
     *          <code>base.getContentClass()</code> is incompatible with
     *          <code>data</code>
     */
    public static PrimitiveArrayColumn makePrimitiveColumn( ColumnInfo base,
                                                            Object data ) {
        Class<?> clazz = data.getClass();
        if ( clazz == boolean[].class ) {
            return new BooleanArrayColumn( base, (boolean[]) data );
        }
        else if ( clazz == char[].class ) {
            return new CharacterArrayColumn( base, (char[]) data );
        }
        else if ( clazz == byte[].class ) {
            return new ByteArrayColumn( base, (byte[]) data );
        }
        else if ( clazz == short[].class ) {
            return new ShortArrayColumn( base, (short[]) data );
        }
        else if ( clazz == int[].class ) {
            return new IntegerArrayColumn( base, (int[]) data );
        }
        else if ( clazz == long[].class ) {
            return new LongArrayColumn( base, (long[]) data );
        }
        else if ( clazz == float[].class ) {
            return new FloatArrayColumn( base, (float[]) data );
        }
        else if ( clazz == double[].class ) {
            return new DoubleArrayColumn( base, (double[]) data );
        }
        else {
            throw new IllegalArgumentException(
                "Data object is not an array of primitives " +
                " (it's a " + clazz.getName() + ")" );
        }
    }

    /**
     * Obtains an <code>ArrayColumn</code> object based on a template
     * object with a given number of rows.  A new ColumnInfo object
     * will be constructed based on the given one.
     *
     * @param   base  the template <code>ColumnInfo</code> - note this is
     *          not the actual ColumnInfo object which will be returned
     *          by the <code>getColumnInfo</code> method of the returned
     *          <code>ArrayColumn</code>
     * @param   rowCount  the number of rows it is to hold
     * @return  a new <code>PrimitiveArrayColumn</code>
     *          based on <code>base</code> with
     *          storage for <code>rowCount</code> elements
     */
    public static PrimitiveArrayColumn makePrimitiveColumn( ColumnInfo base,
                                                            long rowCount ) {

        /* Validate the number of rows. */
        if ( rowCount > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException( "Too many rows requested: "
                                              + rowCount
                                              + " > Integer.MAX_VALUE" );
        }
        int nrow = (int) rowCount;
        assert (long) nrow == rowCount;

        /* Get the base class. */
        Class<?> clazz = base.getContentClass();

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
        else {
            throw new IllegalArgumentException(
                "Supplied columninfo content class is not a primitive " +
                "wrapper type (it's a " + clazz.getName() + ")" );
        }
    }

    //
    // Implementations for all the primitive types.
    // Actually, I've got a feeling I could avoid having all these inner
    // classes by just making use of the java.lang.reflect.Array
    // set and get methods.  Not sure if there is much in the way of
    // performance pros/cons either way, but it works as it is, so
    // leave it for now.
    //

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
}
