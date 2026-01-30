package uk.ac.starlink.table.formats;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

    protected String formatSeparator( int[] colwidths ) {
        return "";
    }

    protected String formatLine( int[] colwidths, String[] data ) {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append( ' ' );
        appendItems( sbuf, colwidths, data );
        return sbuf.toString();
    }

    /**
     * Appends a row of formatted values to a supplied buffer.
     *
     * @param   sbuf  buffer to append to
     * @param  colwidths  column widths in characters
     * @param  array of data values
     */
    private void appendItems( StringBuilder sbuf, int[] colwidths,
                              String[] data ) {
        for ( int i = 0; i < colwidths.length; i++ ) {
            sbuf.append( ' ' );
            String datum = data[ i ];
            if ( datum == null || datum.length() == 0 ) {
                datum = "\"\"";
            }
            sbuf.append( datum );
            int padding = colwidths[ i ] - datum.length();
            if ( padding > 0 ) {
                for ( int j = 0; j < padding; j++ ) {
                    sbuf.append( ' ' );
                }
            }
        }
        sbuf.append( '\n' );
    }

    protected String formatColumnHeads( int[] colwidths, ColumnInfo[] cinfos ) {
        String[] heads =
            Arrays.stream( cinfos )
                  .map( c -> c.getName().replaceAll( "[ '\"#\\t\\r\\n]", "_" ) )
                  .toArray( n -> new String[ n ] );
        StringBuilder sbuf = new StringBuilder();
        sbuf.append( '#' );
        appendItems( sbuf, colwidths, heads );
        return sbuf.toString();
    }

    protected String formatParam( String name, String value, Class<?> clazz ) {
        return new StringBuilder()
              .append( '#' )
              .append( ' ' )
              .append( name )
              .append( ':' )
              .append( ' ' )
              .append( value.replaceAll( "[\\n\\r]", " " ) )
              .append( '\n' )
              .toString();
    }
}
