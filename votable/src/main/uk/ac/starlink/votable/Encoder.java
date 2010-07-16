package uk.ac.starlink.votable;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Iterator;
import java.util.HashMap;
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

    final ValueInfo info;
    final Map attMap = new HashMap();
    String description;
    String nullString;
    String links;
    private String content;

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
        this.info = info;

        /* Datatype attribute. */
        attMap.put( "datatype", datatype.trim() );

        /* Name attribute. */
        String name = info.getName();
        if ( name != null && name.trim().length() > 0 ) {
            attMap.put( "name", name.trim() );
        }

        /* Unit attribute. */
        String units = info.getUnitString();
        if ( units != null && units.trim().length() > 0 ) {
            attMap.put( "unit", units.trim() );
        }

        /* UCD attribute. */
        String ucd = info.getUCD();
        if ( ucd != null && ucd.trim().length() > 0 ) {
            attMap.put( "ucd", ucd.trim() );
        }

        /* Utype attribute. */
        String utype = info.getUtype();
        if ( utype != null && utype.trim().length() > 0 ) {
            attMap.put( "utype", utype.trim() );
        }

        /* Column auxiliary metadata items. */
        if ( info instanceof ColumnInfo ) {
            ColumnInfo cinfo = (ColumnInfo) info;

            /* ID attribute. */
            String id = 
                (String) cinfo.getAuxDatumValue( VOStarTable.ID_INFO,
                                                 String.class );
            if ( id != null && id.trim().length() > 0 ) {
                attMap.put( "ID", id.trim() );
            }

            /* Ref attribute. */
            String ref = (String) cinfo.getAuxDatumValue( VOStarTable.REF_INFO,
                                                          String.class );
            if ( ref != null && ref.trim().length() > 0 ) {
                attMap.put( "ref", ref.trim() );
            }

            /* XType attribute. */
            String xtype =
                (String) cinfo.getAuxDatumValue( VOStarTable.XTYPE_INFO,
                                                 String.class );
            if ( xtype != null && xtype.trim().length() > 0 ) {
                attMap.put( "xtype", xtype.trim() );
            }

            /* Width attribute. */
            Integer width = 
                (Integer) cinfo.getAuxDatumValue( VOStarTable.WIDTH_INFO,
                                                  Integer.class );
            if ( width != null && width.intValue() > 0 ) {
                attMap.put( "width", width.toString() );
            }

            /* Precision attribute. */
            String precision =
                (String) cinfo.getAuxDatumValue( VOStarTable.PRECISION_INFO,
                                                 String.class );
            if ( precision != null && precision.trim().length() > 0 ) {
                attMap.put( "precision", precision.trim() );
            }
        }

        /* Description information. */
        String desc = info.getDescription();
        if ( desc != null ) {
            desc = desc.trim();
            if ( desc.length() > 0 ) { 
                description = "<DESCRIPTION>"
                            + VOSerializer.formatText( desc )
                            + "</DESCRIPTION>";
            }
        }

        /* URL-type auxiliary metadata can be encoded as LINK elements. */
        if ( info instanceof ColumnInfo ) {
            StringBuffer linksBuf = new StringBuffer();
            for ( Iterator it = ((ColumnInfo) info).getAuxData().iterator();
                  it.hasNext(); ) {
                DescribedValue dval = (DescribedValue) it.next();
                ValueInfo linkInfo = dval.getInfo();
                if ( URL.class.equals( linkInfo.getContentClass() ) ) {
                    String linkName = linkInfo.getName();
                    URL linkUrl = (URL) dval.getValue();
                    if ( linkName != null && linkUrl != null ) {
                        if ( linksBuf.length() > 0 ) {
                            linksBuf.append( '\n' );
                        }
                        linksBuf.append( "<LINK" )
                                .append( VOSerializer
                                        .formatAttribute( "title", linkName ) )
                                .append( VOSerializer
                                        .formatAttribute( "href", 
                                                          linkUrl.toString() ) )
                                .append( "/>" );
                    }
                }
            }
            links = linksBuf.toString();
        }
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
        if ( content == null ) {
            StringBuffer contBuf = new StringBuffer();
            if ( description != null && description.trim().length() > 0 ) {
                contBuf.append( '\n' )
                       .append( description );
            }
            if ( nullString != null && nullString.trim().length() > 0 ) {
                contBuf.append( '\n' )
                       .append( "<VALUES null='" + nullString + "'/>" );
            }
            if ( links != null && links.trim().length() > 0 ) {
                contBuf.append( '\n' )
                       .append( links );
            }
            content = contBuf.toString();
        }
        return content;
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
    public Map getFieldAttributes() {
        return attMap;
    }

    /**
     * Sets the null representation for this encoder.
     *
     * @param   nullString  null representation
     */
    public void setNullString( String nullString ) {
        this.nullString = nullString;
    }

    /**
     * Returns an Encoder suitable for encoding values described by a
     * given ValueInfo object.
     * If <tt>info</tt> is a ColumnInfo, then the preferred binary 
     * representation of bad values can be submitted in its auxiliary
     * metadata under the key {@link Tables#NULL_VALUE_INFO}.
     *
     * @param   info  a description of the type of value which needs to
     *          be encoded
     * @return  an encoder object which can do it
     */
    public static Encoder getEncoder( ValueInfo info ) {

        Class clazz = info.getContentClass();
        int[] dims = info.getShape();
        final boolean isNullable = info.isNullable();
        final boolean isVariable = dims != null 
                                && dims.length > 0 
                                && dims[ dims.length - 1 ] < 0;

        /* Try to work out a representation to use for blank integer values. */
        Number nullObj = null;
        if ( info instanceof ColumnInfo ) {
            DescribedValue nullValue = 
                ((ColumnInfo) info).getAuxDatum( Tables.NULL_VALUE_INFO );
            if ( nullValue != null ) {
                Object o = nullValue.getValue();
                if ( o instanceof Number ) {
                    nullObj = (Number) o;
                }
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

        else if ( clazz == Byte.class ||
                  clazz == Short.class ) {
            final int badVal = nullObj == null ? Short.MIN_VALUE 
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
            final int badVal = nullObj == null ? Integer.MIN_VALUE
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
            final long badVal = nullObj == null ? Long.MIN_VALUE
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
                public void encodeToStream( Object val, DataOutput out )
                        throws IOException {
                    Number value = (Number) val;
                    out.writeDouble( value == null ? Double.NaN
                                                   : value.doubleValue() );
                }
            };
        }

        else if ( clazz == Character.class ) {
            final int badVal = (int) '\0';
            return new ScalarEncoder( info, "char", null ) {
                /* For a single character take care to add the attribute
                 * arraysize="1" - although this is implicit according to
                 * the standard, it's often left off and assumed to be
                 * equivalent to arraysize="*".  This makes sure there is
                 * no ambiguity. */
                /*anonymousConstructor*/ {
                    attMap.put( "arraysize", "1" );
                }
                public void encodeToStream( Object val, DataOutput out )
                        throws IOException {
                    Character value = (Character) val;
                    out.write( value == null ? badVal 
                                             : (int) value.charValue() );
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
                final byte[] padBuf = new byte[ nChar ];
                return new Encoder( info, "char" ) {
                    /*anonymousConstructor*/ {
                        attMap.put( "arraysize", Integer.toString( nChar ) );
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
                                out.write( value.charAt( i ) );
                            }
                        }
                        out.write( padBuf, 0, nChar - i );
                    }
                };
            }

            /* Variable length strings. */
            else {
                return new Encoder( info, "char" ) {
                    /*anonymousConstructor*/ {
                        attMap.put( "arraysize", "*" );
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
                                out.write( value.charAt( i ) );
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

            return new Encoder( info, "char" ) {
                char[] cbuf = new char[ nChar ];
                byte[] bbuf = new byte[ nChar ];
                byte[] padding = new byte[ nChar ];

                /*anonymousConstructor*/ {
                    attMap.put( "arraysize", arraysize );
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
                            bbuf[ ic ] = (byte) str.charAt( ic );
                        }
                        for ( ; ic < nChar; ic++ ) {
                            bbuf[ ic ] = (byte) '\0';
                        }
                        out.write( bbuf );
                    }
                    if ( ! isVariable ) {
                        for ( ; is < nString; is++ ) {
                            out.write( padding );
                        }
                    }
                }
            };
        }

        /* Not a type we can do anything with. */
        return null;
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
            attMap.put( "arraysize", sizeBuf.toString() );
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
}
