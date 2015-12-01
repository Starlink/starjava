package uk.ac.starlink.ttools.plot2.data;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.util.ByteList;
import uk.ac.starlink.util.DoubleList;
import uk.ac.starlink.util.FloatList;
import uk.ac.starlink.util.IntList;
import uk.ac.starlink.util.ShortList;

/**
 * CachedColumnFactory implementation that stores data in arrays in memory.
 *
 * @author   Mark Taylor
 * @since    5 Feb 2013
 */
public class MemoryColumnFactory implements CachedColumnFactory {

    private static final Map<StorageType,ColumnCreator> creatorMap_ =
        createCreatorMap();
   
    public CachedColumn createColumn( StorageType type, long nrow ) {
        return creatorMap_.get( type ).createColumn( nrow );
    }

    /**
     * Creates a map of StorageType-specific column creators.
     *
     * @return   column creator map
     */
    private static Map<StorageType,ColumnCreator> createCreatorMap() {
        Map<StorageType,ColumnCreator> map =
            new HashMap<StorageType,ColumnCreator>();
        map.put( StorageType.BOOLEAN, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return new BitSetColumn( nrow );
            }
        } );
        map.put( StorageType.DOUBLE, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return nrow >= 0 ? new FixedDoubleColumn( nrow )
                                 : new UnknownDoubleColumn();
            }
        } );
        map.put( StorageType.FLOAT, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return nrow >= 0 ? new FixedFloatColumn( nrow )
                                 : new UnknownFloatColumn();
            }
        } );
        map.put( StorageType.INT, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return nrow >= 0 ? new FixedIntColumn( nrow )
                                 : new UnknownIntColumn();
            }
        } );
        map.put( StorageType.SHORT, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return nrow >= 0 ? new FixedShortColumn( nrow )
                                 : new UnknownShortColumn();
            }
        } );
        map.put( StorageType.BYTE, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return nrow >= 0 ? new FixedByteColumn( nrow )
                                 : new UnknownByteColumn();
            }
        } );
        map.put( StorageType.STRING, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return nrow >= 0
                     ? new FixedObjectColumn<String>( String.class, nrow )
                     : new UnknownObjectColumn<String>( String.class );
            }
        } );
        map.put( StorageType.FLOAT_ARRAY, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return nrow >= 0
                    ? new FixedObjectColumn<float[]>( float[].class, nrow )
                    : new UnknownObjectColumn<float[]>( float[].class );
            }
        } );
        map.put( StorageType.DOUBLE_ARRAY, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return nrow >= 0
                    ? new FixedObjectColumn<double[]>( double[].class, nrow )
                    : new UnknownObjectColumn<double[]>( double[].class );
            }
        } );
        map.put( StorageType.INT3, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return nrow >= 0
                    ? new FixedIntArrayColumn( 3, nrow ) {
                          protected CachedSequence
                                    createSequence( int[] data ) {
                              return new IntSequence3( data );
                          }
                      }
                    : new UnknownIntArrayColumn( 3 ) {
                          protected CachedSequence
                                    createSequence( int[] data ) {
                              return new IntSequence3( data );
                          }
                      };
            }
        } );
        map.put( StorageType.DOUBLE3, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return nrow >= 0
                    ? new FixedDoubleArrayColumn( 3, nrow ) {
                          protected CachedSequence
                                    createSequence( double[] data ) {
                              return new DoubleSequence3( data );
                          }
                      }
                    : new UnknownDoubleArrayColumn( 3 ) {
                          protected CachedSequence
                                    createSequence( double[] data ) {
                              return new DoubleSequence3( data );
                          }
                      };
            }
        } );
        map.put( StorageType.FLOAT3, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return nrow >= 0
                    ? new FixedFloatArrayColumn( 3, nrow ) {
                          protected CachedSequence
                                    createSequence( float[] data ) {
                              return new FloatSequence3( data );
                          }
                      }
                    : new UnknownFloatArrayColumn( 3 ) {
                          protected CachedSequence
                                    createSequence( float[] data ) {
                              return new FloatSequence3( data );
                          }
                      };
            }
        } );
        assert map.keySet()
                  .containsAll( Arrays.asList( StorageType.values() ) );
        return Collections.unmodifiableMap( map );
    }

    /**
     * Converts an object to a double.
     *
     * @param   obj, presumed numeric
     * @return  numerical value of <code>obj</code>
     */
    private static double toDouble( Object obj ) {
        return ((Number) obj).doubleValue();
    }

    /**
     * Converts an object to a float.
     *
     * @param   obj, presumed numeric
     * @return  numerical value of <code>obj</code>
     */
    private static float toFloat( Object obj ) {
        return ((Number) obj).floatValue();
    }

    /**
     * Converts an object to an integer.
     *
     * @param   obj, presumed numeric
     * @return  numerical value of <code>obj</code>
     */
    private static int toInt( Object obj ) {
        return ((Number) obj).intValue();
    }

    /**
     * Converts an object to a short.
     *
     * @param   obj, presumed numeric
     * @return  numerical value of <code>obj</code>
     */
    private static short toShort( Object obj ) {
        return ((Number) obj).shortValue();
    }

    /**
     * Converts an object to a byte.
     *
     * @param   obj, presumed numeric
     * @return  numerical value of <code>obj</code>
     */
    private static byte toByte( Object obj ) {
        return ((Number) obj).byteValue();
    }

    /**
     * Converts an object to a boolean.
     *
     * @param  obj  object, presumed boolean
     * @return  boolean value of <code>obj</code>, if in doubt, false
     */
    private static boolean toBoolean( Object obj ) {
        return ((Boolean) obj).booleanValue();
    }

    /**
     * Converts an object to a String.
     *
     * @param   obj  object, presumed string
     * @return   string value of <code>obj</code>, or null
     */
    private static String toString( Object obj ) {
        return (String) obj;
    }

    /**
     * Creates a column for a known storage type.
     */
    private static interface ColumnCreator {

        /**
         * Returns a new storage object of given known or unknown length
         * appropriate to this object's storage type.
         *
         * @param  nrow   number of elements to be stored;
         *                if a value &lt;0 is supplied,
         *                an indeterminate number will be stored
         * @return   storage object
         */
        CachedColumn createColumn( long nrow );
    }

    /**
     * CachedColumn implementation for boolean values.
     */
    private static class BitSetColumn implements CachedColumn {
        private final BitSet mask_;
        private int irow_;
        private Integer nrow_;

        /**
         * Constructor.
         *
         * @param   nrow  size of array, &lt;0 if unknown
         */
        BitSetColumn( long nrow ) {
            if ( nrow > Integer.MAX_VALUE ) {
                throw new UnsupportedOperationException( "Too long " + nrow );
            }
            mask_ = nrow >= 0 ? new BitSet( (int) nrow ) : new BitSet();
        }
        public void add( Object value ) {
            mask_.set( irow_++, toBoolean( value ) );
        }
        public void endAdd() {
            nrow_ = new Integer( irow_ );
        }
        public CachedSequence createSequence() {
            return new BitSetSequence( mask_, nrow_.intValue() );
        }
    }

    /**
     * CachedColumn implementation for object values, column length is known.
     */
    private static class FixedObjectColumn<T> implements CachedColumn {
        private final Class<T> clazz_;
        private final int nrow_;
        private final T[] data_;
        private int irow_;

        /**
         * Constructor.
         *
         * @param   clazz  column element class
         * @param   nrow   column length
         */
        FixedObjectColumn( Class<T> clazz, long nrow ) {
            clazz_ = clazz;
            if ( nrow > Integer.MAX_VALUE ) {
                throw new UnsupportedOperationException( "Too long " + nrow );
            }
            nrow_ = (int) nrow;
            @SuppressWarnings("unchecked")
            T[] data = (T[]) Array.newInstance( clazz, nrow_ );
            data_ = data;
        }

        public void add( Object value ) {
            data_[ irow_++ ] = clazz_.cast( value );
        }

        public void endAdd() {
            assert irow_ == nrow_;
        }

        public CachedSequence createSequence() {
            return new ObjectArraySequence<T>( data_ );
        }
    }

    /**
     * CachedColumn implementation for double values, column length is known.
     */
    private static class FixedDoubleColumn implements CachedColumn {
        private final int nrow_;
        private final double[] data_;
        private int irow_;

        /**
         * Constructor.
         *
         * @param   nrow   column length
         */
        FixedDoubleColumn( long nrow ) {
            if ( nrow > Integer.MAX_VALUE ) {
                throw new UnsupportedOperationException( "Too long " + nrow );
            }
            nrow_ = (int) nrow;
            data_ = new double[ nrow_ ];
        }

        public void add( Object value ) {
            data_[ irow_++ ] = toDouble( value );
        }

        public void endAdd() {
            assert irow_ == nrow_;
        }

        public CachedSequence createSequence() {
            return new DoubleArraySequence( data_ );
        }
    }

    /**
     * CachedColumn implementation for float values, column length is known.
     */
    private static class FixedFloatColumn implements CachedColumn {
        private final int nrow_;
        private final float[] data_;
        private int irow_;

        /**
         * Constructor.
         *
         * @param   nrow   column length
         */
        FixedFloatColumn( long nrow ) {
            if ( nrow > Integer.MAX_VALUE ) {
                throw new UnsupportedOperationException( "Too long " + nrow );
            }
            nrow_ = (int) nrow;
            data_ = new float[ nrow_ ];
        }

        public void add( Object value ) {
            data_[ irow_++ ] = toFloat( value );
        }

        public void endAdd() {
            assert irow_ == nrow_;
        }

        public CachedSequence createSequence() {
            return new FloatArraySequence( data_ );
        }
    }

    /**
     * CachedColumn implementation for int values, column length is known.
     */
    private static class FixedIntColumn implements CachedColumn {
        private final int nrow_;
        private final int[] data_;
        private int irow_;

        /**
         * Constructor.
         *
         * @param  nrow  column length
         */
        FixedIntColumn( long nrow ) {
            if ( nrow > Integer.MAX_VALUE ) {
                throw new UnsupportedOperationException( "Too long " + nrow );
            }
            nrow_ = (int) nrow;
            data_ = new int[ nrow_ ];
        }

        public void add( Object value ) {
            data_[ irow_++ ] = toInt( value );
        }

        public void endAdd() {
            assert irow_ == nrow_;
        }

        public CachedSequence createSequence() {
            return new IntArraySequence( data_ );
        }
    }

    /**
     * CachedColumn implementation for short values, column length is known.
     */
    private static class FixedShortColumn implements CachedColumn {
        private final int nrow_;
        private final short[] data_;
        private int irow_;

        /**
         * Constructor.
         *
         * @param  nrow  column length
         */
        FixedShortColumn( long nrow ) {
            if ( nrow > Integer.MAX_VALUE ) {
                throw new UnsupportedOperationException( "Too long " + nrow );
            }
            nrow_ = (int) nrow;
            data_ = new short[ nrow_ ];
        }

        public void add( Object value ) {
            data_[ irow_++ ] = toShort( value );
        }

        public void endAdd() {
            assert irow_ == nrow_;
        }

        public CachedSequence createSequence() {
            return new ShortArraySequence( data_ );
        }
    }

    /**
     * CachedColumn implementation for byte values, column length is known.
     */
    private static class FixedByteColumn implements CachedColumn {
        private final int nrow_;
        private final byte[] data_;
        private int irow_;

        /**
         * Constructor.
         *
         * @param  nrow  column length
         */
        FixedByteColumn( long nrow ) {
            if ( nrow > Integer.MAX_VALUE ) {
                throw new UnsupportedOperationException( "Too long " + nrow );
            }
            nrow_ = (int) nrow;
            data_ = new byte[ nrow_ ];
        }

        public void add( Object value ) {
            data_[ irow_++ ] = toByte( value );
        }

        public void endAdd() {
            assert irow_ == nrow_;
        }

        public CachedSequence createSequence() {
            return new ByteArraySequence( data_ );
        }
    }

    /**
     * CachedColumn implementation for fixed-length double arrays, 
     * column length is known.
     */
    private static abstract class FixedDoubleArrayColumn
            implements CachedColumn {
        private final int ncol_;
        private final int nrow_;
        private final double[] data_;
        private int ipos_;

        /**
         * Constructor.
         *
         * @param  ncol  array size of each column element
         * @param  nrow  column length
         */
        FixedDoubleArrayColumn( int ncol, long nrow ) {
            if ( nrow * ncol > Integer.MAX_VALUE ) {
                throw new IllegalArgumentException( "Too long " + nrow );
            }
            ncol_ = ncol;
            nrow_ = (int) nrow;
            data_ = new double[ nrow_ * ncol_ ];
        }

        public void add( Object value ) {
            double[] dval = (double[]) value;
            for ( int ic = 0; ic < ncol_; ic++ ) {
                data_[ ipos_++ ] = dval[ ic ];
            }
        }

        public void endAdd() {
            assert ipos_ == nrow_ * ncol_;
        }

        public CachedSequence createSequence() {
            return createSequence( data_ );
        }

        /**
         * Returns a CachedSequence based on a single vector of floating
         * point data, with columns varying quickest.
         *
         * @param  data  array data
         */
        protected abstract CachedSequence createSequence( double[] data );
    }

    /**
     * CachedColumn implementation for fixed-length float arrays, 
     * column length is known.
     */
    private static abstract class FixedFloatArrayColumn
            implements CachedColumn {
        private final int ncol_;
        private final int nrow_;
        private final float[] data_;
        private int ipos_;

        /**
         * Constructor.
         *
         * @param  ncol  array size of each column element
         * @param  nrow  column length
         */
        FixedFloatArrayColumn( int ncol, long nrow ) {
            if ( nrow * ncol > Integer.MAX_VALUE ) {
                throw new IllegalArgumentException( "Too long " + nrow );
            }
            ncol_ = ncol;
            nrow_ = (int) nrow;
            data_ = new float[ nrow_ * ncol_ ];
        }

        public void add( Object value ) {
            float[] fval = (float[]) value;
            for ( int ic = 0; ic < ncol_; ic++ ) {
                data_[ ipos_++ ] = fval[ ic ];
            }
        }

        public void endAdd() {
            assert ipos_ == nrow_ * ncol_;
        }

        public CachedSequence createSequence() {
            return createSequence( data_ );
        }

        /**
         * Returns a CachedSequence based on a single vector of floating
         * point data, with columns varying quickest.
         *
         * @param  data  array data
         */
        protected abstract CachedSequence createSequence( float[] data );
    }

    /**
     * CachedColumn implementation for fixed-length integer arrays,
     * column length is known.
     */
    private static abstract class FixedIntArrayColumn
            implements CachedColumn {
        private final int ncol_;
        private final int nrow_;
        private final int[] data_;
        private int ipos_;

        /**
         * Constructor.
         *
         * @param  ncol  array size of each column element
         * @param  nrow  column length
         */
        FixedIntArrayColumn( int ncol, long nrow ) {
            if ( nrow * ncol > Integer.MAX_VALUE ) {
                throw new IllegalArgumentException( "Too long " + nrow );
            }
            ncol_ = ncol;
            nrow_ = (int) nrow;
            data_ = new int[ nrow_ * ncol_ ];
        }

        public void add( Object value ) {
            int[] ival = (int[]) value;
            for ( int ic = 0; ic < ncol_; ic++ ) {
                data_[ ipos_++ ] = ival[ ic ];
            }
        }

        public void endAdd() {
            assert ipos_ == nrow_ * ncol_;
        }

        public CachedSequence createSequence() {
            return createSequence( data_ );
        }

        /**
         * Returns a CachedSequence based on a single vector of integer
         * data, with columns varying quickest.
         *
         * @param   data   array data
         */
        protected abstract CachedSequence createSequence( int[] data );
    }

    /**
     * CachedColumn implementation for object values, column length not known.
     */
    private static class UnknownObjectColumn<T> implements CachedColumn {
        private final Class<T> clazz_;
        private final T[] zArray_;
        private List<T> list_;
        private T[] data_;

        /**
         * Constructor.
         *
         * @param   clazz  element class for type to be returned from
         *                 sequence getObject method
         */
        UnknownObjectColumn( Class<T> clazz ) {
            clazz_ = clazz;
            @SuppressWarnings("unchecked")
            T[] zArray = (T[]) Array.newInstance( clazz, 0 );
            zArray_ = zArray;
            list_ = new ArrayList<T>();
        }

        public void add( Object value ) {
            list_.add( clazz_.cast( value ) );
        }

        public void endAdd() {
            data_ = list_.toArray( zArray_ );
            list_ = null;
        }

        public CachedSequence createSequence() {
            return new ObjectArraySequence<T>( data_ );
        }
    }

    /**
     * CachedColumn implementation for double values, column length not known.
     */
    private static class UnknownDoubleColumn implements CachedColumn {
        private DoubleList list_;
        private double[] data_;

        /**
         * Constructor.
         */
        UnknownDoubleColumn() {
            list_ = new DoubleList();
        }

        public void add( Object value ) {
            list_.add( toDouble( value ) );
        }

        public void endAdd() {
            data_ = list_.toDoubleArray();
            list_ = null;
        }

        public CachedSequence createSequence() {
            return new DoubleArraySequence( data_ );
        }
    }

    /**
     * CachedColumn implementation for float values, column length not known.
     */
    private static class UnknownFloatColumn implements CachedColumn {
        private FloatList list_;
        private float[] data_;

        UnknownFloatColumn() {
            list_ = new FloatList();
        }

        public void add( Object value ) {
            list_.add( toFloat( value ) );
        }

        public void endAdd() {
            data_ = list_.toFloatArray();
            list_ = null;
        }

        public CachedSequence createSequence() {
            return new FloatArraySequence( data_ );
        }
    }

    /**
     * CachedColumn implementation for int values, column length not known.
     */
    private static class UnknownIntColumn implements CachedColumn {
        private IntList list_;
        private int[] data_;

        UnknownIntColumn() {
            list_ = new IntList();
        }

        public void add( Object value ) {
            list_.add( toInt( value ) );
        }

        public void endAdd() {
            data_ = list_.toIntArray();
            list_ = null;
        }

        public CachedSequence createSequence() {
            return new IntArraySequence( data_ );
        }
    }

    /**
     * CachedColumn implementation for short values, column length not known.
     */
    private static class UnknownShortColumn implements CachedColumn {
        private ShortList list_;
        private short[] data_;

        UnknownShortColumn() {
            list_ = new ShortList();
        }

        public void add( Object value ) {
            list_.add( toShort( value ) );
        }

        public void endAdd() {
            data_ = list_.toShortArray();
            list_ = null;
        }

        public CachedSequence createSequence() {
            return new ShortArraySequence( data_ );
        }
    }

    /**
     * CachedColumn implementation for byte values, column length not known.
     */
    private static class UnknownByteColumn implements CachedColumn {
        private ByteList list_;
        private byte[] data_;

        UnknownByteColumn() {
            list_ = new ByteList();
        }

        public void add( Object value ) {
            list_.add( toByte( value ) );
        }

        public void endAdd() {
            data_ = list_.toByteArray();
            list_ = null;
        }

        public CachedSequence createSequence() {
            return new ByteArraySequence( data_ );
        }
    }

    /**
     * CachedColumn implementation for fixed-length double arrays,
     * column length not known.
     */
    private static abstract class UnknownDoubleArrayColumn
            implements CachedColumn {
        private final int ncol_;
        private DoubleList list_;
        private double[] data_;

        /**
         * Constructor.
         *
         * @param   ncol  array size of each column element
         */
        UnknownDoubleArrayColumn( int ncol ) {
            ncol_ = ncol;
            list_ = new DoubleList();
        }

        public void add( Object value ) {
            double[] dval = (double[]) value;
            for ( int ic = 0; ic < ncol_; ic++ ) {
                list_.add( dval[ ic ] );
            }
        }

        public void endAdd() {
            data_ = list_.toDoubleArray();
            list_ = null;
        }

        public CachedSequence createSequence() {
            return createSequence( data_ );
        }

        /**
         * Returns a CachedSequence based on a single vector of floating
         * point data, with columns varying quickest.
         *
         * @param  data  array data
         */
        protected abstract CachedSequence createSequence( double[] data );
    }

    /**
     * CachedColumn implementation for fixed-length float arrays,
     * column length not known.
     */
    private static abstract class UnknownFloatArrayColumn
            implements CachedColumn {
        private final int ncol_;
        private FloatList list_;
        private float[] data_;

        /**
         * Constructor.
         *
         * @param   ncol  array size of each column element
         */
        UnknownFloatArrayColumn( int ncol ) {
            ncol_ = ncol;
            list_ = new FloatList();
        }

        public void add( Object value ) {
            float[] fval = (float[]) value;
            for ( int ic = 0; ic < ncol_; ic++ ) {
                list_.add( fval[ ic ] );
            }
        }

        public void endAdd() {
            data_ = list_.toFloatArray();
            list_ = null;
        }

        public CachedSequence createSequence() {
            return createSequence( data_ );
        }

        /**
         * Returns a CachedSequence based on a single vector of floating
         * point data, with columns varying quickest.
         *
         * @param  data  array data
         */
        protected abstract CachedSequence createSequence( float[] data );
    }

    /**
     * CachedColumn implementation for fixed-length int arrays,
     * column length not known.
     */
    private static abstract class UnknownIntArrayColumn
            implements CachedColumn {
        private final int ncol_;
        private IntList list_;
        private int[] data_;

        /**
         * Constructor.
         *
         * @param   ncol  array size of each column element
         */
        UnknownIntArrayColumn( int ncol ) {
            ncol_ = ncol;
            list_ = new IntList();
        }

        public void add( Object value ) {
            int[] ival = (int[]) value;
            for ( int ic = 0; ic < ncol_; ic++ ) {
                list_.add( ival[ ic ] );
            }
        }

        public void endAdd() {
            data_ = list_.toIntArray();
            list_ = null;
        }

        public CachedSequence createSequence() {
            return createSequence( data_ );
        }

        /**
         * Returns a CachedSequence based on a single vector of integer
         * data, with columns varying quickest.
         *
         * @param  data  array data
         */
        protected abstract CachedSequence createSequence( int[] data );
    }

    /**
     * Boolean-yielding CachedSequence implementation based on a BitSet.
     */
    private static class BitSetSequence implements CachedSequence {
        private final BitSet mask_;
        private final int nrow_;
        private int irow_ = -1;

        /**
         * Constructor.
         *
         * @param  mask  bit vector
         * @param  nrow  number of items in sequence
         */
        public BitSetSequence( BitSet mask, int nrow ) {
            mask_ = mask;
            nrow_ = nrow;
        }
        public boolean next() {
            return ++irow_ < nrow_;
        }
        public Object getObjectValue() {
            return Boolean.valueOf( mask_.get( irow_ ) );
        }
        public boolean getBooleanValue() {
            return mask_.get( irow_ );
        }
        public double getDoubleValue() {
            return Double.NaN;
        }
        public int getIntValue() {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Object-yielding CachedSequence implementation based on an Object array.
     */
    private static class ObjectArraySequence<T> implements CachedSequence {
        private final T[] data_;
        private final int nrow_;
        private int irow_ = -1;

        /**
         * Constructor.
         *
         * @param   data  data array
         */
        public ObjectArraySequence( T[] data ) {
            data_ = data;
            nrow_ = data.length;
        }
        public boolean next() {
            return ++irow_ < nrow_;
        }
        public T getObjectValue() {
            return data_[ irow_ ];
        }
        public double getDoubleValue() {
            return Double.NaN;
        }
        public int getIntValue() {
            return Integer.MIN_VALUE;
        }
        public boolean getBooleanValue() {
            return false;
        }
    }

    /**
     * Double-yielding CachedSequence implementation based on a double array.
     */
    private static class DoubleArraySequence implements CachedSequence {
        private final double[] data_;
        private final int nrow_;
        private int irow_ = -1;

        /**
         * Constructor.
         *
         * @param   data  data array
         */
        DoubleArraySequence( double[] data ) {
            data_ = data;
            nrow_ = data.length;
        }
        public boolean next() {
            return ++irow_ < nrow_;
        }
        public Object getObjectValue() {
            return new Double( data_[ irow_ ] );
        }
        public double getDoubleValue() {
            return data_[ irow_ ];
        }
        public int getIntValue() {
            return (int) data_[ irow_ ];
        }
        public boolean getBooleanValue() {
            return false;
        }
    }

    /**
     * Double-yielding CachedSequence implementation based on an float array.
     */
    private static class FloatArraySequence implements CachedSequence {
        private final float[] data_;
        private final int nrow_;
        private int irow_ = -1;

        /**
         * Constructor.
         *
         * @param   data  data array
         */
        FloatArraySequence( float[] data ) {
            data_ = data;
            nrow_ = data.length;
        }
        public boolean next() {
            return ++irow_ < nrow_;
        }
        public Object getObjectValue() {
            return new Float( data_[ irow_ ] );
        }
        public double getDoubleValue() {
            return (double) data_[ irow_ ];
        }
        public int getIntValue() {
            return (int) data_[ irow_ ];
        }
        public boolean getBooleanValue() {
            return false;
        }
    }

    /**
     * Int-yielding CachedSequence implementation based on an int array.
     */
    private static class IntArraySequence implements CachedSequence {
        private final int[] data_;
        private final int nrow_;
        private int irow_ = -1;

        /**
         * Constructor.
         *
         * @param   data  data array
         */
        IntArraySequence( int[] data ) {
            data_ = data;
            nrow_ = data.length;
        }
        public boolean next() {
            return ++irow_ < nrow_;
        }
        public Object getObjectValue() {
            return new Integer( data_[ irow_ ] );
        }
        public double getDoubleValue() {
            return data_[ irow_ ];
        }
        public int getIntValue() {
            return data_[ irow_ ];
        }
        public boolean getBooleanValue() {
            return false;
        }
    }

    /**
     * Int-yielding CachedSequence implementation based on a short array.
     */
    private static class ShortArraySequence implements CachedSequence {
        private final short[] data_;
        private final int nrow_;
        private int irow_ = -1;

        /**
         * Constructor.
         *
         * @param   data  data array
         */
        ShortArraySequence( short[] data ) {
            data_ = data;
            nrow_ = data.length;
        }
        public boolean next() {
            return ++irow_ < nrow_;
        }
        public Object getObjectValue() {
            return new Short( data_[ irow_ ] );
        }
        public double getDoubleValue() {
            return data_[ irow_ ];
        }
        public int getIntValue() {
            return data_[ irow_ ];
        }
        public boolean getBooleanValue() {
            return false;
        }
    }

    /**
     * Int-yielding CachedSequence implementation based on a byte array.
     */
    private static class ByteArraySequence implements CachedSequence {
        private final byte[] data_;
        private final int nrow_;
        private int irow_ = -1;

        /**
         * Constructor.
         *
         * @param   data  data array
         */
        ByteArraySequence( byte[] data ) {
            data_ = data;
            nrow_ = data.length;
        }
        public boolean next() {
            return ++irow_ < nrow_;
        }
        public Object getObjectValue() {
            return new Byte( data_[ irow_ ] );
        }
        public double getDoubleValue() {
            return data_[ irow_ ];
        }
        public int getIntValue() {
            return data_[ irow_ ];
        }
        public boolean getBooleanValue() {
            return false;
        }
    }

    /**
     * Double[3]-yielding CachedSequence implementation
     * based on a double array.
     */
    private static class DoubleSequence3 implements CachedSequence {
        private final double[] data_;
        private final int nrow_;
        private final double[] v3_;
        private int irow_ = -1;

        /**
         * Constructor.
         *
         * @param  data  3n-element array
         */
        DoubleSequence3( double[] data ) {
            data_ = data;
            nrow_ = data.length / 3;
            v3_ = new double[ 3 ];
            assert data.length % 3 == 0;
        }
        public boolean next() {
            return ++irow_ < nrow_;
        }
        public Object getObjectValue() {
            int ipos = irow_ * 3;
            v3_[ 0 ] = data_[ ipos++ ];
            v3_[ 1 ] = data_[ ipos++ ];
            v3_[ 2 ] = data_[ ipos++ ];
            return v3_;
        }
        public double getDoubleValue() {
            return Double.NaN;
        }
        public int getIntValue() {
            return Integer.MIN_VALUE;
        }
        public boolean getBooleanValue() {
            return false;
        }
    }

    /**
     * Float[3]-yielding CachedSequence implementation
     * based on a float array.
     */
    private static class FloatSequence3 implements CachedSequence {
        private final float[] data_;
        private final int nrow_;
        private final float[] v3_;
        private int irow_ = -1;

        /**
         * Constructor.
         *
         * @param  data  3n-element array
         */
        FloatSequence3( float[] data ) {
            data_ = data;
            nrow_ = data.length / 3;
            v3_ = new float[ 3 ];
            assert data.length % 3 == 0;
        }
        public boolean next() {
            return ++irow_ < nrow_;
        }
        public Object getObjectValue() {
            int ipos = irow_ * 3;
            v3_[ 0 ] = data_[ ipos++ ];
            v3_[ 1 ] = data_[ ipos++ ];
            v3_[ 2 ] = data_[ ipos++ ];
            return v3_;
        }
        public double getDoubleValue() {
            return Double.NaN;
        }
        public int getIntValue() {
            return Integer.MIN_VALUE;
        }
        public boolean getBooleanValue() {
            return false;
        }
    }

    /**
     * Int[3]-yielding CachedSequence implementation
     * based on an int array.
     */
    private static class IntSequence3 implements CachedSequence {
        private final int[] data_;
        private final int nrow_;
        private final int[] v3_;
        private int irow_ = -1;

        /**
         * Constructor.
         *
         * @param  data  3n-element array
         */
        IntSequence3( int[] data ) {
            data_ = data;
            nrow_ = data.length / 3;
            v3_ = new int[ 3 ];
            assert data.length % 3 == 0;
        }
        public boolean next() {
            return ++irow_ < nrow_;
        }
        public Object getObjectValue() {
            int ipos = irow_ * 3;
            v3_[ 0 ] = data_[ ipos++ ];
            v3_[ 1 ] = data_[ ipos++ ];
            v3_[ 2 ] = data_[ ipos++ ];
            return v3_;
        }
        public double getDoubleValue() {
            return Double.NaN;
        }
        public int getIntValue() {
            return Integer.MIN_VALUE;
        }
        public boolean getBooleanValue() {
            return false;
        }
    }
}
