package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * A StarTableWriter which outputs text to a simple format machine-readable
 * text file.  This format is suitable for reading using 
 * {@link AsciiStarTable} and {@link AsciiTableBuilder}.
 *
 * @author   Mark Taylor (Starlink)
 * @since    29 Mar 2004
 */
public class AsciiTableWriter extends AbstractTextTableWriter {

    @SuppressWarnings("this-escape")
    public AsciiTableWriter() {
        super( new String[] { "txt" }, false );
        setMaxWidth( 158 );
        setEncoding( StandardCharsets.US_ASCII );
    }

    protected String formatValue( Object val, ValueInfo vinfo, int width ) {

        /* If it's null, return a null value representation. */
        if ( Tables.isBlank( val ) ) {
            return "\"\"";
        }

        /* Format the value in the usual way. */
        String fval = vinfo.formatValue( val, width );

        /* If it's a number, make certain it's not been truncated in a
         * bad way (the formatter sometimes adds "..." to it). */
        if ( val instanceof Number ) {
            try {
                Object nval0 = vinfo.unformatString( fval );
                Number nval = (Number) val;
                int ival = nval.intValue();
                if ( ival == (int) nval.doubleValue() &&
                     ( ! ( nval0 instanceof Number ) ||
                       ival != ((Number) nval0).intValue() ) ) {
                    fval = val.toString();
                }
            }
            catch ( RuntimeException e ) {
                fval = val.toString();
            }
            int nchar = fval.length();
            StringBuffer buf = new StringBuffer( nchar );
            boolean changed = false;
            for ( int i = 0; i < nchar; i++ ) {
                char c = fval.charAt( i );
                switch ( c ) {
                    case ' ':
                    case '\t':
                    case '\n':
                        changed = true;
                        break;
                    default:
                        buf.append( c );
                }
            }
            return changed ? buf.toString() : fval;
        }

        /* Otherwise, make sure control characters are escaped. */
        else {
            int nchar = fval.length();
            StringBuffer buf = new StringBuffer( nchar + 2 );
            buf.append( '"' );
            boolean quote = false;
            for ( int i = 0; i < nchar; i++ ) {
                char c = fval.charAt( i );
                switch ( c ) {
                    case '\t':
                    case '\n':
                    case '\r':
                    case ' ':
                        quote = true;
                        buf.append( ' ' );
                        break;
                    case '#':
                    case '\\':
                    case '\'':
                    case '"':
                        quote = true;
                        buf.append( '\\' );
                        buf.append( c );
                        break;
                    default:
                        buf.append( c );
                }
            }
            if ( quote ) {
                buf.append( '"' );
                return buf.toString();
            }
            else {
                return fval;
            }
        }
    }

    /**
     * Returns "ascii".
     *
     * @return   output format
     */
    public String getFormatName() {
        return "ascii";
    }

    public boolean docIncludesExample() {
        return true;
    }

    public String getXmlDescription() {
        return readText( "AsciiTableWriter.xml" );
    }

    protected void printSeparator( OutputStream strm, int[] colwidths ) {
        // no action.
    }

    protected void printLine( OutputStream strm, int[] colwidths,
                              String[] data ) throws IOException {
        strm.write( ' ' );
        printItems( strm, colwidths, data );
    }

    private void printItems( OutputStream strm, int[] colwidths,
                             String[] data ) throws IOException {
        for ( int i = 0; i < colwidths.length; i++ ) {
            strm.write( ' ' );
            String datum = data[ i ];
            if ( datum == null || datum.length() == 0 ) {
                datum = "\"\"";
            }
            strm.write( getBytes( datum ) );
            int padding = colwidths[ i ] - datum.length();
            if ( padding > 0 ) {
                for ( int j = 0; j < padding; j++ ) {
                    strm.write( ' ' );
                }
            }
        }
        strm.write( '\n' );
    }

    protected void printColumnHeads( OutputStream strm, int[] colwidths,
                                     ColumnInfo[] cinfos ) throws IOException {
        int ncol = cinfos.length;
        String[] heads = new String[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            byte[] nameBuf = getBytes( cinfos[ i ].getName() );
            for ( int j = 0; j < nameBuf.length; j++ ) {
                switch ( (char) nameBuf[ j ] ) {
                    case ' ':
                    case '\'':
                    case '"':
                    case '#':
                    case '\t':
                    case '\r':
                    case '\n':
                        nameBuf[ j ] = (byte) '_';
                }
            }
            heads[ i ] = new String( nameBuf );
        }
        strm.write( '#' );
        printItems( strm, colwidths, heads );
    }

    protected void printParam( OutputStream strm, String name, String value,
                               Class<?> clazz )
            throws IOException {
        strm.write( '#' );
        strm.write( ' ' );
        strm.write( getBytes( name ) );
        strm.write( ':' );
        strm.write( ' ' );
        byte[] valbuf = getBytes( value );
        for ( int i = 0; i < valbuf.length; i++ ) {
            switch ( valbuf[ i ] ) {
                case '\r':
                case '\n':
                    valbuf[ i ] = ' ';
                    break;
            }
        }
        strm.write( valbuf );
        strm.write( '\n' );
    }
}
