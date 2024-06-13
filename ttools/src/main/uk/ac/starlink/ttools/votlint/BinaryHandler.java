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

    private final boolean isBinary2_;

    public BinaryHandler( boolean isBinary2 ) {
        isBinary2_ = isBinary2;
    }

    public void feed( InputStream in ) throws IOException {

        /* Prepare to read the stream. */
        FieldHandler[] fields = getFields();
        int ncol = fields.length;
        ValueParser[] parsers = new ValueParser[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            parsers[ icol ] = fields[ icol ].getParser();
            if ( parsers[ icol ] == null ) {
                warning( new VotLintCode( "UKP" ),
                         "Can't validate stream with unidentified column " +
                         fields[ icol ] );
                throw new IOException( "No stream validation" );
            }
        }
        VotLintContext context = getContext();

        /* In case of zero columns return without action, otherwise we
         * will keep reading zero bytes per row until the stream is
         * exhausted (never). */
        if ( ncol == 0 ) {
            return;
        }

        /* Determine how many bytes at the start of each row are used to
         * flag null values. */
        int nflag = isBinary2_ ? ( ncol + 7 ) / 8 : 0;

        /* Read the stream. */
        PushbackInputStream pushIn = new PushbackInputStream( in );
        long irow = 0;
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
            if ( nflag > 0 ) {
                ValueParser.slurpStream( pushIn, nflag, context );
            }
            for ( int icol = 0; icol < ncol; icol++ ) {
                parsers[ icol ].checkStream( pushIn, irow );
            }

            /* Notify the table. */
            foundRow();
            irow++;
        }
    }
}
