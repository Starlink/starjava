package uk.ac.starlink.ttools.lint;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * Element handler for BINARY elements.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Apr 2005
 */
public class BinaryHandler extends StreamingHandler {

    public void feed( InputStream in ) throws IOException {

        /* Prepare to read the stream. */
        ValueParser[] parsers = getParsers();
        int ncol = parsers.length;
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( parsers[ icol ] == null ) {
                warning( "Can't validate stream with unknown column" );
                throw new IOException( "No stream validation" );
            }
        }

        /* Read the stream. */
        PushbackInputStream pushIn = new PushbackInputStream( in );
        while ( true ) {

            /* Check for end of stream. */
            int b = pushIn.read();
            if ( b < 0 ) {
                return;
            }
            else {
                pushIn.unread( b );
            }

            /* Read a row. */
            for ( int icol = 0; icol < ncol; icol++ ) {
                parsers[ icol ].checkStream( pushIn );
            }

            /* Notify the table. */
            foundRow();
        }
    }
}
