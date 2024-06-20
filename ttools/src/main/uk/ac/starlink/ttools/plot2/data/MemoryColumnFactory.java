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
import uk.ac.starlink.util.LongList;
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
        map.put( StorageType.LONG, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return nrow >= 0 ? new FixedLongColumn( nrow )
                                 : new UnknownLongColumn();
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
                          protected CachedReader createReader( int[] data ) {
                              return new IntReader3( data );
                          }
                      }
                    : new UnknownIntArrayColumn( 3 ) {
                          protected CachedReader createReader( int[] data ) {
                              return new IntReader3( data );
                          }
                      };
            }
        } );
        map.put( StorageType.DOUBLE3, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return nrow >= 0
                    ? new FixedDoubleArrayColumn( 3, nrow ) {
                          protected CachedReader createReader( double[] data ) {
                              return new DoubleReader3( data );
                          }
                      }
                    : new UnknownDoubleArrayColumn( 3 ) {
                          protected CachedReader createReader( double[] data ) {
                              return new DoubleReader3( data );
                          }
                      };
            }
        } );
        map.put( StorageType.FLOAT3, new ColumnCreator() {
            public CachedColumn createColumn( long nrow ) {
                return nrow >= 0
                    ? new FixedFloatArrayColumn( 3, nrow ) {
                          protected CachedReader createReader( float[] data ) {
                              return new FloatReader3( data );
                          }
                      }
                    : new UnknownFloatArrayColumn( 3 ) {
                          protected CachedReader createReader( float[] data ) {
                              return new FloatReader3( data );
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
     * Converts an object to a long.
     *
     * @param  obj, presumed numeric
     * @return  numerical value of <code>obj</code>
     */
    private static long toLong( Object obj ) {
        return ((Number) obj).longValue();
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
        }
        public long getRowCount() {
            return irow_;
        }
        public CachedReader createReader() {
            return new BitSetReader( mask_ );
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
        }

        public long getRowCount() {
            return irow_;
        }

        public CachedReader createReader() {
            return new ObjectArrayReader<T>( data_ );
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
        }

        public long getRowCount() {
            return irow_;
        }

        public CachedReader createReader() {
            return new DoubleArrayReader( data_ );
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
        }

        public long getRowCount() {
            return irow_;
        }

        public CachedReader createReader() {
            return new FloatArrayReader( data_ );
        }
    }

    /**
     * CachedColumn implementation for long values, column length is known.
     */
    private static class FixedLongColumn implements CachedColumn {
        private final int nrow_;
        private final long[] data_;
        private int irow_;

        /**
         * Constructor.
         *
         * @param  nrow  column length
         */
        FixedLongColumn( long nrow ) {
            if ( nrow > Integer.MAX_VALUE ) {
                throw new UnsupportedOperationException( "Too long " + nrow );
            }
            nrow_ = (int) nrow;
            data_ = new long[ nrow_ ];
        }

        public void add( Object value ) {
            data_[ irow_++ ] = toLong( value );
        }

        public void endAdd() {
        }

        public long getRowCount() {
            return irow_;
        }

        public CachedReader createReader() {
            return new LongArrayReader( data_ );
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
        }

        public long getRowCount() {
            return irow_;
        }

        public CachedReader createReader() {
            return new IntArrayReader( data_ );
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
        }

        public long getRowCount() {
            return irow_;
        }

        public CachedReader createReader() {
            return new ShortArrayReader( data_ );
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
        }

        public long getRowCount() {
            return irow_;
        }

        public CachedReader createReader() {
            return new ByteArrayReader( data_ );
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
        }

        public long getRowCount() {
            return ipos_ / ncol_;
        }

        public CachedReader createReader() {
            return createReader( data_ );
        }

        /**
         * Returns a CachedReader based on a single vector of floating
         * point data, with columns varying quickest.
         *
         * @param  data  array data
         */
        protected abstract CachedReader createReader( double[] data );
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
        }

        public long getRowCount() {
            return ipos_ / ncol_;
        }

        public CachedReader createReader() {
            return createReader( data_ );
        }

        /**
         * Returns a CachedReader based on a single vector of floating
         * point data, with columns varying quickest.
         *
         * @param  data  array data
         */
        protected abstract CachedReader createReader( float[] data );
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
        }

        public long getRowCount() {
            return ipos_ / ncol_;
        }

        public CachedReader createReader() {
            return createReader( data_ );
        }

        /**
         * Returns a CachedReader based on a single vector of integer
         * data, with columns varying quickest.
         *
         * @param   data   array data
         */
        protected abstract CachedReader createReader( int[] data );
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
         *                 reader getObject method
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

        public long getRowCount() {
            return data_ == null ? list_.size() : data_.length;
        }

        public CachedReader createReader() {
            return new ObjectArrayReader<T>( data_ );
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

        public long getRowCount() {
            return data_ == null ? list_.size() : data_.length;
        }

        public CachedReader createReader() {
            return new DoubleArrayReader( data_ );
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

        public long getRowCount() {
            return data_ == null ? list_.size() : data_.length;
        }

        public CachedReader createReader() {
            return new FloatArrayReader( data_ );
        }
    }

    /**
     * CachedColumn implementation for long values, column length not known.
     */
    private static class UnknownLongColumn implements CachedColumn {
        private LongList list_;
        private long[] data_;

        UnknownLongColumn() {
            list_ = new LongList();
        }

        public void add( Object value ) {
            list_.add( toLong( value ) );
        }

        public void endAdd() {
            data_ = list_.toLongArray();
            list_ = null;
        }

        public long getRowCount() {
            return data_ == null ? list_.size() : data_.length;
        }

        public CachedReader createReader() {
            return new LongArrayReader( data_ );
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

        public long getRowCount() {
            return data_ == null ? list_.size() : data_.length;
        }

        public CachedReader createReader() {
            return new IntArrayReader( data_ );
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

        public long getRowCount() {
            return data_ == null ? list_.size() : data_.length;
        }

        public CachedReader createReader() {
            return new ShortArrayReader( data_ );
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

        public long getRowCount() {
            return data_ == null ? list_.size() : data_.length;
        }

        public CachedReader createReader() {
            return new ByteArrayReader( data_ );
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

        public long getRowCount() {
            return ( data_ == null ? list_.size() : data_.length ) / ncol_;
        }

        public CachedReader createReader() {
            return createReader( data_ );
        }

        /**
         * Returns a CachedReader based on a single vector of floating
         * point data, with columns varying quickest.
         *
         * @param  data  array data
         */
        protected abstract CachedReader createReader( double[] data );
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

        public long getRowCount() {
            return ( data_ == null ? list_.size() : data_.length ) / ncol_;
        }

        public CachedReader createReader() {
            return createReader( data_ );
        }

        /**
         * Returns a CachedReader based on a single vector of floating
         * point data, with columns varying quickest.
         *
         * @param  data  array data
         */
        protected abstract CachedReader createReader( float[] data );
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

        public long getRowCount() {
            return ( data_ == null ? list_.size() : data_.length ) / ncol_;
        }

        public CachedReader createReader() {
            return createReader( data_ );
        }

        /**
         * Returns a CachedReader based on a single vector of integer
         * data, with columns varying quickest.
         *
         * @param  data  array data
         */
        protected abstract CachedReader createReader( int[] data );
    }

    /**
     * Boolean-yielding CachedReader implementation based on a BitSet.
     */
    private static class BitSetReader implements CachedReader {
        private final BitSet mask_;

        /**
         * Constructor.
         *
         * @param  mask  bit vector
         */
        public BitSetReader( BitSet mask ) {
            mask_ = mask;
        }
        public Object getObjectValue( long ix ) {
            return Boolean.valueOf( mask_.get( (int) ix ) );
        }
        public boolean getBooleanValue( long ix ) {
            return mask_.get( (int) ix );
        }
        public double getDoubleValue( long ix ) {
            return Double.NaN;
        }
        public int getIntValue( long ix ) {
            return Integer.MIN_VALUE;
        }
        public long getLongValue( long ix ) {
            return Long.MIN_VALUE;
        }
    }

    /**
     * Object-yielding CachedReader implementation based on an Object array.
     */
    private static class ObjectArrayReader<T> implements CachedReader {
        private final T[] data_;

        /**
         * Constructor.
         *
         * @param   data  data array
         */
        public ObjectArrayReader( T[] data ) {
            data_ = data;
        }
        public T getObjectValue( long ix ) {
            return data_[ (int) ix ];
        }
        public double getDoubleValue( long ix ) {
            return Double.NaN;
        }
        public int getIntValue( long ix ) {
            return Integer.MIN_VALUE;
        }
        public long getLongValue( long ix ) {
            return Long.MIN_VALUE;
        }
        public boolean getBooleanValue( long ix ) {
            return false;
        }
    }

    /**
     * Double-yielding CachedReader implementation based on a double array.
     */
    private static class DoubleArrayReader implements CachedReader {
        private final double[] data_;

        /**
         * Constructor.
         *
         * @param   data  data array
         */
        DoubleArrayReader( double[] data ) {
            data_ = data;
        }
        public Object getObjectValue( long ix ) {
            return Double.valueOf( data_[ (int) ix ] );
        }
        public double getDoubleValue( long ix ) {
            return data_[ (int) ix ];
        }
        public int getIntValue( long ix ) {
            return (int) data_[ (int) ix ];
        }
        public long getLongValue( long ix ) {
            return (long) data_[ (int) ix ];
        }
        public boolean getBooleanValue( long ix ) {
            return false;
        }
    }

    /**
     * Double-yielding CachedReader implementation based on an float array.
     */
    private static class FloatArrayReader implements CachedReader {
        private final float[] data_;

        /**
         * Constructor.
         *
         * @param   data  data array
         */
        FloatArrayReader( float[] data ) {
            data_ = data;
        }
        public Object getObjectValue( long ix ) {
            return Float.valueOf( data_[ (int) ix ] );
        }
        public double getDoubleValue( long ix ) {
            return (double) data_[ (int) ix ];
        }
        public int getIntValue( long ix ) {
            return (int) data_[ (int) ix ];
        }
        public long getLongValue( long ix ) {
            return (long) data_[ (int) ix ];
        }
        public boolean getBooleanValue( long ix ) {
            return false;
        }
    }

    /**
     * Long-yielding CachedReader implementation based on a flat array.
     */
    private static class LongArrayReader implements CachedReader {
        private final long[] data_;

        /**
         * Constructor.
         */
        LongArrayReader( long[] data ) {
            data_ = data;
        }
        public Object getObjectValue( long ix ) {
            return Long.valueOf( data_[ (int) ix ] );
        }
        public double getDoubleValue( long ix ) {
            return (double) data_[ (int) ix ];
        }
        public int getIntValue( long ix ) {
            return (int) data_[ (int) ix ];
        }
        public long getLongValue( long ix ) {
            return data_[ (int) ix ];
        }
        public boolean getBooleanValue( long ix ) {
            return false;
        }
    }

    /**
     * Int-yielding CachedReader implementation based on an int array.
     */
    private static class IntArrayReader implements CachedReader {
        private final int[] data_;

        /**
         * Constructor.
         *
         * @param   data  data array
         */
        IntArrayReader( int[] data ) {
            data_ = data;
        }
        public Object getObjectValue( long ix ) {
            return Integer.valueOf( data_[ (int) ix ] );
        }
        public double getDoubleValue( long ix ) {
            return data_[ (int) ix ];
        }
        public int getIntValue( long ix ) {
            return data_[ (int) ix ];
        }
        public long getLongValue( long ix ) {
            return data_[ (int) ix ];
        }
        public boolean getBooleanValue( long ix ) {
            return false;
        }
    }

    /**
     * Int-yielding CachedReader implementation based on a short array.
     */
    private static class ShortArrayReader implements CachedReader {
        private final short[] data_;

        /**
         * Constructor.
         *
         * @param   data  data array
         */
        ShortArrayReader( short[] data ) {
            data_ = data;
        }
        public Object getObjectValue( long ix ) {
            return Short.valueOf( data_[ (int) ix ] );
        }
        public double getDoubleValue( long ix ) {
            return data_[ (int) ix ];
        }
        public int getIntValue( long ix ) {
            return data_[ (int) ix ];
        }
        public long getLongValue( long ix ) {
            return data_[ (int) ix ];
        }
        public boolean getBooleanValue( long ix ) {
            return false;
        }
    }

    /**
     * Int-yielding CachedReader implementation based on a byte array.
     */
    private static class ByteArrayReader implements CachedReader {
        private final byte[] data_;

        /**
         * Constructor.
         *
         * @param   data  data array
         */
        ByteArrayReader( byte[] data ) {
            data_ = data;
        }
        public Object getObjectValue( long ix ) {
            return Byte.valueOf( data_[ (int) ix ] );
        }
        public double getDoubleValue( long ix ) {
            return data_[ (int) ix ];
        }
        public int getIntValue( long ix ) {
            return data_[ (int) ix ];
        }
        public long getLongValue( long ix ) {
            return data_[ (int) ix ];
        }
        public boolean getBooleanValue( long ix ) {
            return false;
        }
    }

    /**
     * Double[3]-yielding CachedReader implementation
     * based on a double array.
     */
    private static class DoubleReader3 implements CachedReader {
        private final double[] data_;
        private final double[] v3_;

        /**
         * Constructor.
         *
         * @param  data  3n-element array
         */
        DoubleReader3( double[] data ) {
            data_ = data;
            v3_ = new double[ 3 ];
            if ( data.length % 3 != 0 ) {
                throw new IllegalArgumentException();
            }
        }
        public Object getObjectValue( long ix ) {
            int ipos = ((int) ix) * 3;
            v3_[ 0 ] = data_[ ipos++ ];
            v3_[ 1 ] = data_[ ipos++ ];
            v3_[ 2 ] = data_[ ipos   ];
            return v3_;
        }
        public double getDoubleValue( long ix ) {
            return Double.NaN;
        }
        public int getIntValue( long ix ) {
            return Integer.MIN_VALUE;
        }
        public long getLongValue( long ix ) {
            return Long.MIN_VALUE;
        }
        public boolean getBooleanValue( long ix ) {
            return false;
        }
    }

    /**
     * Float[3]-yielding CachedReader implementation
     * based on a float array.
     */
    private static class FloatReader3 implements CachedReader {
        private final float[] data_;
        private final float[] v3_;

        /**
         * Constructor.
         *
         * @param  data  3n-element array
         */
        FloatReader3( float[] data ) {
            data_ = data;
            v3_ = new float[ 3 ];
            if ( data.length % 3 != 0 ) {
                throw new IllegalArgumentException();
            }
        }
        public Object getObjectValue( long ix ) {
            int ipos = ((int) ix) * 3;
            v3_[ 0 ] = data_[ ipos++ ];
            v3_[ 1 ] = data_[ ipos++ ];
            v3_[ 2 ] = data_[ ipos   ];
            return v3_;
        }
        public double getDoubleValue( long ix ) {
            return Double.NaN;
        }
        public int getIntValue( long ix ) {
            return Integer.MIN_VALUE;
        }
        public long getLongValue( long ix ) {
            return Long.MIN_VALUE;
        }
        public boolean getBooleanValue( long ix ) {
            return false;
        }
    }

    /**
     * Int[3]-yielding CachedReader implementation
     * based on an int array.
     */
    private static class IntReader3 implements CachedReader {
        private final int[] data_;
        private final int[] v3_;

        /**
         * Constructor.
         *
         * @param  data  3n-element array
         */
        IntReader3( int[] data ) {
            data_ = data;
            v3_ = new int[ 3 ];
            if ( data.length % 3 != 0 ) {
                throw new IllegalArgumentException();
            }
        }
        public Object getObjectValue( long ix ) {
            int ipos = ((int) ix) * 3;
            v3_[ 0 ] = data_[ ipos++ ];
            v3_[ 1 ] = data_[ ipos++ ];
            v3_[ 2 ] = data_[ ipos   ];
            return v3_;
        }
        public double getDoubleValue( long ix ) {
            return Double.NaN;
        }
        public int getIntValue( long ix ) {
            return Integer.MIN_VALUE;
        }
        public long getLongValue( long ix ) {
            return Long.MIN_VALUE;
        }
        public boolean getBooleanValue( long ix ) {
            return false;
        }
    }
}
