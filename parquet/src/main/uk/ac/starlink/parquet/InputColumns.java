package uk.ac.starlink.parquet;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.ColumnReader;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;
import uk.ac.starlink.util.ByteList;
import uk.ac.starlink.util.DoubleList;
import uk.ac.starlink.util.FloatList;
import uk.ac.starlink.util.IntList;
import uk.ac.starlink.util.LongList;
import uk.ac.starlink.util.PrimitiveList;
import uk.ac.starlink.util.ShortList;

/**
 * Provides InputColumn instances that know how to read parquet column data.
 *
 * @author   Mark Taylor
 * @since    24 Feb 2021
 */
public class InputColumns {

    public static final byte BAD_BYTE = Byte.MIN_VALUE;
    public static final short BAD_SHORT = Short.MIN_VALUE;
    public static final int BAD_INT = Integer.MIN_VALUE;
    public static final long BAD_LONG = Long.MIN_VALUE;

    /**
     * Private constructor prevents instantiation.
     */
    private InputColumns() {
    }

    /**
     * Returns an InputColumn for reading a given column from a parquet file.
     *
     * @param  schema  table schema
     * @param  path   table column identifier
     * @return   input column reader, or null if the column type
     *           is not supported
     */
    public static InputColumn<?> createInputColumn( MessageType schema,
                                                    String[] path ) {
        final Col<?> col = createCol( schema, path );
        if ( col == null ) {
            return null;
        }
        else {
            boolean isNullable = ! schema.getType( path[ 0 ] )
                                  .isRepetition( Type.Repetition.REQUIRED );
            ColumnDescriptor cdesc = schema.getColumnDescription( path );
            return createInputColumn( col, cdesc, isNullable );
        }
    }

    /**
     * Packages internally-generated column handling components
     * into an InputColumn for external use.
     *
     * @param  col  basic column reading object
     * @param  cdesc   column descriptor
     * @param  isNullable   false if column is known to contain no nulls
     */
    private static <T> InputColumn<?>
            createInputColumn( final Col<T> col,
                               ColumnDescriptor cdesc, boolean isNullable ) {
        final Class<T> clazz = col.getContentClass();
        return new InputColumn<T>() {
            public Class<T> getContentClass() {
                return clazz;
            }
            public Decoder<T> createDecoder() {
                return col.createDecoder();
            }
            public ColumnDescriptor getColumnDescriptor() {
                return cdesc; 
            }
            public boolean isNullable() {
                return isNullable;
            }
        };
    }

    /**
     * Creates a column reading object for a table in a column.
     *
     * @param  schema  table schema
     * @param  path   table column identifier
     * @return   column reader
     */
    private static Col<?> createCol( MessageType schema, String[] path ) {
        PrimitiveType scalarType = getScalarType( schema, path );
        if ( scalarType != null ) {
            return createScalarCol( scalarType );
        }
        PrimitiveType elType = getArrayElementType( schema, path );
        if ( elType != null ) {
            return createArrayCol( elType );
        }
        return null;
    }

    /**
     * Returns the primitive type associated with a scalar column, or null.
     *
     * @param  schema  table schema
     * @param  path   table column identifier
     * @return  primitive scalar type of column,
     *          or null if it's not a scalar column of supported type
     */
    private static PrimitiveType getScalarType( MessageType schema,
                                                String[] path ) {
        if ( path.length == 1 ) {
            Type t = schema.getType( path );
            if ( t.isPrimitive() &&
                 ! t.isRepetition( Type.Repetition.REPEATED ) ) {
                return t.asPrimitiveType();
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Returns the primitive type associated with the elements of
     * an array-valued column, or null.
     *
     * @param  schema  table schema
     * @param  path   table column identifier
     * @return   primitive type of column array elements,
     *           or null if it's not an array column of supported type
     */
    private static PrimitiveType getArrayElementType( MessageType schema,
                                                      String[] path ) {

        /* This may not be the only way to do array-valued columns,
         * and it doesn't look like the most obvious way to me,
         * but I've seen it in example parquet files (from pandas?). */
        if ( path.length == 3 ) {
            Type t0 = schema.getType( path[ 0 ] );
            Type t1 = schema.getType( path[ 0 ], path[ 1 ] );
            Type t2 = schema.getType( path );
            if ( ! t0.isPrimitive() &&
                 ! t1.isPrimitive() &&
                 t1.isRepetition( Type.Repetition.REPEATED ) &&
                 t2.isPrimitive() ) {
                return t2.asPrimitiveType();
            }
            else {
                return null;
            }
        }

        /* This generated by Enrique Utrilla's gaia_source examples. */
        else if ( path.length == 2 ) {
            Type t0 = schema.getType( path[ 0 ] );
            Type t1 = schema.getType( path );
            if ( ! t0.isPrimitive() &&
                 t1.isRepetition( Type.Repetition.REPEATED ) &&
                 t1.isPrimitive() ) {
                return t1.asPrimitiveType();
            }
            else {
                return null;
            }
        }

        /* This looks more obvious, but I haven't so far seen examples. */
        else if ( path.length == 1 ) {
            Type t = schema.getType( path );
            if ( t.isPrimitive() &&
                 t.isRepetition( Type.Repetition.REPEATED ) ) {
                return t.asPrimitiveType();
            }
            else {
                return null;
            }
        }

        /* There are probably other ways to do it, but don't try to
         * enumerate them without some evidence. */
        else {
            return null;
        }
    }

    /**
     * Returns a column reader for scalar data.
     *
     * @param  ptype   primitive type of scalar column
     * @return   column reader, or null if type not supported
     */
    private static Col<?> createScalarCol( PrimitiveType ptype ) {
        LogicalTypeAnnotation logType = ptype.getLogicalTypeAnnotation();
        PrimitiveType.PrimitiveTypeName ptName = ptype.getPrimitiveTypeName();

        /* See
         * https://github.com/apache/parquet-format/blob/master/LogicalTypes.md
         */
        switch ( ptName ) {
            case BOOLEAN:
                return new ScalarCol<Boolean>(
                           Boolean.class,
                           rdr -> Boolean.valueOf( rdr.getBoolean() ) );
            case INT32:
                final int nbit;
                final boolean isSigned;
                if ( logType instanceof LogicalTypeAnnotation
                                       .IntLogicalTypeAnnotation ) {
                    LogicalTypeAnnotation.IntLogicalTypeAnnotation intType =
                        (LogicalTypeAnnotation.IntLogicalTypeAnnotation)
                        logType;
                    nbit = intType.getBitWidth();
                    isSigned = intType.isSigned();
                }
                else {
                    nbit = 32;
                    isSigned = true;
                }
                if ( nbit == 8 && isSigned ) {
                    return new ScalarCol<Byte>(
                               Byte.class,
                               rdr -> Byte.valueOf( (byte) rdr.getInteger() ) );
                }
                else if ( nbit == 16 && isSigned ||
                          nbit == 8 && ! isSigned ) {
                    return new ScalarCol<Short>(
                               Short.class,
                               rdr -> Short.valueOf( (short) rdr.getInteger()));
                }
                else if ( nbit == 32 && isSigned ||
                          nbit == 16 && ! isSigned ) {
                    return new ScalarCol<Integer>(
                               Integer.class,
                               rdr -> Integer.valueOf( rdr.getInteger() ) );
                }
                else if ( nbit == 32 || ! isSigned ) {
                    return new ScalarCol<Long>(
                        Long.class,
                        rdr -> Long.valueOf(
                               Integer.toUnsignedLong( rdr.getInteger() ) ) );
                }
                else {
                    return null;
                }
            case INT64:
                return new ScalarCol<Long>(
                           Long.class,
                           rdr -> Long.valueOf( rdr.getLong() ) );
            case FLOAT:
                return new ScalarCol<Float>(
                           Float.class,
                           rdr -> Float.valueOf( rdr.getFloat() ) );
            case DOUBLE:
                return new ScalarCol<Double>(
                           Double.class,
                           rdr -> Double.valueOf( rdr.getDouble() ) );
            case BINARY:
                if ( logType instanceof LogicalTypeAnnotation
                                       .StringLogicalTypeAnnotation ) {
                    return new ScalarCol<String>(
                               String.class,
                               rdr -> rdr.getBinary().toStringUsingUTF8() );
                }
                else {
                    return new ScalarCol<byte[]>(
                               byte[].class,
                               rdr -> rdr.getBinary().getBytes() );
                }
            case FIXED_LEN_BYTE_ARRAY:
                // to-do: timestamps etc.
            case INT96:
            default:
                return null;
        }
    }

    /**
     * Returns a column reader for array data.
     *
     * @param  ptype   primitive type of array column elements
     * @return   column reader, or null if type not supported
     */
    private static Col<?> createArrayCol( PrimitiveType elType ) {
        LogicalTypeAnnotation logType = elType.getLogicalTypeAnnotation();
        PrimitiveType.PrimitiveTypeName ptName = elType.getPrimitiveTypeName();
        switch ( ptName ) {
            case INT32:
                final int nbit;
                final boolean isSigned;
                if ( logType instanceof LogicalTypeAnnotation
                                       .IntLogicalTypeAnnotation ) {
                    LogicalTypeAnnotation.IntLogicalTypeAnnotation intType =
                        (LogicalTypeAnnotation.IntLogicalTypeAnnotation)
                        logType;
                    nbit = intType.getBitWidth();
                    isSigned = intType.isSigned();
                }
                else {
                    nbit = 32;
                    isSigned = true;
                }
                if ( nbit == 8 && isSigned ) {
                    return new PrimitiveArrayCol<byte[],ByteList>(
                               byte[].class, ByteList::new,
                               (rdr, list) -> list.add( (byte)
                                                        rdr.getInteger() ),
                               list -> list.add( BAD_BYTE ) );
                }
                else if ( nbit == 16 && isSigned ||
                          nbit == 8 && ! isSigned ) {
                    return new PrimitiveArrayCol<short[],ShortList>(
                               short[].class, ShortList::new,
                               (rdr, list) -> list.add( (short)
                                                        rdr.getInteger() ),
                               list -> list.add( BAD_SHORT ) );
                }
                else if ( nbit == 32 && isSigned ||
                          nbit == 16 && isSigned ) {
                    return new PrimitiveArrayCol<int[],IntList>(
                               int[].class, IntList::new,
                               (rdr, list) -> list.add( rdr.getInteger() ),
                               list -> list.add( BAD_INT ) );
                }
                else if ( nbit == 32 || ! isSigned ) {
                    return new PrimitiveArrayCol<long[],LongList>(
                               long[].class, LongList::new,
                               (rdr, list) ->
                                   list.add( Integer.toUnsignedLong(
                                                 rdr.getInteger() ) ),
                               list -> list.add( BAD_LONG ) );
                }
                else {
                    return null;
                }
            case INT64:
                return new PrimitiveArrayCol<long[],LongList>(
                           long[].class, LongList::new,
                           (rdr, list) -> list.add( rdr.getLong() ),
                           list -> list.add( BAD_LONG ) );
            case FLOAT:
                return new PrimitiveArrayCol<float[],FloatList>(
                           float[].class, FloatList::new,
                           (rdr, list) -> list.add( rdr.getFloat() ),
                           list -> list.add( Float.NaN ) );
            case DOUBLE:
                return new PrimitiveArrayCol<double[],DoubleList>(
                           double[].class, DoubleList::new,
                           (rdr, list) -> list.add( rdr.getDouble() ),
                           list -> list.add( Double.NaN ) );
            case BINARY:
                if ( logType instanceof LogicalTypeAnnotation
                                       .StringLogicalTypeAnnotation ) {
                    return createStringArrayCol();
                }
                else {
                    return null;
                }
            case BOOLEAN:
                return createBooleanArrayCol();
            case FIXED_LEN_BYTE_ARRAY:
            case INT96:
            default:
                return null;
        }
    }

    /**
     * Returns a reader for boolean-array-valued columns.
     *
     * @return   boolean array reader
     */
    private static Col<boolean[]> createBooleanArrayCol() {
        final boolean[] array0 = new boolean[ 0 ];
        return new Col<boolean[]>() {
            public Class<boolean[]> getContentClass() {
                return boolean[].class;
            }
            public Decoder<boolean[]> createDecoder() {
                return new Decoder<boolean[]>() {
                    final BitSet bits_ = new BitSet();
                    boolean[] value_;
                    int n_;
                    public Class<boolean[]> getContentClass() {
                        return boolean[].class;
                    }
                    public void clearValue() {
                        value_ = null;
                        n_ = 0;
                    }
                    public void readItem( ColumnReader crdr ) {
                        bits_.set( n_++, crdr.getBoolean() );
                    }
                    public void readNull() {
                        bits_.clear( n_++ );
                    }
                    public boolean[] getValue() {
                        if ( value_ == null ) {
                            if ( n_ == 0 ) {
                                value_ = array0;
                            }
                            else {
                                value_ = new boolean[ n_ ];
                                for ( int i = 0; i < n_; i++ ) {
                                    value_[ i ] = bits_.get( i );
                                }
                            }
                        }
                        return value_;
                    }
                };
            }
        };
    }

    /**
     * Returns a reader for String-array-valued columns.
     *
     * @return  string array reader
     */
    private static Col<String[]> createStringArrayCol() {
        final String[] array0 = new String[ 0 ];
        return new Col<String[]>() {
            public Class<String[]> getContentClass() {
                return String[].class;
            }
            public Decoder<String[]> createDecoder() {
                return new Decoder<String[]>() {
                    final List<String> list_ = new ArrayList<String>();
                    String[] value_;
                    public Class<String[]> getContentClass() {
                        return String[].class;
                    }
                    public void clearValue() {
                        value_ = null;
                        list_.clear();
                    }
                    public void readItem( ColumnReader crdr ) {
                        list_.add( crdr.getBinary().toStringUsingUTF8() );
                    }
                    public void readNull() {
                        list_.add( null );
                    }
                    public String[] getValue() {
                        if ( value_ == null ) {
                            int n = list_.size();
                            value_ = n == 0 ? array0
                                            : list_.toArray( new String[ n ] );
                        }
                        return value_;
                    }
                };
            }
        };
    }

    /**
     * Factory for decoders.
     */
    private static interface Col<T> {

        /**
         * Returns the content class of produced decoders.
         *
         * @return   decoder output class
         */
        Class<T> getContentClass();

        /**
         * Creates a decoder.
         *
         * @return   new decoder
         */
        Decoder<T> createDecoder();
    }

    /**
     * Col implementation for scalar valued columns.
     */
    private static class ScalarCol<T> implements Col<T> {
        final Class<T> clazz_;
        final Function<ColumnReader,T> readFunc_;

        /**
         * Constructor.
         *
         * @param   clazz  content class
         * @param   readFunc   takes a typed primitive value from a ColumnReader
         */
        ScalarCol( Class<T> clazz, Function<ColumnReader,T> readFunc ) {
            clazz_ = clazz;
            readFunc_ = readFunc;
        }

        public Class<T> getContentClass() {
            return clazz_;
        }

        public Decoder<T> createDecoder() {
            return new Decoder<T>() {
                T value_; 
                public Class<T> getContentClass() {
                    return clazz_;
                }
                public void clearValue() {
                    value_ = null;
                }     
                public void readItem( ColumnReader crdr ) {
                    value_ = readFunc_.apply( crdr );
                }       
                public void readNull() {
                    assert value_ == null; // clearValue should have been called
                    value_ = null;
                }
                public T getValue() {
                    return value_;
                }
            };
        }
    }

    /**
     * Col implementation for array valued columns.
     */
    private static class PrimitiveArrayCol<T,L extends PrimitiveList>
            implements Col<T> {

        final Class<T> clazz_;
        final Supplier<L> listSupplier_;
        final BiConsumer<ColumnReader,L> append_;
        final Consumer<L> appendNull_;

        /**
         * Constructor.
         *
         * @param  clazz  array value class of column
         * @param  listSupplier  supplier of suitable primitive list instance
         * @param  append  copies a value from a column reader to a list
         * @param  appendNull  adds a null value to a list
         */
        PrimitiveArrayCol( Class<T> clazz, Supplier<L> listSupplier,
                           BiConsumer<ColumnReader,L> append,
                           Consumer<L> appendNull ) {
            clazz_ = clazz;
            listSupplier_ = listSupplier;
            append_ = append;
            appendNull_ = appendNull;
        }

        public Class<T> getContentClass() {
            return clazz_;
        }

        public Decoder<T> createDecoder() {
            return new Decoder<T>() {
                final L plist_ = listSupplier_.get();
                boolean hasValue_;
                T value_;
                public Class<T> getContentClass() {
                    return clazz_;
                }
                public void clearValue() {
                    hasValue_ = false;
                    plist_.clear();
                }
                public void readItem( ColumnReader crdr ) {
                    append_.accept( crdr, plist_ );
                }
                public void readNull() {
                    appendNull_.accept( plist_ );
                }
                public T getValue() {
                    if ( ! hasValue_ ) {
                        hasValue_ = true;
                        value_ = plist_.size() == 0
                               ? null
                               : clazz_.cast( plist_.toArray() );
                    }
                    return value_;
                }
            };
        }
    }
}
