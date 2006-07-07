package uk.ac.starlink.votable;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;
import uk.ac.starlink.util.Base64InputStream;

/**
 * RowStepper implementation which reads streamed data in VOTable BINARY
 * format.
 *
 * @author   Mark Taylor (Starlink)
 */
class BinaryRowStepper implements RowStepper {

    final PushbackInputStream pstrm;
    final DataInputStream datain;
    final Decoder[] decoders;
    final int ncol;
    boolean closed;

    /**
     * Constructs a new row reader from a set of decoders and a
     * possibly encoded input stream.  The input stream will be closed
     * by this stepper once the last row has been read from it.
     *
     * @param  n-element array of decoders for decoding n-column data
     * @param  istrm  input stream containing binary data
     * @param  encoding  encoding string as per <tt>encoding</tt> attribute
     *         of STREAM element ("gzip" or "base64", else assumed none)
     */
    public BinaryRowStepper( Decoder[] decoders, InputStream istrm, 
                             String encoding ) throws IOException {
        if ( "gzip".equals( encoding ) ) {
            istrm = new GZIPInputStream( istrm );
        }
        else if ( "base64".equals( encoding ) ) {
            istrm = new Base64InputStream( istrm );
        }
        this.pstrm = new PushbackInputStream( istrm );
        this.datain = new DataInputStream( pstrm );
        this.decoders = decoders;
        ncol = decoders.length;
    }

    /**
     * Constructs a new row reader from a set of decoders and an unencoded
     * input stream.
     *
     * @param  n-element array of decoders for decoding n-column data
     * @param  istrm  input stream containing binary data
     */
    public BinaryRowStepper( Decoder[] decoders, InputStream istrm )
            throws IOException {
        this( decoders, istrm, null );
    }

    public Object[] nextRow() throws IOException {

        /* If the first byte is off the end of the stream, return null. */
        if ( closed ) {
            return null;
        }
        int b = pstrm.read();
        if ( b < 0 ) {
            closed = true;
            pstrm.close();
            return null;
        }
        else {
            pstrm.unread( b );
        }

        /* Otherwise, decode the next row using the decoders. */
        Object[] row = new Object[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            row[ icol ] = decoders[ icol ].decodeStream( datain );
        }

        /* Return the row. */
        return row;
    }
}
