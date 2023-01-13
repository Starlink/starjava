package uk.ac.starlink.ecsv;

import java.lang.reflect.Array;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONString;
import org.json.JSONTokener;

/**
 * Decodes values in the body of an ECSV file for a given data type.
 *
 * @author   Mark Taylor
 * @since    28 Apr 2020
 */
public abstract class EcsvDecoder<T> {

    private final Class<T> clazz_;
    private final int[] stilShape_;
    private final String msg_;
    private static final int[] SCALAR_SHAPE = null;

    /**
     * Constructor.
     *
     * @param   clazz  destination class
     * @param   stilShape  shape specifier in STIL format
     * @param   msg   warning or diagnostic message concerning decoding,
     *                or null if everything is normal
     */
    protected EcsvDecoder( Class<T> clazz, int[] stilShape, String msg ) {
        clazz_ = clazz;
        stilShape_ = stilShape;
        msg_ = msg;
    }

    /**
     * Attempts to decode the supplied string to a value of this decoder's
     * type.  The supplied string will not be null or the empty string.
     * Implementations should work on the assumption that the supplied
     * string is of an appropriate type for this decoder; if it is not,
     * it is permissiable to throw a NumberFormatException.
     *
     * @param   txt   non-blank string to decode
     * @return   typed value
     * @throws   NumberFormatException in case of unrecognised string
     */
    public abstract T decode( String txt );

    /**
     * Returns the class to which input values will be decoded.
     *
     * @return   destination class
     */
    public Class<T> getContentClass() {
        return clazz_;
    }

    /**
     * Returns the shape of the object that is decoded,
     * in STIL format.
     * That may not be the same as what was specified in the ECSV metadata,
     * if STIL is not smart enough to make sense of it.
     *
     * @return  STIL shape array, or null for scalar
     */
    public int[] getShape() {
        return stilShape_ == null ? null : stilShape_.clone();
    }

    /**
     * Returns any warning message concerning the behaviour of this decoder.
     *
     * @return  warning message to be conveyed to user concerning this column,
     *          or null if everything is normal
     */
    public String getWarning() {
        return msg_;
    }

    /**
     * Returns an instance of this class given a datatype
     * and optional subtype.
     *
     * @param  datatype  case-sensitive datatype name
     * @param  subtype  subtype specifier string, or null
     * @return   decoder for datatype, or null if unknown or unsupported
     */
    public static EcsvDecoder<?> createDecoder( String datatype,
                                                String subtype ) {
        String igMsg = subtype == null
                     ? null
                     : "Ignoring unrecognised/unsupported subtype \""
                       + subtype + "\" - treating as " + datatype;
        if ( "int8".equals( datatype ) ) {
            return createDecoder( Byte.class, SCALAR_SHAPE, igMsg,
                                  Byte::valueOf,
                                  EcsvDecoder::resemblesInt );
        }
        else if ( "int16".equals( datatype ) ||
                  "uint8".equals( datatype ) ) {
            return createDecoder( Short.class, SCALAR_SHAPE, igMsg,
                                  Short::valueOf,
                                  EcsvDecoder::resemblesInt );
        }
        else if ( "int32".equals( datatype ) ||
                  "uint16".equals( datatype ) ) {
            return createDecoder( Integer.class, SCALAR_SHAPE, igMsg,
                                  Integer::valueOf,
                                  EcsvDecoder::resemblesInt );
        }
        else if ( "int64".equals( datatype ) ||
                  "uint32".equals( datatype ) ) {
            return createDecoder( Long.class, SCALAR_SHAPE, igMsg,
                                  Long::valueOf,
                                  EcsvDecoder::resemblesInt );
        }
        else if ( "float32".equals( datatype ) ) {
            return createDecoder( Float.class, SCALAR_SHAPE, igMsg,
                                  EcsvDecoder::parseFloat );
        }
        else if ( "float64".equals( datatype ) ) {
            return createDecoder( Double.class, SCALAR_SHAPE, igMsg,
                                  EcsvDecoder::parseDouble );
        }
        else if ( "bool".equals( datatype ) ) {
            return createDecoder( Boolean.class, SCALAR_SHAPE, igMsg, txt -> {
                if ( "True".equals( txt ) ) {
                    return Boolean.TRUE;
                }
                else if ( "False".equals( txt ) ) {
                    return Boolean.FALSE;
                }
                else if ( txt.equalsIgnoreCase( "true" ) ||
                          txt.equalsIgnoreCase( "T" ) ) {
                    return Boolean.TRUE; 
                }
                else if ( txt.equalsIgnoreCase( "false" ) ||
                          txt.equalsIgnoreCase( "F" ) ) {
                    return Boolean.FALSE;
                }
                else {
                    return null;
                }
            } );
        }
        else if ( "string".equals( datatype ) ) {
            ArrayType arraytype = parseArraytype( subtype );
            if ( arraytype != null ) {
                String eltype = arraytype.elementType_;
                int[] stilShape = arraytype.stilShape_;
                if ( stilShape == null ) {
                    String msg = "Can't cope with variable-length "
                               + "higher dimensions (" + subtype
                               + ") - treat as " + datatype;
                    return createDecoder( String.class, SCALAR_SHAPE, msg,
                                          Function.identity() );
                }
                else if ( "int8".equals( eltype ) ) {
                    return createArrayDecoder( byte[].class, stilShape,
                                               ( arr, i, jobj ) -> {
                        arr[ i ] = jobj instanceof Number
                                 ? ((Number) jobj).byteValue()
                                 : 0;
                    } );
                }
                else if ( "int16".equals( eltype ) ||
                          "uint8".equals( eltype ) ) {
                    return createArrayDecoder( short[].class, stilShape,
                                               ( arr, i, jobj ) -> {
                        arr[ i ] = jobj instanceof Number
                                 ? ((Number) jobj).shortValue()
                                 : 0;
                    } );
                }
                else if ( "int32".equals( eltype ) ||
                          "uint16".equals( eltype ) ) {
                    return createArrayDecoder( int[].class, stilShape,
                                               ( arr, i, jobj ) -> {
                        arr[ i ] = jobj instanceof Number
                                 ? ((Number) jobj).intValue()
                                 : 0;
                    } );
                }
                else if ( "int64".equals( eltype ) ||
                          "uint32".equals( eltype ) ) {
                    return createArrayDecoder( long[].class, stilShape,
                                               ( arr, i, jobj ) -> {
                        arr[ i ] = jobj instanceof Number
                                 ? ((Number) jobj).longValue()
                                 : 0;
                    } );
                }
                else if ( "float32".equals( eltype ) ) {
                    return createArrayDecoder( float[].class, stilShape,
                                               ( arr, i, jobj ) -> {
                        arr[ i ] = jobj instanceof Number
                                 ? ((Number) jobj).floatValue()
                                 : Float.NaN;
                    } );
                }
                else if ( "float64".equals( eltype ) ) {
                    return createArrayDecoder( double[].class, stilShape,
                                               ( arr, i, jobj ) -> {
                        arr[ i ] = jobj instanceof Number
                                 ? ((Number) jobj).doubleValue()
                                 : Double.NaN;
                    } );
                }
                else if ( "bool".equals( eltype ) ) {
                    return createArrayDecoder( boolean[].class, stilShape,
                                               ( arr, i, jobj ) -> {
                        arr[ i ] = jobj instanceof Boolean
                                 ? ((Boolean) jobj).booleanValue()
                                 : false;
                    } );
                }
                else if ( "string".equals( eltype ) ) {
                    return createArrayDecoder( String[].class, stilShape,
                                               ( arr, i, jobj ) -> {

                        /* Could make different tests here, but *don't* just
                         * check it's not null: jobj may have the value
                         * JSONObject.NULL. */
                        arr[ i ] = ( jobj instanceof JSONString ||
                                     jobj instanceof String )
                                 ? jobj.toString()
                                 : null;
                    } );
                }
                else {
                    return createDecoder( String.class, SCALAR_SHAPE, igMsg,
                                          Function.identity() );
                }
            }
            else {
                return createDecoder( String.class, SCALAR_SHAPE, igMsg,
                                      Function.identity() );
            }
        }
        else if ( "float128".equals( datatype ) ||
                  "complex64".equals( datatype ) ||
                  "complex128".equals( datatype ) ||
                  "complex256".equals( datatype ) ) {
            return createDecoder( String.class, SCALAR_SHAPE,
                                  "Unsupported ECSV type \"" + datatype
                                + "\" - treating as string",
                                  Function.identity() );
        }
        else {
            return createDecoder( String.class, SCALAR_SHAPE,
                                  "Unknown ECSV type \"" + datatype
                                + "\" - treating as string",
                                  Function.identity() );
        }
    }

    /**
     * Parses a subtype string as an array type, of the form
     * <code>elementType[dim,dim,...]</code>.
     * If it's not of that form, null is returned.
     * If the dimensions can't be represented in a STIL-friendly way
     * (variable-length array in higher dimensions),
     * the shape is null.
     *
     * @param  subtype  ESCV string subtype string
     * @return   arraytype defined by subtype, or null if not array format
     */
    private static ArrayType parseArraytype( String subtype ) {
        if ( subtype == null || subtype.trim().length() == 0 ) {
            return null;
        }
        Matcher stMatcher =
            Pattern.compile( "\\s*([a-zA-Z0-9]+)\\s*\\[(.*)\\]\\s*" )
                   .matcher( subtype );
        if ( stMatcher.matches() ) {
            String elType = stMatcher.group( 1 );
            String dimsTxt = stMatcher.group( 2 );
            String[] dimTxts = dimsTxt.trim().split( "\\s*,\\s*", -1 );
            int ndim = dimTxts.length;
            int[] dims = new int[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                String dimTxt = dimTxts[ i ];
                final int dim;
                if ( "null".equals( dimTxt ) ) {
                    dim = -1;
                }
                else if ( dimTxt.matches( "[1-9][0-9]*" ) ) {
                    dim = Integer.parseInt( dimTxt );
                }
                else {
                    return null;
                }
                // JSON array indices are the other way round than STIL
                dims[ ndim - 1 - i ] = dim;
            }

            /* The problem is that arrays have their indices in the opposite
             * order between STIL and ECSV/JSON, so a final indeterminate
             * array size in ECSV would give a non-rectangular array in STIL. */
            boolean isLegalStil = true;
            for ( int idim = 0; idim < dims.length - 1; idim++ ) {
                if ( dims[ idim ] < 0 ) {
                    isLegalStil = false;
                }
            }
            return new ArrayType( elType, isLegalStil ? dims : null );
        }
        else {
            return null;
        }
    }

    /**
     * Creates an EcsvDecoder instance given a string-&gt;type mapping function.
     *
     * @param  clazz  output class
     * @param  stilShape   array shape specification in STIL format
     * @param   msg   warning or diagnostic message concerning decoding,
     *                or null if everything is normal
     * @param  decode   function that decodes strings to typed values
     * @return   decoder
     */
    private static <T> EcsvDecoder<T>
            createDecoder( Class<T> clazz, int[] stilShape, String msg,
                           Function<String,T> decode ) {
        return new EcsvDecoder<T>( clazz, stilShape, msg ) {
            public T decode( String txt ) {
                return decode.apply( txt );
            }
        };
    }

    /**
     * Creates an EcsvDecoder instance given a string-&gt;type mapping function
     * and a filter predicate to test if values look OK first.
     *
     * <p>The purpose of the <code>isPlausible</code> argument is to perform
     * a fast test of the string to see if looks like it's probably OK.
     * If it's clearly not suitable for parsing, the decoder will return
     * null rather than attempting the parse and throwing a
     * NumberFormatException, which is relatively expensive.
     *
     * @param  clazz  output class
     * @param  stilShape   array shape specification in STIL format
     * @param   msg   warning or diagnostic message concerning decoding,
     *                or null if everything is normal
     * @param  decode   function that decodes strings to typed values
     * @param  isPlausible  fast test which returns false if argument
     *                      should definitely be interpreted as null
     * @return   decoder
     */
    private static <T> EcsvDecoder<T>
            createDecoder( Class<T> clazz, int[] stilShape, String msg,
                           Function<String,T> decode,
                           Predicate<String> isPlausible ) {
        return new EcsvDecoder<T>( clazz, stilShape, msg ) {
            public T decode( String txt ) {
                return isPlausible.test( txt )
                     ? decode.apply( txt )
                     : null;
            }
        };
    }

    /**
     * Fast check whether a string looks like it could be a representation of
     * an integer-like number.  Not bulletproof.
     *
     * @param  txt  string to test
     * @return  false if it's definitely not an integer
     */
    private static boolean resemblesInt( String txt ) {
        int len = txt.length();
        if ( len == 0 ) {
            return false;
        }
        for ( int i = 0; i < len; i++ ) {
            switch ( txt.charAt( i ) ) {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                case '+': case '-':
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    /**
     * Fast check whether a string looks like it could be a representation of
     * a floating-point number.  Not bulletproof.
     *
     * @param  txt  string to text
     * @return  false if it's definitely not a normal floating point number
     */
    private static boolean resemblesFloat( String txt ) {
        int len = txt.length();
        if ( len == 0 ) {
            return false;
        }
        for ( int i = 0; i < len; i++ ) {
            switch ( txt.charAt( i ) ) {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                case '+': case '-':
                case '.': case 'e': case 'E':
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    /**
     * Parses text to a Float value.
     *
     * @param  txt  input text
     * @return   float value, or null
     */
    private static Float parseFloat( String txt ) {
        if ( "inf".equals( txt ) ) {
            return Float.POSITIVE_INFINITY;
        }
        if ( "-inf".equals( txt ) ) {
            return Float.NEGATIVE_INFINITY;
        }
        return resemblesFloat( txt ) ? Float.valueOf( txt ) : null;
    }

    /**
     * Parses text to a Double value.
     *
     * @param  txt  input text
     * @return   double value, or null
     */
    private static Double parseDouble( String txt ) {
        if ( "inf".equals( txt ) ) {
            return Double.POSITIVE_INFINITY;
        }
        if ( "-inf".equals( txt ) ) {
            return Double.NEGATIVE_INFINITY;
        }
        return resemblesFloat( txt ) ? Double.valueOf( txt ) : null;
    }

    /**
     * Creates an EcsvDecoder instance for content serialized as a JSON array.
     *
     * @param  aclazz  output array class
     * @param  stilShape   shape specifier in STIL format
     * @param  storage   can store JSON values in a typed array
     */
    private static <T> EcsvDecoder<?>
            createArrayDecoder( Class<T> aclazz, int[] stilShape,
                                ElementStore<T> storage ) {
        final int ndim = stilShape.length;
        int nel = 1;
        boolean isFixed = true;
        for ( int idim = 0; idim < ndim; idim++ ) {
            int dim = stilShape[ idim ];
            nel *= Math.abs( dim );
            isFixed = isFixed && dim > 0;
        }
        final boolean isFixed0 = isFixed;
        final int nel0 = nel;
        Class<?> compType = aclazz.getComponentType();
        String msg = null;
        if ( ndim == 1 ) {
            return createDecoder( aclazz, stilShape, msg, txt -> {
                if ( txt == null || txt.trim().length() == 0 ) {
                    return null;
                }
                Object jvalue = new JSONTokener( txt ).nextValue();
                if ( jvalue instanceof JSONArray ) {
                    JSONArray jarray = (JSONArray) jvalue;
                    int jleng = jarray.length();
                    int aleng;
                    int count;
                    if ( isFixed0 ) {
                        aleng = nel0;
                        count = Math.min( aleng, jleng );
                    }
                    else {
                        aleng = jleng;
                        count = aleng;
                    }
                    final T array =
                        aclazz.cast( Array.newInstance( compType, aleng ) );
                    for ( int i = 0; i < count; i++ ) {
                        storage.writeElement( array, i, jarray.get( i ) );
                    }
                    return array;
                }
                else {
                    return null;
                }
            } );
        }
        else if ( isFixed ) {
            return createDecoder( aclazz, stilShape, msg, txt -> {
                if ( txt == null || txt.trim().length() == 0 ) {
                    return null;
                }
                Object jvalue = new JSONTokener( txt ).nextValue();
                if ( jvalue instanceof JSONArray ) {
                    JSONArray jarray = (JSONArray) jvalue;
                    final T array =
                        aclazz.cast( Array.newInstance( compType, nel0 ) );
                    writeArray( array, storage, jarray, new int[ 1 ] );
                    return array;
                }
                else {
                    return null;
                }
            } );
        }
        else {
            assert false;  // not legal STIL, shouldn't have got here
            return null;
        }
    }

    /**
     * Recursively copies content from an N-dimensional JSON array to
     * a given array object.
     *
     * @param  array   destination array
     * @param  storage   can store JSON scalars into array elements
     * @param  jarray   JSON array with content; if it's an array of arrays,
     *                  recursion will occur
     * @param  ipos    mutable pointer to position in output array
     */
    private static <T> void writeArray( T array, ElementStore<T> storage,
                                        JSONArray jarray, int[] ipos ) {
        int nel = jarray.length();
        Object jobj0 = jarray.get( 0 );
        if ( jobj0 instanceof JSONArray ) {
            int[] ipos1 = ipos.clone();
            for ( int i = 0; i < nel; i++ ) {
                writeArray( array, storage, (JSONArray) jarray.get( i ), ipos );
            }
        }
        else {
            for ( int i = 0; i < nel; i++ ) {
                storage.writeElement( array, ipos[ 0 ]++, jarray.get( i ) );
            }
        }
    }

    /**
     * Defines a function copying a JSON object into a typed array.
     */
    @FunctionalInterface
    private static interface ElementStore<T> {

        /**
         * Writes the value of an object obtained from JSON parsing
         * into a given typed array.
         *
         * @param  array  destination array
         * @param  index  index at which value should be inserted in array
         * @param  jsonEl  object retrieved from JSON parsing
         */
        void writeElement( T array, int index, Object jsonEl );
    }

    /**
     * Characterises a string subtype representing a typed array value.
     */
    private static class ArrayType {
        final String elementType_;
        final int[] stilShape_;

        /**
         * Constructor.
         *
         * @param  elementType  type name for the type of array elements
         * @param  stilShape  array dimensions in STIL format,
         *                    or null if can't be represented in STIL
         */
        ArrayType( String elementType, int[] stilShape ) {
            elementType_ = elementType;
            stilShape_ = stilShape == null ? null : stilShape.clone();
        }
    }
}
