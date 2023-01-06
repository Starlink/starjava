package uk.ac.starlink.pds4;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;
import gov.nasa.pds.label.object.FieldType;
import gov.nasa.pds.objectAccess.table.FieldAdapter;
import uk.ac.starlink.util.DoubleList;
import uk.ac.starlink.util.FloatList;
import uk.ac.starlink.util.IntList;
import uk.ac.starlink.util.LongList;
import uk.ac.starlink.util.ShortList;

/**
 * Adapts a FieldAdapter to return a typed value.
 * The NASA-provided FieldAdapter class does the work of reading bytes
 * or text to turn them into some numeric or string value,
 * but this class augments that to provide an object that knows what
 * type of object should be read.
 * An instance of this class may be used to read either scalar values
 * or (within a Field_Group_*) array values.
 *
 * @author   Mark Taylor
 * @since    24 Nov 2021
 */
public abstract class FieldReader<S,A> {

    private final FieldType ftype_;
    private final Class<S> scalarClazz_;
    private final Class<A> arrayClazz_;
    private static final boolean IS_SAFE = true;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.pds4" );

    /**
     * Constructor.
     *
     * @param  ftype   field type
     * @param  scalarClazz   field data content class for scalar values
     * @param  arrayClazz    field data content class for array values
     */
    private FieldReader( FieldType ftype,
                         Class<S> scalarClazz, Class<A> arrayClazz ) {
        ftype_ = ftype;
        scalarClazz_ = scalarClazz;
        arrayClazz_ = arrayClazz;
    }

    /**
     * Reads a typed scalar value from a buffer in accordance
     * with this field type.
     *
     * <p>The startBit and endBit arguments reflect their appearence in
     * the corresponding NASA classes, but I don't know what they do.
     *
     * @param   buf   byte buffer containing data
     * @param   offset   index into buf of byte at which data value begins
     * @param   length   number of bytes over which value is represented
     * @param   startBit   ??
     * @param   endBit     ??
     * @return   typed data value
     */
    public abstract S readScalar( byte[] buf, int offset, int length,
                                  int startBit, int endBit );

    /**
     * Reads an value from a buffer in accordance with this field type
     * and stores it in one element of a supplied typed array.
     * There is a limit to what null handling can be done in this case.
     *
     * <p>The startBit and endBit arguments reflect their appearence in
     * the corresponding NASA classes, but I don't know what they do.
     *
     * @param   buf   byte buffer containing data
     * @param   offset   index into buf of byte at which data value begins
     * @param   length   number of bytes over which value is represented
     * @param   startBit   ??
     * @param   endBit     ??
     * @param   array    array into which read value is stored
     * @param   iel    index into array a wich value is written
     */
    public abstract void readElement( byte[] buf, int offset, int length,
                                      int startBit, int endBit,
                                      A array, int iel );

    /**
     * Creates a new instance of an array value corresponding to this
     * reader's array type.
     *
     * @param  nel  array size
     * @return   new array
     */
    public abstract A createArray( int nel );

    /**
     * Returns the type of scalar object that this reader will read.
     *
     * @return  scalar content class
     */
    public Class<S> getScalarClass() {
        return scalarClazz_;
    }

    /**
     * Returns the type of array value into which this reader can store values.
     *
     * @return  array content class
     */
    public Class<A> getArrayClass() {
        return arrayClazz_;
    }

    /**
     * Returns the field type for this reader.
     *
     * @return   field type
     */
    public FieldType getFieldType() {
        return ftype_;
    }

    /**
     * Returns a FieldReader instance for a given FieldType.
     *
     * @param   ftype   field type
     * @param   blankTxts  array of string values representing field values
     *                     that are to be mapped to null when read
     * @return   field reader
     */
    public static FieldReader<?,?> getInstance( FieldType ftype,
                                                String[] blankTxts ) {
        FieldAdapter adapter0 = ftype.getAdapter();
        final FieldAdapter adapter = IS_SAFE
                                   ? new SafeFieldAdapter( adapter0 )
                                   : adapter0;
        switch ( ftype ) {
            case UTF8_STRING:
            case ASCII_ANYURI:
            case ASCII_BIBCODE:
            case ASCII_DATE:
            case ASCII_DATE_DOY:
            case ASCII_DATE_TIME:
            case ASCII_DATE_TIME_DOY:
            case ASCII_DATE_TIME_DOY_UTC:
            case ASCII_DATE_TIME_UTC:
            case ASCII_DATE_TIME_YMD:
            case ASCII_DATE_TIME_YMD_UTC:
            case ASCII_DATE_YMD:
            case ASCII_DIRECTORY_PATH_NAME:
            case ASCII_DOI:
            case ASCII_FILE_NAME:
            case ASCII_FILE_SPECIFICATION_NAME:
            case ASCII_LID:
            case ASCII_LIDVID:
            case ASCII_LIDVID_LID:
            case ASCII_MD5_CHECKSUM:
            case ASCII_STRING:
            case ASCII_TIME:
            case ASCII_VID:
            case COMPLEXLSB16:
            case COMPLEXLSB8:
            case COMPLEXMSB16:
            case COMPLEXMSB8:
            case SIGNEDBITSTRING:
            case UNSIGNEDBITSTRING:
            case UNKNOWN: {
                int nblank = blankTxts.length;
                final UnaryOperator<String> wrapString;
                if ( nblank == 0 ) {
                    wrapString = UnaryOperator.identity();
                }
                else if ( nblank == 1 ) {
                    String blank = blankTxts[ 0 ];
                    wrapString = s -> s.equals( blank ) ? null : s;
                }
                else {
                    wrapString = s -> {
                        for ( int i = 0; i < nblank; i++ ) {
                            if ( s.equals( blankTxts[ i ] ) ) {
                                return null;
                            }
                        }
                        return s;
                    };
                }
                return new FieldReader<String,String[]>
                                      ( ftype, String.class, String[].class ) {
                    public String readScalar( byte[] buf, int off, int leng,
                                              int startBit, int endBit ) {
                        String txt = adapter.getString( buf, off, leng,
                                                        startBit, endBit );
                        return txt == null ? null
                                           : wrapString.apply( txt.trim() );
                    }
                    public void readElement( byte[] buf, int off, int leng,
                                             int startBit, int endBit,
                                             String[] array, int iel ) {
                        String txt = adapter.getString( buf, off, leng,
                                                        startBit, endBit );
                        array[ iel ] = txt == null
                                     ? null
                                     : wrapString.apply( txt.trim() );
                    }
                    public String[] createArray( int n ) {
                        return new String[ n ];
                    }
                };
            }

            case ASCII_BOOLEAN:
                return new FieldReader<Boolean,boolean[]>
                                      ( ftype, Boolean.class, boolean[].class ){
                    public Boolean readScalar( byte[] buf, int off, int leng,
                                               int startBit, int endBit ) {
                        String txt = adapter.getString( buf, off, leng,
                                                        startBit, endBit );
                        txt = txt == null ? null : txt.trim();
                        if ( "true".equals( txt ) || "1".equals( txt ) ) {
                            return Boolean.TRUE;
                        }
                        else if ( "false".equals( txt ) || "0".equals( txt ) ) {
                            return Boolean.FALSE;
                        }
                        else {
                            return null;
                        }
                    }
                    public void readElement( byte[] buf, int off, int leng,
                                             int startBit, int endBit,
                                             boolean[] array, int iel ) {
                        String txt = adapter.getString( buf, off, leng,
                                                        startBit, endBit );
                        txt = txt == null ? null : txt.trim();
                        array[ iel ] = "true".equals( txt )
                                    || "1".equals( txt );
                    }
                    public boolean[] createArray( int n ) {
                        return new boolean[ n ];
                    }
                };

            case SIGNEDBYTE:
            case UNSIGNEDBYTE:
            case SIGNEDLSB2:
            case SIGNEDMSB2: {
                ShortList blankList = new ShortList();
                for ( String txt : blankTxts ) {
                    try {
                        blankList.add( Short.parseShort( txt ) );
                    }
                    catch ( NumberFormatException e ) {
                    }
                }
                short[] blanks = blankList.toShortArray();
                int nblank = blanks.length;
                final ShortFunction<Short> wrapShort;
                if ( nblank == 0 ) {
                    wrapShort = Short::valueOf;
                }
                else if ( nblank == 1 ) {
                    int blank = blanks[ 0 ];
                    wrapShort = s -> s == blank ? null : Short.valueOf( s );
                }
                else {
                    wrapShort = s -> {
                        for ( int i = 0; i < nblank; i++ ) {
                            if ( s == blanks[ i ] ) {
                                return null;
                            }
                        }
                        return Short.valueOf( s );
                    };
                }
                return new FieldReader<Short,short[]>
                                      ( ftype, Short.class, short[].class ) { 
                    public Short readScalar( byte[] buf, int off, int leng,
                                             int startBit, int endBit ) {
                        return wrapShort
                              .apply( adapter.getShort( buf, off, leng,
                                                        startBit, endBit ) );
                    }
                    public void readElement( byte[] buf, int off, int leng,
                                             int startBit, int endBit,
                                             short[] array, int iel ) {
                        array[ iel ] = adapter.getShort( buf, off, leng,
                                                         startBit, endBit );
                    }
                    public short[] createArray( int n ) {
                        return new short[ n ];
                    }
                };
            }

            case SIGNEDLSB4:
            case SIGNEDMSB4:
            case UNSIGNEDLSB2:
            case UNSIGNEDMSB2: {
                IntList blankList = new IntList();
                for ( String txt : blankTxts ) {
                    try {
                        blankList.add( Integer.parseInt( txt ) );
                    }
                    catch ( NumberFormatException e ) {
                    }
                }
                int[] blanks = blankList.toIntArray();
                int nblank = blanks.length;
                final IntFunction<Integer> wrapInt;
                if ( nblank == 0 ) {
                    wrapInt = Integer::valueOf;
                }
                else if ( nblank == 1 ) {
                    int blank = blanks[ 0 ];
                    wrapInt = i -> i == blank ? null : Integer.valueOf( i );
                }
                else {
                    wrapInt = j -> {
                        for ( int k = 0; k < nblank; k++ ) {
                            if ( j == blanks[ k ] ) {
                                return null;
                            }
                        }
                        return Integer.valueOf( j );
                    };
                }
                return new FieldReader<Integer,int[]>
                                      ( ftype, Integer.class, int[].class ) {
                    public Integer readScalar( byte[] buf, int off, int leng,
                                               int startBit, int endBit ) {
                        return wrapInt
                              .apply( adapter.getInt( buf, off, leng,
                                                      startBit, endBit ) );
                    }
                    public void readElement( byte[] buf, int off, int leng,
                                             int startBit, int endBit,
                                             int[] array, int iel ) {
                        array[ iel ] = adapter.getInt( buf, off, leng,
                                                       startBit, endBit );
                    }
                    public int[] createArray( int n ) {
                        return new int[ n ];
                    }
                };
            }

            case SIGNEDLSB8:
            case SIGNEDMSB8:
            case UNSIGNEDLSB4:
            case UNSIGNEDMSB4:
            case ASCII_INTEGER:
            case ASCII_NONNEGATIVE_INTEGER:
            case ASCII_NUMERIC_BASE16:
            case ASCII_NUMERIC_BASE2:
            case ASCII_NUMERIC_BASE8: {
                LongList blankList = new LongList();
                for ( String txt : blankTxts ) {
                    try {
                        blankList.add( Long.parseLong( txt ) );
                    }
                    catch ( NumberFormatException e ) {
                    }
                }
                long[] blanks = blankList.toLongArray();
                int nblank = blanks.length;
                final LongFunction<Long> wrapLong;
                if ( nblank == 0 ) {
                    wrapLong = Long::valueOf;
                }
                else if ( nblank == 1 ) {
                    long blank = blanks[ 0 ];
                    wrapLong = l -> l == blank ? null : Long.valueOf( l );
                }
                else {
                    wrapLong = l -> {
                        for ( int i = 0; i < nblank; i++ ) {
                            if ( l == blanks[ i ] ) {
                                return null;
                            }
                        }
                        return Long.valueOf( l );
                    };
                }
                return new FieldReader<Long,long[]>
                                      ( ftype, Long.class, long[].class ) {
                    public Long readScalar( byte[] buf, int off, int leng,
                                            int startBit, int endBit ) {
                        return wrapLong
                              .apply( adapter.getLong( buf, off, leng,
                                                       startBit, endBit ) );
                    }
                    public void readElement( byte[] buf, int off, int leng,
                                             int startBit, int endBit,
                                             long[] array, int iel ) {
                        array[ iel ] = adapter.getLong( buf, off, leng,
                                                        startBit, endBit );
                    }
                    public long[] createArray( int n ) {
                        return new long[ n ];
                    }
                };
            }

            case IEEE754LSBSINGLE:
            case IEEE754MSBSINGLE: {
                FloatList blankList = new FloatList();
                for ( String txt : blankTxts ) {
                    try {
                        blankList.add( Float.parseFloat( txt ) );
                    }
                    catch ( NumberFormatException e ) {
                    }
                }
                float[] blanks = blankList.toFloatArray();
                int nblank = blanks.length;
                final FloatFunction<Float> wrapFloat;
                final FloatUnaryOperator maskFloat;
                if ( nblank == 0 ) {
                    wrapFloat = Float::valueOf;
                    maskFloat = f -> f;
                }
                else if ( nblank == 1 ) {
                    float blank = blanks[ 0 ];
                    wrapFloat = f -> f == blank ? null : Float.valueOf( f );
                    maskFloat = f -> f == blank ? Float.NaN : f;
                }
                else {
                    wrapFloat = f -> {
                        for ( int i = 0; i < nblank; i++ ) {
                            if ( f == blanks[ i ] ) {
                                return null;
                            }
                        }
                        return Float.valueOf( f );
                    };
                    maskFloat = f -> {
                        for ( int i = 0; i < nblank; i++ ) {
                            if ( f == blanks[ i ] ) {
                                return Float.NaN;
                            }
                        }
                        return f;
                    };
                }
                return new FieldReader<Float,float[]>
                                      ( ftype, Float.class, float[].class ) {
                    public Float readScalar( byte[] buf, int off, int leng,
                                             int startBit, int endBit ) {
                        return wrapFloat
                              .apply( adapter.getFloat( buf, off, leng,
                                                        startBit, endBit ) );
                    }
                    public void readElement( byte[] buf, int off, int leng,
                                             int startBit, int endBit,
                                             float[] array, int iel ) {
                        array[ iel ] =
                            maskFloat
                           .applyAsFloat( adapter
                                         .getFloat( buf, off, leng,
                                                    startBit, endBit ) );
                    }
                    public float[] createArray( int n ) {
                        return new float[ n ];
                    }
                };
            }

            case ASCII_REAL:
            case IEEE754LSBDOUBLE:
            case IEEE754MSBDOUBLE: {
                DoubleList blankList = new DoubleList();
                for ( String txt : blankTxts ) {
                    try {
                        blankList.add( Double.parseDouble( txt ) );
                    }
                    catch ( NumberFormatException e ) {
                    }
                }
                double[] blanks = blankList.toDoubleArray();
                int nblank = blanks.length;
                final DoubleFunction<Double> wrapDouble;
                final DoubleUnaryOperator maskDouble;
                if ( nblank == 0 ) {
                    wrapDouble = Double::valueOf;
                    maskDouble = d -> d;
                }
                else if ( nblank == 1 ) {
                    double blank = blanks[ 0 ];
                    wrapDouble = d -> d == blank ? null : Double.valueOf( d );
                    maskDouble = d -> d == blank ? Double.NaN : d;
                }
                else {
                    wrapDouble = d -> {
                        for ( int i = 0; i < nblank; i++ ) {
                            if ( d == blanks[ i ] ) {
                                return null;
                            }
                        }
                        return Double.valueOf( d );
                    };
                    maskDouble = d -> {
                        for ( int i = 0; i < nblank; i++ ) {
                            if ( d == blanks[ i ] ) {
                                return Double.NaN;
                            }
                        }
                        return d;
                    };
                }
                return new FieldReader<Double,double[]>
                                      ( ftype, Double.class, double[].class ) {
                    public Double readScalar( byte[] buf, int off, int leng,
                                              int startBit, int endBit ) {
                        return wrapDouble
                              .apply( adapter.getDouble( buf, off, leng,
                                                         startBit, endBit ) );
                    }
                    public void readElement( byte[] buf, int off, int leng,
                                             int startBit, int endBit,
                                             double[] array, int iel ) {
                        array[ iel ] =
                            maskDouble
                           .applyAsDouble( adapter
                                          .getDouble( buf, off, leng,
                                                      startBit, endBit ) );
                    }
                    public double[] createArray( int n ) {
                        return new double[ n ];
                    }
                };
            }

            case UNSIGNEDLSB8:
            case UNSIGNEDMSB8: {
                List<BigInteger> blankList = new ArrayList<>();
                for ( String txt : blankTxts ) {
                    try {
                        blankList.add( new BigInteger( txt ) );
                    }
                    catch ( NumberFormatException e ) {
                    }
                }
                BigInteger[] blanks = blankList.toArray( new BigInteger[ 0 ] );
                int nblank = blanks.length;
                final BigInteger max = BigInteger.valueOf( Long.MAX_VALUE );
                final BigInteger min = BigInteger.valueOf( Long.MIN_VALUE );
                final Function<BigInteger,Long> wrapBigint0 =
                    b -> b.compareTo( min ) >= 1 && b.compareTo( max ) <= 1
                             ? b.longValue()
                             : null;
                final Function<BigInteger,Long> wrapBigint;
                if ( nblank == 0 ) {
                    wrapBigint = wrapBigint0;
                }
                else if ( nblank == 1 ) {
                    BigInteger blank = blanks[ 0 ];
                    wrapBigint =
                        b -> blank.equals( b ) ? null : wrapBigint0.apply( b );
                }
                else {
                    wrapBigint = b -> {
                        for ( int i = 0; i < nblank; i++ ) {
                            if ( blanks[ i ].equals( b ) ) {
                                return null;
                            }
                        }
                        return wrapBigint0.apply( b );
                    };
                }
                return new FieldReader<Long,long[]>
                                      ( ftype, Long.class, long[].class ) {
                    public Long readScalar( byte[] buf, int off, int leng,
                                            int startBit, int endBit ) {
                        return wrapBigint
                              .apply( adapter.getBigInteger( buf, off, leng,
                                                             startBit, endBit));
                    }
                    public void readElement( byte[] buf, int off, int leng,
                                             int startBit, int endBit,
                                             long[] array, int iel ) {
                        array[ iel ] = adapter.getLong( buf, off, leng,
                                                        startBit, endBit );
                    }
                    public long[] createArray( int n ) {
                        return new long[ n ];
                    }
                };
            }

            default:
                logger_.warning( "Unknown PDS4 FieldType " + ftype
                               + ", treat as String" );
                assert false;
                return new FieldReader<String,String[]>
                                      ( ftype, String.class, String[].class ) {
                    public String readScalar( byte[] buf, int off, int leng,
                                              int startBit, int endBit ) {
                        String txt = adapter.getString( buf, off, leng,
                                                        startBit, endBit );
                        return txt == null ? null : txt.trim();
                    }
                    public void readElement( byte[] buf, int off, int leng,
                                             int startBit, int endBit,
                                             String[] array, int iel ) {
                        String txt = adapter.getString( buf, off, leng,
                                                        startBit, endBit );
                        array[ iel ] = txt == null ? null : txt.trim();
                    }
                    public String[] createArray( int n ) {
                        return new String[ n ];
                    }
                };
        }
    }

    /**
     * Functional interface missing from java.util.function.
     */
    @FunctionalInterface
    private interface ShortFunction<R> {
        R apply( short value );
    }

    /**
     * Functional interface missing from java.util.function.
     */
    @FunctionalInterface
    private interface FloatFunction<R> {
        R apply( float value );
    }

    /**
     * Functional interface missing from java.util.function.
     */
    @FunctionalInterface
    private interface FloatUnaryOperator {
        float applyAsFloat( float value );
    }
}
