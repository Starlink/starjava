package uk.ac.starlink.ecsv;

import java.util.function.Function;

/**
 * Decodes values in the body of an ECSV file for a given data type.
 *
 * @author   Mark Taylor
 * @since    28 Apr 2020
 */
public abstract class EcsvDecoder<T> {

    private final Class<T> clazz_;
    private static final Float FNAN = Float.valueOf( Float.NaN );
    private static final Double DNAN = Double.valueOf( Double.NaN );

    /**
     * Constructor.
     *
     * @param   clazz  destination class
     */
    protected EcsvDecoder( Class<T> clazz ) {
        clazz_ = clazz;
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
     * Returns an instance of this class given a datatype name.
     *
     * @param  datatype  case-sensitive datatype name
     * @return   decoder for datatype, or null if unknown or unsupported
     */
    public static EcsvDecoder<?> createDecoder( String datatype ) {
        if ( "int8".equals( datatype ) ) {
            return createDecoder( Byte.class, Byte::parseByte );
        }
        else if ( "int16".equals( datatype ) ||
                  "uint8".equals( datatype ) ) {
            return createDecoder( Short.class, Short::parseShort );
        }
        else if ( "int32".equals( datatype ) ||
                  "uint16".equals( datatype ) ) {
            return createDecoder( Integer.class, Integer::parseInt );
        }
        else if ( "int64".equals( datatype ) ||
                  "uint32".equals( datatype ) ) {
            return createDecoder( Long.class, Long::parseLong );
        }
        else if ( "float32".equals( datatype ) ) {
            return createDecoder( Float.class,
                txt -> "nan".equals( txt ) ? FNAN : Float.parseFloat( txt ) );
        }
        else if ( "float64".equals( datatype ) ) {
            return createDecoder( Double.class,
                txt -> "nan".equals( txt ) ? DNAN : Double.parseDouble( txt ) );
        }
        else if ( "bool".equals( datatype ) ) {
            return createDecoder( Boolean.class, txt -> {
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
            return createDecoder( String.class, Function.identity() );
        }
        else {
            return null;
        }
    }

    /**
     * Creates an EcsvDecoder instance given a string-&gt;type mapping function.
     *
     * @param  clazz  output class
     * @param  decode   function that decodes strings to typed values
     * @return   decoder
     */
    private static <T> EcsvDecoder<T>
            createDecoder( Class<T> clazz, final Function<String,T> decode ) {
        return new EcsvDecoder<T>( clazz ) {
            public T decode( String txt ) {
                return decode.apply( txt );
            }
        };
    }
}
