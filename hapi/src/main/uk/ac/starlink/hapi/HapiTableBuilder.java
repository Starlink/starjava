package uk.ac.starlink.hapi;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.formats.DocumentedIOHandler;
import uk.ac.starlink.table.formats.DocumentedTableBuilder;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.IOSupplier;

/**
 * TableBuilder implementation for HAPI data streams.
 * This only works for binary- and csv-format streams,
 * and they must contain a HAPI header as obtained using the
 * HAPI Data endpoint with <code>include=header</code>.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2024
 */
public class HapiTableBuilder extends DocumentedTableBuilder {

    public HapiTableBuilder() {
        super( new String[] { "hapi" } );
    }

    public boolean canStream() {
        return true;
    }

    public String getFormatName() {
        return "HAPI";
    }

    @Override
    public boolean looksLikeFile( String location ) {
        return super.looksLikeFile( location )
            || ( location.startsWith( "http" ) &&
                 location.indexOf( "data?" ) > 0 &&
                 location.indexOf( "include=header" ) > 0 );
    }
    
    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public boolean docIncludesExample() {
        return false;
    }

    public String getXmlDescription() {
        return String.join( "\n",
            "<p>HAPI, the",
            "<a href='http://hapi-server.org/'",
            "   >Heliophysics Data Application Programmer’s Interface</a>",
            "is a protocol for serving streamed time series data.",
            "This reader can read HAPI CSV and binary tables",
            "if they include header information",
            "(the <code>include=header</code> request parameter",
            " must be present).",
            "An example HAPI URL is",
            "<verbatim>",
            "   https://vires.services/hapi/data?dataset=GRACE_A_MAG" +
               "&amp;start=2009-01-01&amp;stop=2009-01-02" +
               "&amp;include=header",
            "</verbatim>",
            "</p>",
            "<p>While HAPI data is normally accessed directly",
            "from the service,",
            "it is possible to download a HAPI stream to a local file",
            "and use this handler to read it from disk.",
            "</p>",
        "" );
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {
        if ( !isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "No HAPI header" );
        }
        final HapiInfo hdr;
        final Byte byte0;
        try ( InputStream in = datsrc.getInputStream() ) {
            int[] overread1 = new int[ 1 ];
            hdr = HapiInfo.fromCommentedStream( in, overread1 );
            int b0 = overread1[ 0 ];
            byte0 = ( b0 & 0xff ) == b0 ? Byte.valueOf( (byte) b0 ) : null;
        }
        HapiTableReader rdr = new HapiTableReader( hdr.getParameters() );
        String fmt = hdr.getFormat();
        IOSupplier<RowSequence> rseqSupplier = () -> {
            InputStream in = new BufferedInputStream( datsrc.getInputStream() );
            HapiInfo.fromCommentedStream( in, null );
            return rdr.createRowSequence( in, byte0, fmt );
        };
        return rdr.createStarTable( rseqSupplier );
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws IOException {
        int[] overread1 = new int[ 1 ];
        HapiInfo hdr = HapiInfo.fromCommentedStream( in, overread1 );
        String fmt = hdr.getFormat();
        int b0 = overread1[ 0 ];
        Byte byte0 = ( b0 & 0xff ) == b0 ? Byte.valueOf( (byte) b0 ) : null;
        HapiTableReader rdr = new HapiTableReader( hdr.getParameters() );
        StarTable meta = rdr.createStarTable( null );
        RowSequence rseq = rdr.createRowSequence( in, byte0, fmt );
        sink.acceptMetadata( meta );
        while ( rseq.next() ) {
            sink.acceptRow( rseq.getRow() );
        }
        sink.endRows();
    }

    /**
     * Attempts to determine whether a stream contains a HAPI table
     * including header.  False positives are quite possible.
     *
     * @return  false if it's definitely not a suitable HAPI stream
     */
    public static boolean isMagic( byte[] buf ) {
        if ( buf[ 0 ] != '#' ) {
            return false;
        }
        for ( int i = 1; i < buf.length; i++ ) {
            switch ( buf[ i ] ) {
                case '\r':
                case '\n':
                case ' ':
                case '#':
                    break;
                case '{':
                    return true;
                default:
                    return false;
            }
        }
        return true;
    }
}
