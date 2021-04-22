package uk.ac.starlink.ecsv;

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

    private String datatype_;

    /**
     * Constructor.
     *
     * @param  datatype  datatype name as declared by ECSV
     */
    protected EcsvEncoder( String datatype ) {
        datatype_ = datatype;
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
                        return "True";
                    }
                    else if ( Boolean.FALSE.equals( value ) ) {
                        return "False";
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
                            return "nan";
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
                        return "nan";
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
                            return "nan";
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
                        return "nan";
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
}
