package uk.ac.starlink.ecsv;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Decodes values in the body of an ECSV file for a given data type.
 *
 * @author   Mark Taylor
 * @since    28 Apr 2020
 */
public abstract class EcsvDecoder<T> {

    private final Class<T> clazz_;
    private final String datatype_;
    private static final Map<String,EcsvDecoder<?>> DECODERS = createDecoders();

    /**
     * Constructor.
     *
     * @param   clazz  destination class
     * @param   datatype   datatype string declared in ECSV file
     */
    protected EcsvDecoder( Class<T> clazz, String datatype ) {
        clazz_ = clazz;
        datatype_ = datatype;
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
     * Returns the declared name for this decoder.
     *
     * @return   datatype name
     */
    public String getDatatype() {
        return datatype_;
    }

    /**
     * Returns an instance of this class given a datatype name.
     *
     * @param  txt  case-sensitive datatype name
     * @return   decoder for datatype, or null if unknown or unsupported
     */
    public static EcsvDecoder<?> forDatatype( String txt ) {
        return DECODERS.get( txt );
    }

    /**
     * Creates a name-&gt;decoder map for all supported datatypes.
     *
     * @return  map keyed by decoder name
     */
    private static final Map<String,EcsvDecoder<?>> createDecoders() {
        EcsvDecoder<?>[] decoders = new EcsvDecoder<?>[] {
            new EcsvDecoder<Boolean>( Boolean.class, "bool" ) {
                public Boolean decode( String txt ) {
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
                }
            },
            new EcsvDecoder<Byte>( Byte.class, "int8" ) {
                public Byte decode( String txt ) {
                    return Byte.parseByte( txt );
                }
            },
            new EcsvDecoder<Short>( Short.class, "int16" ) {
                public Short decode( String txt ) {
                    return Short.parseShort( txt );
                }
            },
            new EcsvDecoder<Integer>( Integer.class, "int32" ) {
                public Integer decode( String txt ) {
                    return Integer.parseInt( txt );
                }
            },
            new EcsvDecoder<Long>( Long.class, "int64" ) {
                public Long decode( String txt ) {
                    return Long.parseLong( txt );
                }
            },
            new EcsvDecoder<Float>( Float.class, "float32" ) {
                private final Float FNAN = Float.valueOf( Float.NaN );
                public Float decode( String txt ) {
                    if ( "nan".equals( txt ) ) {
                        return FNAN;
                    }
                    else {
                        return Float.parseFloat( txt );
                    }
                }
            },
            new EcsvDecoder<Double>( Double.class, "float64" ) {
                private final Double DNAN = Double.valueOf( Double.NaN );
                public Double decode( String txt ) {
                    if ( "nan".equals( txt ) ) {
                        return DNAN;
                    }
                    else {
                        return Double.parseDouble( txt );
                    }
                }
            },
            new EcsvDecoder<String>( String.class, "string" ) {
                public String decode( String txt ) {
                    return txt;
                }
            },
            new EcsvDecoder<Short>( Short.class, "uint8" ) {
                public Short decode( String txt ) {
                    return Short.parseShort( txt );
                }
            },
            new EcsvDecoder<Integer>( Integer.class, "uint16" ) {
                public Integer decode( String txt ) {
                    return Integer.parseInt( txt );
                }
            },
            new EcsvDecoder<Long>( Long.class, "uint32" ) {
                public Long decode( String txt ) {
                    return Long.parseLong( txt );
                }
            },
        };
        Map<String,EcsvDecoder<?>> map = new LinkedHashMap<>();
        for ( EcsvDecoder<?> decoder : decoders ) {
            map.put( decoder.getDatatype(), decoder );
        }
        return Collections.unmodifiableMap( map );
    }
}
