package uk.ac.starlink.table.formats;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

    protected void printSeparator( OutputStream strm, int[] colwidths )
            throws IOException {
        for ( int i = 0; i < colwidths.length; i++ ) {
            strm.write( '+' );
            strm.write( '-' );
            for ( int j = 0; j < colwidths[ i ]; j++ ) {
                strm.write( '-' );
            }
            strm.write( '-' );
        }
        strm.write( '+' );
        strm.write( '\n' );
    }

    protected void printColumnHeads( OutputStream strm, int[] colwidths,
                                     ColumnInfo[] cinfos ) throws IOException {
        int ncol = cinfos.length;
        String[] heads = new String[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            heads[ i ] = cinfos[ i ].getName();
        }
        printSeparator( strm, colwidths );
        printLine( strm, colwidths, heads );
        printSeparator( strm, colwidths );
    }

    protected void printLine( OutputStream strm, int[] colwidths,
                              String[] data ) 
            throws IOException {
        for ( int i = 0; i < colwidths.length; i++ ) {
            strm.write( '|' );
            strm.write( ' ' );
            String datum = ( data[ i ] == null ) ? "" : data[ i ];
            int padding = colwidths[ i ] - datum.length();
            strm.write( getBytes( datum ), 0,
                        Math.min( colwidths[ i ], datum.length() ) );
            if ( padding > 0 ) {
                for ( int j = 0; j < padding; j++ ) {
                    strm.write( ' ' );
                }
            }
            strm.write( ' ' );
        }
        strm.write( '|' );
        strm.write( '\n' );
    }

    protected void printParam( OutputStream strm, String name, String value,
                               Class<?> clazz )
            throws IOException {
        strm.write( getBytes( name ) );
        strm.write( ':' );
        strm.write( ' ' );
        strm.write( getBytes( value ) );
        strm.write( '\n' );
    }
}
