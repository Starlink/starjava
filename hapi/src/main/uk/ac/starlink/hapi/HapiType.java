package uk.ac.starlink.hapi;

import java.util.Arrays;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.TimeMapper;

/**
 * Data type for a HAPI parameter.
 *
 * @param  <S>  scalar output type
 * @param  <A>  array output type
 *
 * @author   Mark Taylor
 * @since    11 Jan 2024
 */
public abstract class HapiType<S,A> {

    private final String identifier_;
    private final Class<S> scalarClazz_;
    private final Class<A> arrayClazz_;
    private final int nbyte_;

    /** String type. */
    public static final HapiType<String,String[]> STRING;

    /** Restricted ISO-8601 timestamp type. */
    public static final HapiType<String,String[]> ISOTIME;

    /** Floating point type. */
    public static final HapiType<Double,double[]> DOUBLE;

    /** Integer type. */
    public static final HapiType<Integer,int[]> INTEGER;

    /** All known types. */
    private static final HapiType<?,?>[] VALUES = new HapiType<?,?>[] {
        STRING = new StringType(),
        ISOTIME = new IsotimeType(),
        DOUBLE = new DoubleType(),
        INTEGER = new IntegerType(),
    };

    /**
     * Constructor.
     *
     * @param  identifier  datatype name
     * @param  scalarClazz   output class for scalar values
     * @param  arrayClazz   output class for array values
     * @param  nbyte  number of bytes per primitive in binary serialization
     */
    private HapiType( String identifier, Class<S> scalarClazz,
                      Class<A> arrayClazz, int nbyte ) {
        identifier_ = identifier;
        scalarClazz_ = scalarClazz;
        arrayClazz_ = arrayClazz;
        nbyte_ = nbyte;
    }

    /**
     * Returns the name for this type, which is also the
     * value of the "type" member in a corresponding Parameter JSON object.
     *
     * @return  type name
     */
    public String getIdentifier() {
        return identifier_;
    }

    /**
     * Returns the output class for scalar values.
     *
     * @return  scalar class
     */
    public Class<S> getScalarClass() {
        return scalarClazz_;
    }

    /**
     * Returns the output class for array values.
     *
     * @return  array class
     */
    public Class<A> getArrayClass() {
        return arrayClazz_;
    }

    /**
     * Returns the number of bytes required for a scalar element
     * with this type and the supplied length value.
     * The <code>paramLength</code> is as supplied by
     * {@link HapiParam#getLength}.
     *
     * @param  paramLength   string length, or -1 for non-string types
     * @return   binary byte count for a scalar element of this type
     */
    public int getByteCount( int paramLength ) {
        return nbyte_ > 0 ? nbyte_
                          : paramLength;
    }

    /**
     * Returns a scalar value of this type from its text representation.
     *
     * @param  txt  text field as found in CSV
     * @return   typed scalar value
     */
    public abstract S readStringScalar( String txt );

    /**
     * Returns a scalar value of this type from its binary representation.
     *
     * @param  buf  bytes representing value as found in binary format stream
     * @param  ipos   starting position of value in buf
     * @param  leng  length of value in bytes
     * @return  typed scalar value
     */
    public abstract S readBinaryScalar( byte[] buf, int ipos, int leng );

    /**
     * Returns an array value of this type from an array of string
     * representations.
     *
     * @param  txts  text fields as found in CSV, elements may be null
     * @param  ipos  index of first element in txts to read
     * @param  nel   number of elements in txts to read
     * @return  typed array value, same length as input
     */
    public abstract A readStringArray( String[] txts, int ipos, int nel );

    /**
     * Returns an array value of this type from a binary buffer.
     *
     * @param  buf  bytes representing values as found in binary format stream
     * @param  ipos  starting position of values in buf
     * @param  elSize  number of bytes per value
     * @param  nel  number of values to decode
     * @return  typed array value with <code>nel</code> elements
     */
    public abstract A readBinaryArray( byte[] buf, int ipos,
                                       int elSize, int nel );

    /**
     * Replaces array elements matching a given fill value
     * in place with a suitable blank value representation.
     *
     * @param  array  array whose elements can be replaced
     * @param  fill   non-null value for which corresponding array
     *                elements should be blanked
     */
    public abstract void applyFills( A array, S fill );

    /**
     * Performs customisation appropriate to this type on a
     * metadata info object that will represent it.
     * This only changes less-essential metadata, basic things
     * like content class will be handled elsewhere.
     * The correct action may be a no-op.
     *
     * @param  info  metadata object to modify in place
     */
    public abstract void adjustInfo( DefaultValueInfo info );

    @Override
    public String toString() {
        return identifier_;
    }

    /**
     * Returns a type value given the identifier string as found
     * in the "type" member of a Parameter JSON object.
     *
     * @param  txt  identifier string
     * @return  type instance, or null if no match
     */
    public static HapiType<?,?> fromText( String txt ) {
        return Arrays.stream( VALUES )
              .filter( s -> s.identifier_.equalsIgnoreCase( txt ) )
              .findAny()
              .orElse( null );
    }

    /**
     * HapiType implementation for string values.
     */
    private static class StringType extends HapiType<String,String[]> {

        private final Consumer<DefaultValueInfo> infoAdjuster_;

        /**
         * Constructor.
         *
         * @param  name  type name
         * @param  infoAdjuster  implementation for {@link HapiType#adjustInfo}
         */
        StringType( String name, Consumer<DefaultValueInfo> infoAdjuster ) {
            super( name, String.class, String[].class, -1 );
            infoAdjuster_ = infoAdjuster;
        }

        StringType() {
            this( "string", info -> {} );
        }

        public String readStringScalar( String txt ) {
            return txt;
        }

        public String readBinaryScalar( byte[] buf, int ipos, int leng ) {
            return readUtf8( buf, ipos, leng );
        }

        public String[] readStringArray( String[] txts, int ipos, int nel ) {
            String[] out = new String[ nel ];
            System.arraycopy( txts, ipos, out, 0, nel );
            return out;
        }

        public String[] readBinaryArray( byte[] buf, int ipos,
                                         int elSize, int nel ) {
            String[] out = new String[ nel ];
            for ( int i = 0; i < nel; i++ ) {
                out[ i ] = readUtf8( buf, ipos, elSize );
                ipos += elSize;
            }
            return out;
        }

        public void applyFills( String[] array, String fill ) {
            for ( int i = 0; i < array.length; i++ ) {
                if ( fill.equals( array[ i ] ) ) {
                    array[ i ] = null;
                }
            }
        }

        public void adjustInfo( DefaultValueInfo info ) {
            infoAdjuster_.accept( info );
        }

        /**
         * Reads a UTF-8-encoded string from a byte buffer.
         * Any content following a NUL character is ignored.
         *
         * @param  buf  buffer containing bytes
         * @param  ipos   start position of string
         * @param  leng  maximum length of string in bytes
         * @return  string
         */
        private static String readUtf8( byte[] buf, int ipos, int leng ) {
            int iend = ipos + leng;
            for ( int i = ipos; i < iend; i++ ) {
                if ( buf[ i ] == 0 ) {
                    iend = i;
                }
            }
            return new String( buf, ipos, iend - ipos, StandardCharsets.UTF_8 );
        }
    }

    /**
     * HapiType implementation for Isotime values.
     */
    private static class IsotimeType extends StringType {
        IsotimeType() {
            super( "isotime", info -> {
                info.setXtype( "timestamp" );
                info.setDomainMappers( new DomainMapper[] {
                    TimeMapper.ISO_8601,
                } );
            } );
        }
    }

    /**
     * HapiType implementation for integer values.
     */
    private static class IntegerType extends HapiType<Integer,int[]> {

        /** Value used for blanks in integer array return values. */
        static final int BLANK_INT_ARRAY_VALUE = 0;

        IntegerType() {
            super( "integer", Integer.class, int[].class, 4 );
        }

        public Integer readStringScalar( String txt ) {
            try {
                return Integer.parseInt( txt );
            }
            catch ( NumberFormatException e ) {
                return null;
            }
        }

        public Integer readBinaryScalar( byte[] buf, int ipos, int leng ) {
            return Integer.valueOf( readBinaryInt( buf, ipos ) );
        }

        public int[] readStringArray( String[] txts, int ipos, int nel ) {
            int[] out = new int[ nel ];
            for ( int i = 0; i < nel; i++ ) {
                String txt = txts[ ipos + i ];
                int ival;
                if ( txt == null || txt.length() == 0 ) {
                    ival = BLANK_INT_ARRAY_VALUE;
                }
                else {
                    try {
                        ival = Integer.parseInt( txt );
                    }
                    catch ( NumberFormatException e ) {
                        ival = BLANK_INT_ARRAY_VALUE;
                    }
                }
                out[ i ] = ival;
            }
            return out;
        }

        public int[] readBinaryArray( byte[] buf, int ipos,
                                      int elSize, int nel  ) {
            int[] out = new int[ nel ];
            for ( int i = 0; i < nel; i++ ) {
                out[ i ] = readBinaryInt( buf, ipos );
                ipos += 4;
            }
            return out;
        }

        public void applyFills( int[] array, Integer fill ) {
            int ifill = fill.intValue();
            for ( int i = 0; i < array.length; i++ ) {
                if ( array[ i ] == ifill ) {
                    array[ i ] = BLANK_INT_ARRAY_VALUE;
                }
            }
        }

        public void adjustInfo( DefaultValueInfo info ) {
        }

        /**
         * Reads a little-endian signed 4-byte integer from a byte buffer.
         *
         * @param  buf  buffer
         * @param  ipos  starting position for read,
         *               must be at least 4 bytes from the end of the buffer
         * @return  integer value
         */
        private static int readBinaryInt( byte[] buf, int ipos ) {
            return ( ( buf[ ipos + 0 ] & 0xff )       )
                 | ( ( buf[ ipos + 1 ] & 0xff ) <<  8 )
                 | ( ( buf[ ipos + 2 ] & 0xff ) << 16 )
                 | ( ( buf[ ipos + 3 ] & 0xff ) << 24 );
        }
    }

    /**
     * HapiType implementation for floating point values.
     */
    private static class DoubleType extends HapiType<Double,double[]> {

        DoubleType() {
            super( "double", Double.class, double[].class, 8 );
        }

        public Double readStringScalar( String txt ) {
            try {
                return Double.parseDouble( txt );
            }
            catch ( NumberFormatException e ) {
                return null;
            }
        }

        public Double readBinaryScalar( byte[] buf, int ipos, int leng ) {
            return Double.valueOf( readBinaryDouble( buf, ipos ) );
        }

        public double[] readStringArray( String[] txts, int ipos, int nel ) {
            double[] out = new double[ nel ];
            for ( int i = 0; i < nel; i++ ) {
                String txt = txts[ ipos + i ];
                double dval;
                if ( txt == null || txt.length() == 0 ) {
                    dval = Double.NaN;
                }
                else {
                    try {
                        dval = Double.parseDouble( txt );
                    }
                    catch ( NumberFormatException e ) {
                        dval = Double.NaN;
                    }
                }
                out[ i ] = dval;
            }
            return out;
        }

        public double[] readBinaryArray( byte[] buf, int ipos,
                                         int elSize, int nel ) {
            double[] out = new double[ nel ];
            for ( int i = 0; i < nel; i++ ) {
                out[ i ] = readBinaryDouble( buf, ipos );
                ipos += 8;
            }
            return out;
        }

        public void applyFills( double[] array, Double fill ) {
            double dfill = fill.doubleValue();
            for ( int i = 0; i < array.length; i++ ) {
                if ( array[ i ] == dfill ) {
                    array[ i ] = Double.NaN;
                }
            }
        }

        public void adjustInfo( DefaultValueInfo info ) {
        }

        /**
         * Reads a little-endian IEEE 764 8-bit floating point value
         * from a byte buffer.
         *
         * @param  buf  buffer
         * @param  ipos  starting position for read,
         *               must be at least 8 bytes from the end of the buffer
         * @return   floating point value
         */
        private static double readBinaryDouble( byte[] buf, int ipos ) {
            long bits = ( (long) ( buf[ ipos + 0 ] & 0xff )       )
                      | ( (long) ( buf[ ipos + 1 ] & 0xff ) <<  8 )
                      | ( (long) ( buf[ ipos + 2 ] & 0xff ) << 16 )
                      | ( (long) ( buf[ ipos + 3 ] & 0xff ) << 24 )
                      | ( (long) ( buf[ ipos + 4 ] & 0xff ) << 32 )
                      | ( (long) ( buf[ ipos + 5 ] & 0xff ) << 40 )
                      | ( (long) ( buf[ ipos + 6 ] & 0xff ) << 48 )
                      | ( (long) ( buf[ ipos + 7 ] & 0xff ) << 56 );
            return Double.longBitsToDouble( bits );
        }
    }
}
