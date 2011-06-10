package uk.ac.starlink.ttools.votlint;

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
        FieldHandler[] fields = getFields();
        int ncol = fields.length;
        ValueParser[] parsers = new ValueParser[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            parsers[ icol ] = fields[ icol ].getParser();
            if ( parsers[ icol ] == null ) {
                warning( "Can't validate stream with unidentified column " +
                         fields[ icol ] );
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
