package uk.ac.starlink.table.formats;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.MultiStarTableWriter;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.ValueInfo;

/**
 * A <code>StarTableWriter</code> which outputs text to
 * a human-readable text file.
 * Table parameters (per-table metadata) can optionally be output 
 * as well as the table data themselves.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TextTableWriter extends AbstractTextTableWriter
                             implements MultiStarTableWriter {

    @SuppressWarnings("this-escape")
    public TextTableWriter() {
        super( new String[ 0 ], true );
        setMaxWidth( 40 );
    }

    /**
     * Returns "text";
     *
     * @return "text"
     */
    public String getFormatName() {
        return "text";
    }

    public String getMimeType() {
        return "text/plain";
    }

    public boolean docIncludesExample() {
        return true;
    }

    public String getXmlDescription() {
        return String.join( "\n",
            "<p>Writes tables in a simple text-based format",
            "designed to be read by humans.",
            "No reader exists for this format.",
            "</p>",
        "" );
    }

    /**
     * Returns true if the location argument is equal to "-",
     * indicating standard output.
     */
    @Override
    public boolean looksLikeFile( String location ) {
        return location.equals( "-" );
    }

    public void writeStarTables( TableSequence tableSeq, OutputStream out )
            throws IOException {
        int ix = 0;
        for ( StarTable table; ( table = tableSeq.nextTable() ) != null; ix++) {
            if ( ix > 0 ) {
                out.write( '\n' );
            }
            writeStarTable( table, out );
        }
    }

    public void writeStarTables( TableSequence tableSeq, String location,
                                 StarTableOutput sto ) throws IOException {
        OutputStream out = sto.getOutputStream( location );
        try {
            out = new BufferedOutputStream( out );
            writeStarTables( tableSeq, out );
            out.flush();
        }
        finally {
            out.close();
        }
    }

    protected String formatValue( Object val, ValueInfo vinfo, int width ) {
        return vinfo.formatValue( val, width );
    }

    protected String formatSeparator( int[] colwidths ) {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < colwidths.length; i++ ) {
            sbuf.append( '+' );
            sbuf.append( '-' );
            for ( int j = 0; j < colwidths[ i ]; j++ ) {
                sbuf.append( '-' );
            }
            sbuf.append( '-' );
        }
        sbuf.append( '+' );
        sbuf.append( '\n' );
        return sbuf.toString();
    }

    protected String formatColumnHeads( int[] colwidths, ColumnInfo[] cinfos ) {
        String[] heads =
            Arrays.stream( cinfos ).map( c -> c.getName() )
                  .toArray( n -> new String[ n ] );
        return new StringBuffer()
           .append( formatSeparator( colwidths ) )
           .append( formatLine( colwidths, heads ) )
           .append( formatSeparator( colwidths ) )
           .toString();
    }

    protected String formatLine( int[] colwidths, String[] data ) {
        StringBuilder sbuf = new StringBuilder();
        for ( int i = 0; i < colwidths.length; i++ ) {
            sbuf.append( "| " );
            String datum = ( data[ i ] == null ) ? "" : data[ i ];
            if ( datum.length() > colwidths[ i ] ) {
                datum = datum.substring( 0, colwidths[ i ] );
            }
            int padding = colwidths[ i ] - datum.length();
            sbuf.append( datum );
            if ( padding > 0 ) {
                for ( int j = 0; j < padding; j++ ) {
                    sbuf.append( ' ' );
                }
            }
            sbuf.append( ' ' );
        }
        sbuf.append( '|' );
        sbuf.append( '\n' );
        return sbuf.toString();
    }

    protected String formatParam( String name, String value, Class<?> clazz ) {
        return new StringBuffer()
           .append( name )
           .append( ": " )
           .append( value )
           .append( '\n' )
           .toString();
    }
}
