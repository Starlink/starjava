package uk.ac.starlink.ecsv;

import java.lang.reflect.Array;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.Tables;

/**
 * Converts values for a given column into strings suitable for inclusion
 * in the body of an ECSV file.
 *
 * @author   Mark Taylor
 * @since    28 Apr 2020
 */
public abstract class EcsvEncoder {

    private final String datatype_;
    private final String subtype_;
    private static final String ECSV_TRUE = "True";
    private static final String ECSV_FALSE = "False";
    private static final String JSON_BLANK = "null";
    private static final String JSON_TRUE = "true";
    private static final String JSON_FALSE = "false";

    /**
     * Constructor.
     *
     * @param  datatype  datatype name as declared by ECSV
     * @param  subtype   subtype string as declared by ECSV, may be null
     */
    protected EcsvEncoder( String datatype, String subtype ) {
        datatype_ = datatype;
        subtype_ = subtype;
    }

    /**
     * Constructor with blank subtype.
     *
     * @param  datatype  datatype name as declared by ECSV
     */
    private EcsvEncoder( String datatype ) {
        this( datatype, null );
    }

    /**
     * Formats a value of this encoder's datatype for insertion into
     * the body of an ECSV file.  The output string must include any
     * required quoting, for instance it may not include unescaped
     * delimiters, newlines or double quote characters.
     * As a special case, null may be returned instead if no conversion
     * can be performed.
     *
     * @param   value   typed value to encode
     * @return   formatted string representation, or null
     */
    public abstract String encode( Object value );

    /**
     * Returns the ECSV datatype handled by this encoder.
     *
     * @return   datatype string
     */
    public String getDatatype() {
        return datatype_;
    }

    /**
     * Returns the ECSV subtype for this decoder.
     *
     * @return  subtype, may be null
     */
    public String getSubtype() {
        return subtype_;
    }

    /**
     * Returns an encoder suitable for encoding data from a column with
     * given metadata.
     *
     * @param  info  column metadata
     * @param  delimiter   delimiter value, must be comma or space,
     *                     required to determine quoting details
     * @return    encoder, or null for unknown or unsupported data type
     */
    public static EcsvEncoder createEncoder( ColumnInfo info,
                                             final char delimiter ) {
        Class<?> clazz = info.getContentClass();
        if ( Boolean.class.equals( clazz ) ) {
            return new EcsvEncoder( "bool" ) {
                public String encode( Object value ) {
                    if ( Boolean.TRUE.equals( value ) ) {
                        return ECSV_TRUE;
                    }
                    else if ( Boolean.FALSE.equals( value ) ) {
                        return ECSV_FALSE;
                    }
                    else {
                        return null;
                    }
                }
            };
        }
        else if ( Byte.class.equals( clazz ) ) {
            return new EcsvEncoder( "int8" ) {
                public String encode( Object value ) {
                    return value instanceof Byte
                         ? ((Byte) value).toString()
                         : null;
                }
            };
        }
        else if ( Short.class.equals( clazz ) ) {
            boolean isUnsignedByte =
                Boolean.TRUE
               .equals( info.getAuxDatumValue( Tables.UBYTE_FLAG_INFO,
                                               Boolean.class ) );
            return new EcsvEncoder( isUnsignedByte ? "uint8" : "int16" ) {
                public String encode( Object value ) {
                    return value instanceof Short
                         ? ((Short) value).toString()
                         : null;
                }
            };
        }
        else if ( Integer.class.equals( clazz ) ) {
            return new EcsvEncoder( "int32" ) {
                public String encode( Object value ) {
                    return value instanceof Integer
                         ? ((Integer) value).toString()
                         : null;
                }
            };
        }
        else if ( Long.class.equals( clazz ) ) {
            return new EcsvEncoder( "int64" ) {
                public String encode( Object value ) {
                    return value instanceof Long
                         ? ((Long) value).toString()
                         : null;
                }
            };
        }
        else if ( Float.class.equals( clazz ) ) {
            return new EcsvEncoder( "float32" ) {
                public String encode( Object value ) {
                    if ( value instanceof Float ) {
                        Float fval = (Float) value;
                        float f = fval.floatValue();
                        if ( Float.isFinite( f ) ) {
                            return fval.toString();
                        }
                        else if ( Float.isNaN( f ) ) {
                            return null;
                        }
                        else if ( Float.isInfinite( f ) ) {
                            return f > 0 ? "inf" : "-inf";
                        }
                        else {
                            assert false;
                            return null;
                        }
                    }
                    else if ( value == null ) {
                        return null;
                    }
                    else {
                        return null;
                    }
                }
            };
        }
        else if ( Double.class.equals( clazz ) ) {
            return new EcsvEncoder( "float64" ) {
                public String encode( Object value ) {
                    if ( value instanceof Double ) {
                        Double dval = (Double) value;
                        double d = dval.doubleValue();
                        if ( Double.isFinite( d ) ) {
                            return dval.toString();
                        }
                        else if ( Double.isNaN( d ) ) {
                            return null;
                        }
                        else if ( Double.isInfinite( d ) ) {
                            return d > 0 ? "inf" : "-inf";
                        }
                        else {
                            assert false;
                            return null;
                        }
                    }
                    else if ( value == null ) {
                        return null;
                    }
                    else {
                        return null;
                    }
                }
            };
        }
        else if ( String.class.equals( clazz ) ) {
            return new EcsvEncoder( "string" ) {
                public String encode( Object value ) {
                    return value instanceof String
                         ? quoteString( (String) value, delimiter )
                         : null;
                }
            };
        }
        else if ( Character.class.equals( clazz ) ) {
            return new EcsvEncoder( "string" ) {
                public String encode( Object value ) {
                    return value instanceof Character
                         ? quoteString( ((Character) value).toString(),
                                        delimiter )
                         : null;
                }
            };
        }
        else if ( boolean[].class.equals( clazz ) ) {
            return createArrayEncoder( "bool", info.getShape(),
                                       boolean[].class, delimiter,
                ( arr, i ) -> arr[ i ] ? JSON_TRUE : JSON_FALSE
            );
        }
        else if ( byte[].class.equals( clazz ) ) {
            return createArrayEncoder( "int8", info.getShape(),
                                       byte[].class, delimiter,
                ( arr, i ) -> Byte.toString( arr[ i ] )
            );
        }
        else if ( short[].class.equals( clazz ) ) {
            boolean isUnsignedByte =
                Boolean.TRUE
               .equals( info.getAuxDatumValue( Tables.UBYTE_FLAG_INFO,
                                               Boolean.class ) );
            return createArrayEncoder( isUnsignedByte ? "uint8" : "int16",
                                       info.getShape(), short[].class,
                                       delimiter,
                ( arr, i ) -> Short.toString( arr[ i ] )
            );
        }
        else if ( int[].class.equals( clazz ) ) {
            return createArrayEncoder( "int32", info.getShape(), int[].class,
                                       delimiter,
                ( arr, i ) -> Integer.toString( arr[ i ] )
            );
        }
        else if ( long[].class.equals( clazz ) ) {
            return createArrayEncoder( "int64", info.getShape(), long[].class,
                                       delimiter,
                ( arr, i ) -> Long.toString( arr[ i ] )
            );
        }
        else if ( float[].class.equals( clazz ) ) {
            return createArrayEncoder( "float32", info.getShape(),
                                       float[].class, delimiter, ( arr, i ) -> {
                
                float f = arr[ i ];
                return Float.isNaN( f ) || Float.isInfinite( f )
                     ? JSON_BLANK
                     : Float.toString( f );
            } );
        }
        else if ( double[].class.equals( clazz ) ) {
            return createArrayEncoder( "float64", info.getShape(),
                                       double[].class, delimiter, ( arr, i ) ->{
                double d = arr[ i ];
                return Double.isNaN( d ) || Double.isInfinite( d )
                     ? JSON_BLANK
                     : Double.toString( d );
            } );
        }
        else if ( String[].class.equals( clazz ) ) {
            return createArrayEncoder( "string", info.getShape(),
                                       String[].class, delimiter,
                                       ( arr, i ) -> toJsonString( arr[ i ] ) );
        }
        else {
            return null;
        }
    }

    /**
     * Returns a suitably escaped value of a string, ensuring that no
     * ECSV control characters are present in the result.
     *
     * @param  txt  raw string
     * @param  delimiter   delimiter character to be avoided,
     *                     must be comma or space
     * @return   sanitised version of the string
     */
    public static String quoteString( String txt, char delimiter ) {
        int nc = txt.length();
        if ( nc == 0 ) {
            return delimiter == ' ' ? "\"\"" : "";
        }
        boolean needsQuote = false;
        int ndq = 0;
        for ( int ic = 0; ic < nc; ic++ ) {
            char c = txt.charAt( ic );
            switch ( c ) {
                case ' ':
                case ',':
                    needsQuote |= ( c == delimiter );
                    break;
                case '"':
                    ndq++;
                    needsQuote = true;
                    break;
                case '\n':
                    needsQuote = true;
                    break;
                default:
            }
        }
        if ( needsQuote ) {
            int nqc = 2 + nc + ndq;
            StringBuilder sbuf = new StringBuilder( nqc );
            sbuf.append( '"' );
            for ( int ic = 0; ic < nc; ic++ ) {
                char c = txt.charAt( ic );
                sbuf.append( c );
                if ( c == '"' ) {
                    sbuf.append( c );
                }
            }
            sbuf.append( '"' );
            assert sbuf.length() == nqc;
            return sbuf.toString();
        }
        else {
            return txt;
        }
    }

    /**
     * Encodes a string value to its JSON representation.
     *
     * @param  txt  input string
     * @return   JSON serialization, including surrounding quote characters
     */
    private static String toJsonString( String txt ) {
        if ( txt == null ) {
            return JSON_BLANK;
        }
        else {
            int leng = txt.length();
            StringBuffer sbuf = new StringBuffer( leng + 2 );
            sbuf.append( '"' );
            for ( int i = 0; i < leng; i++ ) {
                char c = txt.charAt( i );
                switch ( c ) {
                    case '"':
                        sbuf.append( "\\\"" );
                        break;
                    case '\\':
                        sbuf.append( "\\\\" );
                        break;
                    case '\b':
                        sbuf.append( "\\b" );
                        break;
                    case '\f':
                        sbuf.append( "\\f" );
                        break;
                    case '\n':
                        sbuf.append( "\\n" );
                        break;
                    case '\r':
                        sbuf.append( "\\r" );
                        break;
                    case '\t':
                        sbuf.append( "\\t" );
                        break;
                    default:
                        if ( c <= 0x7f ) {
                            sbuf.append( c );
                        }
                        else {
                            sbuf.append( "\\u" );
                            String hex = Integer.toHexString( c );
                            for ( int j = hex.length(); j < 4; j++ ) {
                                sbuf.append( '0' );
                            }
                            sbuf.append( hex );
                        }
                }
            }
            sbuf.append( '"' );
            return sbuf.toString();
        }
    }

    /**
     * Returns an encoder for array-valued types.
     *
     * @param   elementType  ECSV name for element type
     * @param   stilShape  STIL-format shape array
     * @param   aclazz    array class for output type
     * @param   delimiter   ECSV delimiter character
     * @param   elEnc   function for encoding a single element to JSON format
     * @return   array encoder instance, or null if it can't be done
     */
    private static <T> EcsvEncoder
            createArrayEncoder( String elementType, int[] stilShape,
                                Class<T> aclazz, char delimiter,
                                final ElementEncoder<T> elEnc ) {
        final String dimsTxt;
        final int nel;
        final int ndim;
        if ( stilShape == null || stilShape.length == 0 ) {
            dimsTxt = JSON_BLANK;
            nel = -1;
            ndim = 1;
        }
        else {
            int nd = stilShape.length;
            boolean isFixed = true;
            int n = 1;
            for ( int i = 0; i < nd; i++ ) {
                int dim = stilShape[ i ];
                isFixed = isFixed && dim > 0;
                n *= Math.abs( dim );
            }
            if ( isFixed ) {
                nel = n;
                dimsTxt = IntStream
                         .range( 0, nd )
                         .map( i -> stilShape[ nd - 1 - i ] )
                         .mapToObj( Integer::toString )
                         .collect( Collectors.joining( "," ) );
                ndim = nd;
            }
            else {
                nel = -1;
                dimsTxt = JSON_BLANK;
                ndim = 1;
            }
        }
        String subtype = elementType + "[" + dimsTxt + "]";
        if ( ndim == 1 ) {
            return new EcsvEncoder( "string", subtype ) {
                public String encode( Object value ) {
                    if ( aclazz.isInstance( value ) ) {
                        T tval = aclazz.cast( value );
                        int tleng = Array.getLength( tval );
                        int leng = nel > 0 ? nel : tleng;
                        StringBuffer sbuf = new StringBuffer();
                        sbuf.append( '[' );
                        for ( int i = 0; i < leng; i++ ) {
                            if ( i > 0 ) {
                                sbuf.append( ',' );
                            }
                            sbuf.append( i < tleng
                                             ? elEnc.elementToJson( tval, i )
                                             : JSON_BLANK );
                        }
                        sbuf.append( ']' );
                        return quoteString( sbuf.toString(), delimiter );
                    }
                    else {
                        return null;
                    }
                }
            };
        }
        else {
            return new EcsvEncoder( "string", subtype ) {
                public String encode( Object value ) {
                    if ( aclazz.isInstance( value ) ) {
                        T tval = aclazz.cast( value );
                        if ( Array.getLength( tval ) == nel ) {
                            StringBuffer sbuf = new StringBuffer();
                            appendElements( sbuf, tval, 0, stilShape, elEnc );
                            return quoteString( sbuf.toString(), delimiter );
                        }
                        else {
                            return null;
                        }
                    }
                    else {
                        return null;
                    }
                }
            };
        }
    }

    /**
     * Recursive routine for printing out elements of a multidimensional
     * array in JSON-friendly format.
     *
     * @param  sbuf  string buffer accumulating result
     * @param  array   input full array value, values stored in one dimension
     * @param  pos    array index of next element to be appended
     * @param  dims   multidimensional shape of array
     * @param   elEnc   function for encoding a single element to JSON format
     * @return   updated array index of next element to be appended
     */
    private static <T> int appendElements( StringBuffer sbuf, T array,
                                           int pos, int[] dims,
                                           ElementEncoder<T> elEnc ) {
        sbuf.append( '[' );
        int ndim = dims.length;
        int limit = dims[ ndim - 1 ];
        if ( ndim == 1 ) {
            for ( int i = 0; i < limit; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ',' );
                }
                sbuf.append( elEnc.elementToJson( array, pos++ ) );
            }
        }
        else {
            int[] subdims = new int[ ndim - 1 ];
            System.arraycopy( dims, 0, subdims, 0, ndim - 1 );
            for ( int i = 0; i < limit; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ',' );
                }
                pos = appendElements( sbuf, array, pos, subdims, elEnc );
            }
        }
        sbuf.append( ']' );
        return pos;
    }

    /**
     * Defines how to write an array element.
     */
    @FunctionalInterface
    private static interface ElementEncoder<T> {

        /**
         * Serializes a single element of an array to a JSON scalar
         * representation.
         *
         * @param  array  input array
         * @param  index  position of element to output
         * @return  JSON representation of element
         */
        String elementToJson( T array, int index );
    }
}
