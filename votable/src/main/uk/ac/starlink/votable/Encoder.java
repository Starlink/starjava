package uk.ac.starlink.votable;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * Handles writing an Object to a VOTable element.  Obtain a concrete
 * instance of one of this abstract class's subclasses using the static
 * {@link #getEncoder} method.  Encoders deal with serializing data
 * objects (e.g. objects which live in the cell of a StarTable) to 
 * a 'native' VOTable serialization format, that is TABLEDATA or BINARY.
 *
 * @author   Mark Taylor (Starlink)
 */
abstract class Encoder {

    private final ValueInfo info_;
    private final Map<String,String> attMap_;
    private final String description_;
    private final String links_;
    private String nullString_;
    private String content_;

    private final static Logger logger =
        Logger.getLogger( "uk.ac.starlink.votable" );

    /**
     * Returns a text string which represents a given value of the type
     * encoded by this object.  The text is suitable for use as the
     * content of a VOTable CDATA element, except that no escaping of
     * XML special characters has been done.
     *
     * @param  value  an object of the type handled by this encoder
     * @return  text representing the value suitable for inclusion in
     *          an XML attribute value or CDATA element
     */
    abstract public String encodeAsText( Object value );

    /**
     * Writes a given value of the type handled by this encoder out to
     * a stream in BINARY serialization format.  Any required element
     * count is included.
     *
     * @param  value  the value to write (of the type handled by this encoder)
     * @param  out  destination stream
     */
    abstract public void encodeToStream( Object value, DataOutput out )
            throws IOException;

    /**
     * Constructs a new encoder which can serialize cell data of a 
     * type described by a given ValueInfo object.
     *
     * @param  info  description of the data this encoder will have 
     *               to serialize
     * @param  datatype  value of the datatype attribute
     */
    private Encoder( ValueInfo info, String datatype ) {
        info_ = info;
        attMap_ = new LinkedHashMap<String,String>();

        /* Datatype attribute. */
        putAtt( "datatype", datatype.trim() );

        /* Name attribute. */
        String name = info.getName();
        if ( name != null && name.trim().length() > 0 ) {
            putAtt( "name", name.trim() );
        }

        /* Unit attribute. */
        String units = info.getUnitString();
        if ( units != null && units.trim().length() > 0 ) {
            putAtt( "unit", units.trim() );
        }

        /* UCD attribute. */
        String ucd = info.getUCD();
        if ( ucd != null && ucd.trim().length() > 0 ) {
            putAtt( "ucd", ucd.trim() );
        }

        /* Utype attribute. */
        String utype = info.getUtype();
        if ( utype != null && utype.trim().length() > 0 ) {
            putAtt( "utype", utype.trim() );
        }

        /* XType attribute. */
        String xtype = info.getXtype();
        if ( xtype != null && xtype.trim().length() > 0 ) {
            putAtt( "xtype", xtype.trim() );
        }

        /* ID attribute. */
        String id =
            Tables.getAuxDatumValue( info, VOStarTable.ID_INFO, String.class );
        if ( id != null && id.trim().length() > 0 ) {
            putAtt( "ID", id.trim() );
        }

        /* Ref attribute. */
        String ref =
            Tables.getAuxDatumValue( info, VOStarTable.REF_INFO, String.class );
        if ( ref != null && ref.trim().length() > 0 ) {
            putAtt( "ref", ref.trim() );
        }

        /* Width attribute. */
        Integer width =
            Tables.getAuxDatumValue( info, VOStarTable.WIDTH_INFO,
                                     Integer.class );
        if ( width != null && width.intValue() > 0 ) {
            putAtt( "width", width.toString() );
        }

        /* Precision attribute. */
        String precision =
            Tables.getAuxDatumValue( info, VOStarTable.PRECISION_INFO,
                                     String.class );
        if ( precision != null && precision.trim().length() > 0 ) {
            putAtt( "precision", precision.trim() );
        }

        /* Description information. */
        String desc = info.getDescription();
        desc = desc == null ? null : desc.trim();
        description_ = desc == null || desc.length() == 0
                     ? null
                     : "<DESCRIPTION>"
                     + VOSerializer.formatText( desc )
                     + "</DESCRIPTION>";

        /* URL-type auxiliary metadata can be encoded as LINK elements. */
        StringBuffer linksBuf = new StringBuffer();
        for ( DescribedValue dval : info.getAuxData() ) {
            ValueInfo linkInfo = dval.getInfo();
            if ( URL.class.equals( linkInfo.getContentClass() ) ) {
                String linkName = linkInfo.getName();
                URL linkUrl = dval.getTypedValue( URL.class );
                if ( linkUrl != null ) {
                    if ( linksBuf.length() > 0 ) {
                        linksBuf.append( '\n' );
                    }
                    linksBuf.append( "<LINK" );
                    if ( linkName != null ) {
                        linksBuf.append( VOSerializer
                                .formatAttribute( "title", linkName ) );
                    }
                    linksBuf.append( VOSerializer
                                    .formatAttribute( "href", 
                                                      linkUrl.toString() ) );
                    linksBuf.append( "/>" );
                }
            }
        }
        links_ = linksBuf.toString();
    }

    /**
     * Returns any text which should go inside the FIELD (or PARAM) element
     * corresponding to this Encoder.  Such text may contain XML markup,
     * so should not be further escaped.
     *
     * @return  a string containing XML text for inside the FIELD element -
     *          may be empty but will not be <tt>null</tt>
     */
    public String getFieldContent() {
        if ( content_ == null ) {
            StringBuffer contBuf = new StringBuffer();
            if ( description_ != null && description_.trim().length() > 0 ) {
                contBuf.append( '\n' )
                       .append( description_ );
            }
            if ( nullString_ != null && nullString_.trim().length() > 0 ) {
                contBuf.append( '\n' )
                       .append( "<VALUES null='" + nullString_ + "'/>" );
            }
            if ( links_ != null && links_.trim().length() > 0 ) {
                contBuf.append( '\n' )
                       .append( links_ );
            }
            content_ = contBuf.toString();
        }
        return content_;
    }

    /**
     * Returns a map of key, value pairs representing a set of XML
     * attributes describing the objects encoded by this object.
     * The attributes can be used to qualify a FIELD or PARAM
     * element in a VOTable document.
     *
     * @return  a map of attribute name, attribute value pairs applying
     *          to this encoder
     */
    public Map<String,String> getFieldAttributes() {
        return attMap_;
    }

    /**
     * Sets the null representation for this encoder.
     *
     * @param   nullString  null representation
     */
    public void setNullString( String nullString ) {
        nullString_ = nullString;
    }

    /**
     * Returns the value metadata which this encoder is serializing.
     *
     * @return  info  description of the data this encoder will have 
     *               to serialize
     */
    public ValueInfo getInfo() {
        return info_;
    }

    /**
     * Sets an attribute value.
     *
     * @param key  attribute name
     * @param  value  attribute value
     */
    void putAtt( String key, String value ) {
        attMap_.put( key, value );
    }

    /**
     * Returns an Encoder suitable for encoding values described by a
     * given ValueInfo object.
     * If <tt>info</tt> is a ColumnInfo, then the preferred binary 
     * representation of bad values can be submitted in its auxiliary
     * metadata under the key {@link Tables#NULL_VALUE_INFO}.
     * Byte values will normally be serialised as short ints
     * (since the java byte type is signed but the VOTable one is unsigned),
     * but unsigned byte output can be forced by setting the
     * {@link Tables#UBYTE_FLAG_INFO} aux metadata item to true.
     *
     * @param   info  a description of the type of value which needs to
     *          be encoded
     * @param   magicNulls  if true, the returned encoder may attempt to use
     *          a magic value to signify null values; if false, it never will
     * @param   useUnicodeChar  if true, character-type columns will be output
     *          with datatype unicodeChar, else with datatype char
     * @return  an encoder object which can do it
     */
    public static Encoder getEncoder( ValueInfo info, boolean magicNulls,
                                      boolean useUnicodeChar ) {

        final CharWriter cwrite = useUnicodeChar ? CharWriter.UCS2
                                                 : CharWriter.ASCII;
        Class<?> clazz = info.getContentClass();
        int[] dims = info.getShape();
        final boolean isNullable = info.isNullable() && magicNulls;
        final boolean isVariable = dims != null 
                                && dims.length > 0 
                                && dims[ dims.length - 1 ] < 0;

        /* See if unsigned byte output is explicitly requested. */
        boolean isUbyte = false;
        if ( Boolean.TRUE
            .equals( Tables.getAuxDatumValue( info, Tables.UBYTE_FLAG_INFO,
                                              Boolean.class ) ) ) {
            if ( clazz == short[].class || clazz == Short.class ) {
                isUbyte = true;
            }
            else {
                logger.warning( "Ignoring " + Tables.UBYTE_FLAG_INFO
                              + " on non-short column/param " + info );
            }
        }

        /* Try to work out a representation to use for blank integer values. */
        Number nullObj = null;
        DescribedValue nullValue =
        info.getAuxDatumByName( Tables.NULL_VALUE_INFO.getName() );
        if ( nullValue != null ) {
            Object o = nullValue.getValue();
            if ( o instanceof Number ) {
                nullObj = (Number) o;
            }
        }

        if ( clazz == Boolean.class ) {
            return new ScalarEncoder( info, "boolean", null ) {
                public String encodeAsText( Object val ) {
                    Boolean value = (Boolean) val;
                    return value == null 
                               ? "" 
                               : ( value.booleanValue() ? "T" : "F" );
                }
                public void encodeToStream( Object val, DataOutput out )
                        throws IOException {
                    Boolean value = (Boolean) val;
                    char b = value == null
                                   ? ' '
                                   : ( value.booleanValue() ? 'T' : 'F' );
                    out.write( (int) b );
                }
            };
        }

        else if ( isUbyte && clazz == Short.class ) {
            final int badVal = nullObj == null
                              ? ( magicNulls ? 255 : 0 )
                              : nullObj.intValue();
            String badString = isNullable ? Integer.toString( badVal ) : null;
            return new ScalarEncoder( info, "unsignedByte", badString ) {
                public void encodeToStream( Object val, DataOutput out )
                        throws IOException {
                    Number value = (Number) val;
                    out.writeByte( value == null ? badVal
                                                 : value.intValue() );
                }
            };
        }

        else if ( clazz == Byte.class ||
                  clazz == Short.class ) {
            final int badVal = nullObj == null
                             ? ( magicNulls ? Short.MIN_VALUE : (short) 0 )
                             : nullObj.intValue();
            String badString = isNullable ? Integer.toString( badVal ) : null;
            return new ScalarEncoder( info, "short", badString ) {
                public void encodeToStream( Object val, DataOutput out ) 
                        throws IOException {
                    Number value = (Number) val;
                    out.writeShort( value == null ? badVal
                                                  : value.intValue() );
                }
            };
        }

        else if ( clazz == Integer.class ) {
            final int badVal = nullObj == null
                             ? ( magicNulls ? Integer.MIN_VALUE : 0 )
                             : nullObj.intValue();
            String badString = isNullable ? Integer.toString( badVal ) 
                                          : null;
            return new ScalarEncoder( info, "int", badString ) {
                public void encodeToStream( Object val, DataOutput out )
                        throws IOException {
                    Number value = (Number) val;
                    out.writeInt( value == null ? badVal 
                                                : value.intValue() );
                }
            };
        }

        else if ( clazz == Long.class ) {
            final long badVal = nullObj == null
                              ? ( magicNulls ? Long.MIN_VALUE : 0L )
                              : nullObj.longValue();
            String badString = isNullable ? Long.toString( badVal ) : null;
            return new ScalarEncoder( info, "long", badString ) {
                public void encodeToStream( Object val, DataOutput out )
                        throws IOException {
                    Number value = (Number) val;
                    out.writeLong( value == null ? badVal
                                                 : value.longValue() );
                }
            };
        }

        else if ( clazz == Float.class ) {
            return new ScalarEncoder( info, "float", null ) {
                public String encodeAsText( Object val ) {
                    if ( val instanceof Float &&
                         Float.isInfinite( ((Float) val).floatValue() ) ) {
                        return infinityText( ((Float) val).floatValue() > 0 );
                    }
                    else {
                        return super.encodeAsText( val );
                    }
                }
                public void encodeToStream( Object val, DataOutput out )
                        throws IOException {
                    Number value = (Number) val;
                    out.writeFloat( value == null ? Float.NaN
                                                  : value.floatValue() );
                }
            };
        }

        else if ( clazz == Double.class ) {
            return new ScalarEncoder( info, "double", null ) {
                public String encodeAsText( Object val ) {
                    if ( val instanceof Double &&
                         Double.isInfinite( ((Double) val).doubleValue() ) ) {
                        return infinityText( ((Double) val).doubleValue() > 0 );
                    }
                    else {
                        return super.encodeAsText( val );
                    }
                }
                public void encodeToStream( Object val, DataOutput out )
                        throws IOException {
                    Number value = (Number) val;
                    out.writeDouble( value == null ? Double.NaN
                                                   : value.doubleValue() );
                }
            };
        }

        else if ( clazz == Character.class ) {
            final char badVal = '\0';
            return new ScalarEncoder( info, cwrite.getDatatype(), null ) {
                /* For a single character take care to add the attribute
                 * arraysize="1" - although this is implicit according to
                 * the standard, it's often left off and assumed to be
                 * equivalent to arraysize="*".  This makes sure there is
                 * no ambiguity. */
                /*anonymousConstructor*/ {
                    putAtt( "arraysize", "1" );
                }
                public void encodeToStream( Object val, DataOutput out )
                        throws IOException {
                    Character value = (Character) val;
                    cwrite.writeChar( out, value == null ? badVal
                                                         : value.charValue() );
                }
            };
        }

        if ( clazz == boolean[].class ) {
            Encoder1 enc1 = new Encoder1() {
                public void encode1( Object array, int index, DataOutput out )
                        throws IOException {
                    boolean value = ((boolean[]) array)[ index ];
                    out.write( value ? 'T' : 'F' );
                }
                public void pad1( DataOutput out ) throws IOException {
                    out.write( ' ' );
                }
            };
            return isVariable
                 ? (Encoder) new VariableArrayEncoder( info, "boolean",
                                                       dims, enc1 )
                 : (Encoder) new FixedArrayEncoder( info, "boolean",
                                                    dims, enc1 );
        }

        else if ( isUbyte && clazz == short[].class ) {
            Encoder1 enc1 = new Encoder1() {
                public void encode1( Object array, int index, DataOutput out )
                        throws IOException {
                    int value = ((short[]) array)[ index ];
                    out.writeByte( value );
                }
                public void pad1( DataOutput out ) throws IOException {
                    out.writeByte( 0x0 );
                }
            };
            return isVariable
                 ? (Encoder) new VariableArrayEncoder( info, "unsignedByte",
                                                       dims, enc1 )
                 : (Encoder) new FixedArrayEncoder( info, "unsignedByte",
                                                    dims, enc1 );
        }

        else if ( clazz == byte[].class ) {
            Encoder1 enc1 = new Encoder1() {
                public void encode1( Object array, int index, DataOutput out )
                        throws IOException {
                    byte value = ((byte[]) array)[ index ];
                    out.writeShort( value );
                }
                public void pad1( DataOutput out ) throws IOException {
                    out.writeShort( 0x0 );
                }
            };
            return isVariable
                ? (Encoder) new VariableArrayEncoder( info, "short",
                                                      dims, enc1 )
                : (Encoder) new FixedArrayEncoder( info, "short",
                                                   dims, enc1 );
        }

        else if ( clazz == short[].class ) {
            Encoder1 enc1 = new Encoder1() {
                public void encode1( Object array, int index, DataOutput out )
                        throws IOException {
                    short value = ((short[]) array)[ index ];
                    out.writeShort( value );
                }
                public void pad1( DataOutput out ) throws IOException {
                    out.writeShort( 0x0 );
                }
            };
            return isVariable
                ? (Encoder) new VariableArrayEncoder( info, "short",
                                                      dims, enc1 )
                : (Encoder) new FixedArrayEncoder( info, "short",
                                                   dims, enc1 );
        }

        else if ( clazz == int[].class ) {
            Encoder1 enc1 = new Encoder1() {
                public void encode1( Object array, int index, DataOutput out )
                        throws IOException {
                    int value = ((int[]) array)[ index ];
                    out.writeInt( value );
                }
                public void pad1( DataOutput out ) throws IOException {
                    out.writeInt( 0 );
                }         
            };
            return isVariable
                ? (Encoder) new VariableArrayEncoder( info, "int", dims, enc1 )
                : (Encoder) new FixedArrayEncoder( info, "int", dims, enc1 );
        }

        else if ( clazz == long[].class ) {
            Encoder1 enc1 = new Encoder1() {
                public void encode1( Object array, int index, DataOutput out )
                        throws IOException {
                    long value = ((long[]) array)[ index ];
                    out.writeLong( value );
                }
                public void pad1( DataOutput out ) throws IOException {
                    out.writeLong( 0L );
                }
            };
            return isVariable 
                ? (Encoder) new VariableArrayEncoder( info, "long", dims, enc1 )
                : (Encoder) new FixedArrayEncoder( info, "long", dims, enc1 );
        }

        else if ( clazz == float[].class ) {
            Encoder1 enc1 = new Encoder1() {
                public void encode1( Object array, int index, DataOutput out )
                        throws IOException {
                    float value = ((float[]) array)[ index ];
                    out.writeFloat( value );
                }
                public void pad1( DataOutput out ) throws IOException {
                    out.writeFloat( Float.NaN );
                }
            };
            return isVariable
                ? (Encoder) new VariableArrayEncoder( info, "float",
                                                      dims, enc1 )
                : (Encoder) new FixedArrayEncoder( info, "float",
                                                   dims, enc1 );
        }

        else if ( clazz == double[].class ) {
            Encoder1 enc1 = new Encoder1() {
                public void encode1( Object array, int index, DataOutput out )
                        throws IOException {
                    double value = ((double[]) array)[ index ];
                    out.writeDouble( value );
                }
                public void pad1( DataOutput out ) throws IOException {
                    out.writeDouble( Double.NaN );
                }
            };
            return isVariable
                ? (Encoder) new VariableArrayEncoder( info, "double",
                                                      dims, enc1 )
                : (Encoder) new FixedArrayEncoder( info, "double",
                                                   dims, enc1 );
        }

        else if ( clazz == String.class ) {
            final int nChar = clazz == String.class ? info.getElementSize()
                                                    : -1;

            /* Fixed length strings. */
            if ( nChar > 0 ) {
                return new Encoder( info, cwrite.getDatatype() ) {
                    /*anonymousConstructor*/ {
                        putAtt( "arraysize", Integer.toString( nChar ) );
                    }

                    public String encodeAsText( Object val ) {
                        return val == null ? "" : val.toString();
                    }

                    public void encodeToStream( Object val, DataOutput out )
                            throws IOException {
                        int i = 0;
                        if ( val != null ) {
                            String value = val.toString();
                            int limit = Math.min( value.length(), nChar );
                            for ( ; i < limit; i++ ) {
                                cwrite.writeChar( out, value.charAt( i ) );
                            }
                        }
                        for ( ; i < nChar; i++ ) {
                            cwrite.writeChar( out, '\0' );
                        }
                    }
                };
            }

            /* Variable length strings. */
            else {
                return new Encoder( info, cwrite.getDatatype() ) {
                    /*anonymousConstructor*/ {
                        putAtt( "arraysize", "*" );
                    }

                    public String encodeAsText( Object val ) {
                        return val == null ? "" : val.toString();
                    }

                    public void encodeToStream( Object val, DataOutput out )
                            throws IOException {
                        if ( val == null ) {
                            out.writeInt( 0 );
                        }
                        else {
                            String value = val.toString();
                            int leng = value.length();
                            out.writeInt( leng );
                            for ( int i = 0 ; i < leng; i++ ) {
                                cwrite.writeChar( out, value.charAt( i ) );
                            }
                        }
                    }
                };
            }
        }

        else if ( clazz == String[].class ) {
            final int nChar = clazz == String[].class ? info.getElementSize()
                                                      : -1;
            if ( nChar < 0 ) {
                logger.warning(
                    "Oh dear - can't serialize array of variable-length " +
                    "strings to VOTable - sorry" );
                return null;
            }

            /* Add an extra dimension since writing treats a string as an
             * array of chars. */
            int[] charDims = new int[ dims.length + 1 ];
            charDims[ 0 ] = nChar;
            System.arraycopy( dims, 0, charDims, 1, dims.length );

            /* Work out the arraysize attribute. */
            StringBuffer sbuf = new StringBuffer();
            for ( int i = 0; i < charDims.length; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( 'x' );
                }
                if ( i == charDims.length - 1 && charDims[ i ] < 0 ) {
                    sbuf.append( '*' );
                }
                else {
                    sbuf.append( charDims[ i ] );
                }
            }
            int ns = 0;
            if ( ! isVariable ) {
                ns = 1;
                for ( int i = 0; i < dims.length; i++ ) {
                    ns *= dims[ i ];
                }
            }
            final String arraysize = sbuf.toString();
            final int nString = ns;

            return new Encoder( info, cwrite.getDatatype() ) {
                char[] cbuf = new char[ nChar ];

                /*anonymousConstructor*/ {
                    putAtt( "arraysize", arraysize );
                }
              
                public String encodeAsText( Object val ) {
                    if ( val != null ) {
                        Object[] value = (Object[]) val;
                        StringBuffer sbuf = new StringBuffer();
                        for ( int i = 0; i < value.length; i++ ) {
                            Object el = value[ i ];
                            String str = el == null ? "" : el.toString();
                            int j = 0;
                            int limit = Math.min( str.length(), nChar );
                            for ( ; j < limit; j++ ) {
                                cbuf[ j ] = str.charAt( j );
                            }
                            for ( ; j < nChar; j++ ) {
                                cbuf[ j ] = ' ';
                            }
                            sbuf.append( new String( cbuf ) );
                        }
                        return sbuf.toString();
                    }
                    else {
                        return null;
                    }
                }

                public void encodeToStream( Object val, DataOutput out ) 
                        throws IOException {
                    Object[] value = val == null ? new Object[ 0 ] 
                                                 : (Object[]) val;
                    int slimit;
                    if ( isVariable ) {
                        slimit = value.length;
                        out.writeInt( nChar * slimit );
                    }
                    else {
                        slimit = Math.min( value.length, nString );
                    }
                    int is = 0;
                    for ( ; is < slimit; is++ ) {
                        Object v = value[ is ];
                        String str = v == null ? "" : v.toString();
                        int climit = Math.min( str.length(), nChar );
                        int ic = 0;
                        for ( ; ic < climit; ic++ ) {
                            cwrite.writeChar( out, str.charAt( ic ) );
                        }
                        for ( ; ic < nChar; ic++ ) {
                            cwrite.writeChar( out, '\0' );
                        }
                    }
                    if ( ! isVariable ) {
                        int nc = ( nString - is ) * nChar;
                        for ( int ic = 0; ic < nc; ic++ ) {
                            cwrite.writeChar( out, '\0' );
                        }
                    }
                }
            };
        }

        /* Not a type we can do anything with. */
        return null;
    }

    /**
     * Represents infinity as permitted by the VOTable standard.
     *
     * @param  isPositive  true for +infinity, false for -infinity
     */
    private static String infinityText( boolean isPositive ) {
        return isPositive ? "+Inf" : "-Inf";
    }

    /**
     * Encoder subclass which can encode scalar objects.
     */
    private static abstract class ScalarEncoder extends Encoder {

        private final String nullText;

        /**
         * Constructs a new ScalarEncoder.
         *
         * @param   info  valueinfo describing the encoder
         * @param   datatype  votable datatype attribute for this encoder
         * @param   nullString  string representation of the bad value for 
         *          this encoder (may be null)
         */
        ScalarEncoder( ValueInfo info, String datatype, String nullString ) {
            super( info, datatype );

            /* Set up bad value representation. */
            setNullString( nullString );
            this.nullText = nullString == null ? "" : nullString;
        }

        public String encodeAsText( Object val ) {
            return val == null ? nullText : val.toString();
        }
    }

    /**
     * Abstract Encoder subclass which can encode arrays.
     */
    private static abstract class ArrayEncoder extends Encoder {
        final Encoder1 enc1;

        ArrayEncoder( ValueInfo info, String datatype, int[] dims,
                      Encoder1 enc1 ) {
            super( info, datatype );
            this.enc1 = enc1;

            /* Set up the arraysize element. */
            StringBuffer sizeBuf = new StringBuffer();
            for ( int i = 0; i < dims.length; i++ ) {
                if ( i > 0 ) {
                    sizeBuf.append( 'x' );
                }
                if ( i == dims.length - 1 && dims[ i ] < 0 ) {
                    sizeBuf.append( '*' );
                }
                else {
                    sizeBuf.append( dims[ i ] );
                }
            }
            putAtt( "arraysize", sizeBuf.toString() );
        }

        public String encodeAsText( Object val ) {
            if ( val == null ) {
                return "";
            }
            else {
                int nel = Array.getLength( val );
                StringBuffer sbuf = new StringBuffer();
                for ( int i = 0; i < nel; i++ ) {
                    if ( i > 0 ) {
                        sbuf.append( ' ' );
                    }
                    sbuf.append( Array.get( val, i ).toString() );
                }
                return sbuf.toString();
            }
        }
    }

    /**
     * Encoder subclass which can encode variable length arrays.
     */
    private static class VariableArrayEncoder extends ArrayEncoder {
        VariableArrayEncoder( ValueInfo info, String datatype, int[] dims,
                              Encoder1 enc1 ) {
            super( info, datatype, dims, enc1 );
        }

        public void encodeToStream( Object val, DataOutput out )
                throws IOException {
            if ( val == null ) {
                out.writeInt( 0 );
            }
            else {
                int nel = Array.getLength( val );
                out.writeInt( nel );
                for ( int i = 0; i < nel; i++ ) {
                    enc1.encode1( val, i, out );
                }
            }
        }
    }

    /**
     * Encoder subclass which can encode fixed length arrays.
     */
    private static class FixedArrayEncoder extends ArrayEncoder {
        final int nfixed;
        FixedArrayEncoder( ValueInfo info, String datatype, int[] dims,
                           Encoder1 enc1 ) {
            super( info, datatype, dims, enc1 );
            int nfixed = 1;
            for ( int i = 0; i < dims.length; i++ ) {
                nfixed *= dims[ i ];
            }
            this.nfixed = nfixed;
        }

        public void encodeToStream( Object val, DataOutput out )
                throws IOException {
            int i = 0;
            if ( val != null ) {
                int nel = Array.getLength( val );
                int limit = Math.min( nel, nfixed );
                for ( ; i < limit; i++ ) {
                    enc1.encode1( val, i, out );
                }
            }
            for ( ; i < nfixed; i++ ) {
                enc1.pad1( out );
            }
        }
    }

    /**
     * Helper interface which defines the behaviour of an object which can
     * write one element of an array to a stream.
     */
    private static interface Encoder1 {

        /**
         * Writes one element of a given array to an output stream.
         *
         * @param  array  array object
         * @param  index  the index of <tt>array</tt> to be written
         * @param  out   destination stream
         */
        void encode1( Object array, int index, DataOutput out )
            throws IOException;

        /**
         * Writes one padding element to an output stream.
         * The streamed output must comprise the same number of bytes as
         * a call to <tt>encode1</tt>.
         *
         * @param  out   destination stream
         */
        void pad1( DataOutput out ) throws IOException;
    }

    /**
     * Handles character output.
     */
    private static abstract class CharWriter {
        private final String datatype_;

        /**
         * Implementation using 1-byte characters.
         * Only the lower 7 bits of a char are used, anything else is ignored.
         */
        public static final CharWriter ASCII = new CharWriter( "char" ) {
            public void writeChar( DataOutput out, char c ) throws IOException {
                out.write( (int) c );
            }
        };

        /**
         * Implementation using 2-byte characters.
         * The encoding is java's character storage, which is either identical
         * or similar (not sure which) to the deprecated Unicode UCS-2 encoding.
         */
        public static final CharWriter UCS2 = new CharWriter( "unicodeChar" ) {
            public void writeChar( DataOutput out, char c ) throws IOException {
                out.writeChar( c );
            }
        };

        /**
         * Constructor.
         *
         * @return   value of VOTable datatype attribute corresponding
         *           to this writer's output
         */
        public CharWriter( String datatype ) {
            datatype_ = datatype;
        }

        /**
         * Returns the value of the VOTable datatype attribute corresponding
         * to this writer's output.
         *
         * @return   datatype attribute value
         */
        public String getDatatype() {
            return datatype_;
        }

        /**
         * Writes a character to an output stream.
         *
         * @param  out  destination stream
         * @param  c   character to output
         */
        public abstract void writeChar( DataOutput out, char c )
                throws IOException;
    }
}
