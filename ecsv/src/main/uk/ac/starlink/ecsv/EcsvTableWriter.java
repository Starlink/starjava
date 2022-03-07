package uk.ac.starlink.ecsv;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.formats.DocumentedStreamStarTableWriter;
import uk.ac.starlink.util.ByteList;
import uk.ac.starlink.util.ConfigMethod;

/**
 * TableWriter for ECSV output format.
 * The format currently supported is ECSV 1.0, as documented at
 * <a href="https://github.com/astropy/astropy-APEs/blob/master/APE6.rst"
 *    >Astropy APE6</a>.
 *
 * <p>The current implementation avoids use of any YAML serialization library,
 * it just uses print statements.  This may facilitate its use in some
 * contexts, and doing it like this seems straightforward enough.
 * However if it turns out that the serialization is more complicated
 * or error-prone than I thought it was, it might be worth revisiting
 * this decision and using for instance the serialization facilities of
 * the YAML parser library that is in any case a dependency of this package.
 *
 * @author   Mark Taylor
 * @since    29 Apr 2020
 */
public class EcsvTableWriter extends DocumentedStreamStarTableWriter {

    private final String formatName_;
    private final byte badChar_;
    private final String nl_;
    private final String indent_;
    private final ByteList bbuf_;
    private char delimiter_;
    private String nullRep_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ecsv" );

    /** Meta map key for table name string value. */
    public static final String TABLENAME_METAKEY = "name";

    /** Meta map key for UCD string value. */
    public static final String UCD_METAKEY = "ucd";

    /** Meta map key for Utype string value. */
    public static final String UTYPE_METAKEY = "utype";

    /** Meta map key for Xtype string value. */
    public static final String XTYPE_METAKEY = "xtype";

    /** Instance using spaces for delimiters. */
    public static final EcsvTableWriter SPACE_WRITER =
        new EcsvTableWriter( ' ', "-space" );

    /** Instance using commas for delimiters. */
    public static final EcsvTableWriter COMMA_WRITER =
        new EcsvTableWriter( ',', "-comma" );

    private static final Collection<String> EXCLUDE_AUXMETAS = 
            new HashSet<String>( Arrays.asList( new String[] {
        Tables.NULL_VALUE_INFO.getName(),
        Tables.UBYTE_FLAG_INFO.getName(),
        "Datatype",    // VOStarTable.DATATYPE_INFO
        "VOTable ID",  // VOStarTable.ID_INFO
        "VOTable ref", // VOStarTable.REF_INFO
        "Type",        // VOStarTable.TYPE_INFO
    } ) );

    /**
     * Constructs a writer with default characteristics.
     */
    public EcsvTableWriter() {
        this( ' ', null );
    }

    /**
     * Constructs a writer with a given delimiter character.
     *
     * @param  delimiter  field delimiter character; should be a space or comma
     * @param  nameSuffix  string to append to "ECSV" to provide the format name
     */
    public EcsvTableWriter( char delimiter, String nameSuffix ) {
        super( new String[] { "ecsv" } );
        setDelimiter( Character.toString( delimiter ) );
        formatName_ = "ECSV" + ( nameSuffix == null ? "" : nameSuffix );
        badChar_ = (byte) '?';
        nl_ = "\n";
        indent_ = "  ";
        bbuf_ = new ByteList();
    }

    public String getFormatName() {
        return formatName_;
    }

    /**
     * Returns "text/plain".
     */
    public String getMimeType() {
        return "text/plain";
    }

    public boolean docIncludesExample() {
        return true;
    }

    public String getXmlDescription() {
        return readText( "EcsvTableWriter.xml" );
    }

    /**
     * Sets the delimiter.  ECSV only permits the space or comma.
     *
     * @param  delimiter  delimiter character;
     *                    may be "space", "comma", " " or ","
     * @throws  IllegalArgumentException  if not one of the permitted values
     */
    @ConfigMethod(
        property = "delimiter",
        doc = "<p>Delimiter character, which for ECSV may be "
            + "either a space or a comma. "
            + "Permitted values are "
            + "\"<code>space</code>\" or \"<code>comma</code>\".</p>",
        usage = "comma|space",
        example = "comma"
    )
    public void setDelimiter( String delimiter ) {
        if ( " ".equals( delimiter ) || "space".equals( delimiter ) ) {
            delimiter_ = ' ';
        }
        else if ( ",".equals( delimiter ) || "comma".equals( delimiter ) ) {
            delimiter_ = ',';
        }
        else {
            throw new IllegalArgumentException( "Illegal delimiter \""
                                              + delimiter + "\"" );
        }
        nullRep_ = delimiter_ == ' ' ? "\"\"" : "";
    }

    /**
     * Returns the delimiter character, either a space or a comma.
     *
     * @return  delimiter character
     */
    public char getDelimiter() {
        return delimiter_;
    }

    public void writeStarTable( StarTable table, OutputStream out )
            throws IOException {

        /* Prepare per-column encoders. */
        int ncol = table.getColumnCount();
        EcsvEncoder[] encoders = new EcsvEncoder[ ncol ];
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnInfo cinfo = table.getColumnInfo( ic );
            encoders[ ic ] = EcsvEncoder.createEncoder( cinfo, delimiter_ );
            if ( encoders[ ic ] == null ) {
                logger_.warning( "Will not write un-ECSV-able column "
                               + cinfo );
            }
        }

        /* Write preamble. */
        writeHeaderLine( out, "%ECSV 1.0" );
        writeHeaderLine( out, "---" );

        /* Write delimiter if required. */
        if ( delimiter_ != ' ' ) {
            writeHeaderLine( out, "delimiter: '" + delimiter_ + "'" );
        }

        /* Write column metadata. */
        writeHeaderLine( out, "datatype:" );
        StringBuilder nbuf = new StringBuilder();
        boolean isAfter0 = false;
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnInfo colInfo = table.getColumnInfo( ic );
            EcsvEncoder encoder = encoders[ ic ];
            if ( encoder != null ) {
                if ( isAfter0 ) {
                    nbuf.append( delimiter_ );
                }
                isAfter0 = true;
                nbuf.append( EcsvEncoder
                            .quoteString( colInfo.getName(), delimiter_ ) );
                writeHeaderLine( out, "-" );
                writeHeaderPairString( out, 1, "name", colInfo.getName() );
                writeHeaderPairString( out, 1, "datatype",
                                       encoder.getDatatype() );
                writeHeaderPairString( out, 1, "subtype",
                                       encoder.getSubtype() );
                writeHeaderPairString( out, 1, "unit",
                                       colInfo.getUnitString() );
                writeHeaderPairString( out, 1, "description",
                                       colInfo.getDescription());
                Map<String,Object> colmeta = getColumnMeta( colInfo );
                writeMetaMap( out, 1, "meta", colmeta );
            }
        }

        /* Write per-table metadata. */
        Map<String,Object> tmetaMap = new LinkedHashMap<>();
        String tname = table.getName();
        if ( tname != null && tname.trim().length() > 0 ) {
            tmetaMap.put( TABLENAME_METAKEY, tname );
        }
        tmetaMap.putAll( getMetaMap( table.getParameters() ) );
        writeMetaMap( out, 0, "meta", tmetaMap );

        /* Write the required row giving non-YAML column names. */
        writeLine( out, nbuf );

        /* Write data rows. */
        RowSequence rseq = table.getRowSequence();
        StringBuilder rbuf = new StringBuilder();
        try {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                rbuf.setLength( 0 );
                boolean isAfter = false;
                for ( int ic = 0; ic < ncol; ic++ ) {
                    EcsvEncoder encoder = encoders[ ic ];
                    if ( encoder != null ) {
                        if ( isAfter ) {
                            rbuf.append( delimiter_ );
                        }
                        isAfter = true;
                        String ctxt = encoder.encode( row[ ic ] );
                        rbuf.append( ctxt == null ? nullRep_ : ctxt );
                    }
                }
                writeLine( out, rbuf );
            }
        }
        finally {
            rseq.close();
        }
    }

    /**
     * Adapts a list of metadata items to a simple Map.
     *
     * @param  dvals  list of DescribedValue objects
     * @return  map of key-value pairs suitable for writing to an ECSV meta item
     */
    private Map<String,Object> getMetaMap( List<DescribedValue> dvals  ) {
        Map<String,Object> map = new LinkedHashMap<>();
        for ( DescribedValue dval : dvals ) {
            Object value = dval.getValue();
            map.put( dval.getInfo().getName(), value );
        }
        return map;
    }

    /**
     * Returns the per-column metadata of a column as a map.
     * This includes the things that we want to serialize that are not
     * covered by the standard ECSV format.
     *
     * @param  colInfo  column metadata object
     * @return  map of miscellaneous metadata
     */
    private Map<String,Object> getColumnMeta( ColumnInfo colInfo ) {
        Map<String,Object> map = new LinkedHashMap<>();

        /* Add standard STIL metadata items if present. */
        String ucd = colInfo.getUCD();
        String utype = colInfo.getUtype();
        String xtype = colInfo.getXtype();
        if ( ucd != null ) {
            map.put( UCD_METAKEY, ucd );
        }
        if ( utype != null ) {
            map.put( UTYPE_METAKEY, utype );
        }
        if ( xtype != null ) {
            map.put( XTYPE_METAKEY, xtype );
        }

        /* Add miscellaneous metadata, excluding certain items. */
        List<DescribedValue> auxItems = new ArrayList<>();
        for ( DescribedValue dval : colInfo.getAuxData() ) {
            if ( ! EXCLUDE_AUXMETAS.contains( dval.getInfo().getName() ) ) {
                auxItems.add( dval );
            }
        }
        map.putAll( getMetaMap( auxItems ) );
        return map;
    }

    /**
     * Writes a metadata array as a YAML key-&gt;mapping pair,
     * if non-blank mapping content is present.
     *
     * @param  out   output stream
     * @param  nIndent  indentation level of key
     * @param  metaKey  key string
     * @return  metaMap   content for value; if null or empty,
     *                    nothing is written
     */
    private void writeMetaMap( OutputStream out, int nIndent, String metaKey,
                               Map<String,?> metaMap )
            throws IOException {
        if ( metaMap != null && ! metaMap.isEmpty() ) {
            writeHeaderLine( out, repeatIndent( nIndent ) + metaKey + ":" );
            for ( Map.Entry<String,?> entry : metaMap.entrySet() ) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if ( value instanceof String ||
                     value instanceof Number ||
                     value instanceof Boolean ) {
                    final String txtval;
                    if ( Boolean.TRUE.equals( value ) ) {
                        txtval = "True";
                    }
                    else if ( Boolean.FALSE.equals( value ) ) {
                        txtval = "False";
                    }
                    else {
                        txtval = value.toString();
                    }
                    writeHeaderPairString( out, nIndent + 1, key, txtval );
                }
                else if ( value instanceof boolean[] ||
                          value instanceof byte[] ||
                          value instanceof short[] ||
                          value instanceof int[] ||
                          value instanceof long[] ||
                          value instanceof float[] ||
                          value instanceof double[] ||
                          value instanceof String[] ) {
                    writeHeaderPairArray( out, nIndent + 1, key, value );
                }
            }
        }
    }

    /**
     * Writes a name-stringValue pair as a YAML mapping entry
     * in the ECSV header.
     *
     * <p>This probably isn't bulletproof, but it can handle most
     * special characters including newlines.  Hopefully it's good
     * enough for the values that we're likely to encounter.
     *
     * @param   out  output stream
     * @param   nIndent  indentation level
     * @param   key   entry key
     * @param   value  entry string value
     */
    private void writeHeaderPairString( OutputStream out, int nIndent,
                                        String key, String value )
            throws IOException {
        String indentTxt = repeatIndent( nIndent );
        if ( value != null && value.trim().length() > 0 ) {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( indentTxt )
                .append( sanitiseYamlScalar( key ) )
                .append( ": " );
            if ( value.indexOf( '\n' ) < 0 ) { 
                sbuf.append( sanitiseYamlScalar( value ) );
                writeHeaderLine( out, sbuf );
            }
            else {
                writeHeaderLine( out, sbuf );
                writeHeaderLine( out, indentTxt + '|' );
                for ( String line : value.split( "\n", -1 ) ) {
                    writeHeaderLine( out, indentTxt + ' ' + line );
                }
            }
        }
    }

    /**
     * Writes a name-arrayValue pair as a YAML mapping entry
     * in the ECSV header.
     *
     * @param   out  output stream
     * @param   nIndent  indentation level
     * @param   key   entry key
     * @param   array  entry array value
     */
    private void writeHeaderPairArray( OutputStream out, int nIndent,
                                       String key, Object array )
            throws IOException {
        String indentTxt = repeatIndent( nIndent );
        Class<?> cclazz = array.getClass().getComponentType();
        assert array != null && cclazz != null;
        int n = Array.getLength( array );
        StringBuilder sbuf = new StringBuilder();
        sbuf.append( indentTxt )
            .append( sanitiseYamlScalar( key ) )
            .append( ": " );
        if ( n < 16 && cclazz != String.class ) {
            sbuf.append( "[" );
            for ( int i = 0; i < n; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ", " );
                }
                sbuf.append( String.valueOf( Array.get( array, i ) ) );
            }
            sbuf.append( "]" );
            writeHeaderLine( out, sbuf );
        }
        else {
            writeHeaderLine( out, sbuf );
            for ( int i = 0; i < n; i++ ) {
                String elTxt = String.valueOf( Array.get( array, i ) );
                sbuf = new StringBuilder();
                sbuf.append( indentTxt )
                    .append( "- " );
                if ( elTxt.indexOf( '\n' ) < 0 ) {
                    sbuf.append( sanitiseYamlScalar( elTxt ) );
                    writeHeaderLine( out, sbuf );
                }
                else {
                    writeHeaderLine( out, sbuf );
                    writeHeaderLine( out, indentTxt + '|' );
                    for ( String line : elTxt.split( "\n", -1 ) ) {
                        writeHeaderLine( out, indentTxt + ' ' + line );
                    }
                }
            }
        }
    }

    /**
     * Returns a string giving a fixed number of repeats of the indent text.
     *
     * @param  n  number of indents
     * @return   repeated string
     */
    private String repeatIndent( int n ) {
        StringBuilder sbuf = new StringBuilder( n * indent_.length() );
        for ( int i = 0; i < n; i++ ) {
            sbuf.append( indent_ );
        }
        return sbuf.toString();
    }

    /**
     * Writes a line of YAML in the ECSV header.
     *
     * @param  out  output stream
     * @param  txt  YAML line to write
     */
    private void writeHeaderLine( OutputStream out, CharSequence txt )
            throws IOException {
        out.write( '#' );
        out.write( ' ' );
        writeLine( out, txt );
    }

    /**
     * Writes a line to the output stream, terminated by a newline.
     *
     * @param  out  output stream
     * @param  txt  line to write (excluding newline)
     */
    private void writeLine( OutputStream out, CharSequence txt )
            throws IOException {
        int nc = txt.length();
        bbuf_.clear();
        for ( int ic = 0; ic < nc; ic++ ) {
            char c = txt.charAt( ic );
            byte b = (byte) ( c & 0x7f );
            bbuf_.add( b == c ? b : badChar_ );
        }
        for ( int ic = 0; ic < nl_.length(); ic++ ) {
            bbuf_.add( (byte) nl_.charAt( ic ) );
        }
        out.write( bbuf_.getByteBuffer(), 0, bbuf_.size() );
    }

    /**
     * Returns a YAML-flow-mode-safe string representing a scalar.
     *
     * @param  txt   raw text
     * @return   text suitable for use as scalar in flow mode
     */
    private static String sanitiseYamlScalar( String txt ) {
        if ( isPlainScalar( txt ) ) {
            return txt;
        }
        else {
            int leng = txt.length();
            StringBuilder sbuf = new StringBuilder( leng + 2 );
            char squote = '\'';
            sbuf.append( squote );
            for ( int ic = 0; ic < leng; ic++ ) {
                char c = txt.charAt( ic );
                sbuf.append( c );
                if ( c == squote ) {
                    sbuf.append( squote );
                }
            }
            sbuf.append( squote );
            return sbuf.toString();
        }
    }

    /**
     * Indicates whether a string is suitable for unescaped use as
     * a plain YAML scalar in flow mode.
     * This is implemented conservatively with reference to YAML 1.1
     * section 9.1.3.
     *
     * @param  txt  unescaped text
     * @return  true if it's known safe to use txt as it stands as a
     *          plain flow scalar
     */
    private static boolean isPlainScalar( String txt ) {
        int leng = txt.length();
        if ( leng > 0 ) {
            switch ( txt.charAt( 0 ) ) {
                case ' ':
                case '-':
                case '?':
                case ':':
                case ',':
                case '[':
                case ']':
                case '{':
                case '}':
                case '#':
                case '&':
                case '*':
                case '!':
                case '|':
                case '>':
                case '\'':
                case '"':
                case '%':
                case '@':
                case '`':
                    return false;
                default:
            }
        }
        for ( int i = 0; i < leng; i++ ) {
            char c = txt.charAt( i );
            if ( c < 0x20 || c > 0x7f ) {
                return false;
            }
            switch ( c ) {
                case '[':
                case ']':
                case '{':
                case '}':
                case ',':
                    return false;
                default:
            }
        }
        if ( txt.contains( ": " ) ||
             txt.contains( " #" ) ) {
            return false;
        }
        return true;
    }

    /**
     * Returns a list of ECSV writers with variant characteristics.
     *
     * @return  variant EcsvTableWriters
     */
    public static StarTableWriter[] getStarTableWriters() {
        return new StarTableWriter[] {
            new EcsvTableWriter(),
            COMMA_WRITER,
        };
    }
}
