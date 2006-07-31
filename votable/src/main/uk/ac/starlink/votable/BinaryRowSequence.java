package uk.ac.starlink.votable;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.util.Base64InputStream;

/**
 * RowSequence implementation which reads streamed data in VOTable BINARY
 * format.
 *
 * @author   Mark Taylor
 * @since    31 Jul 2006
 */
class BinaryRowSequence implements RowSequence {

    private final PushbackInputStream pIn_;
    private final DataInput dataIn_;
    private final Decoder[] decoders_;
    private final int ncol_;
    private Object[] row_;

    /**
     * Constructs a new row sequence from a set of decoders and a
     * possibly encoded input stream.
     *
     * @param  n-element array of decoders for decoding n-column data
     * @param  in  input stream containing binary data
     * @param  encoding  encoding string as per <tt>encoding</tt> attribute
     *         of STREAM element ("gzip" or "base64", else assumed none)
     */
    public BinaryRowSequence( Decoder[] decoders, InputStream in,
                              String encoding ) throws IOException {
        decoders_ = decoders;
        ncol_ = decoders_.length;
        if ( "gzip".equals( encoding ) ) {
            in = new GZIPInputStream( in );
        }
        else if ( "base64".equals( encoding ) ) {
            in = new Base64InputStream( in );
        }
        pIn_ = new PushbackInputStream( in );
        dataIn_ = new DataInputStream( pIn_ );
    }

    /**
     * Constructs a new row sequence  from a set of decoders and an unencoded
     * input stream.
     *
     * @param  n-element array of decoders for decoding n-column data
     * @param  in  input stream containing binary data
     */
    public BinaryRowSequence( Decoder[] decoders, InputStream in )
            throws IOException {
        this( decoders, in, null );
    }

    public boolean next() throws IOException {
        int b = pIn_.read();
        if ( b < 0 ) {
            return false;
        }
        else {
            pIn_.unread( b );
            Object[] row = new Object[ ncol_ ];
            for ( int icol = 0; icol < ncol_; icol++ ) {
                row[ icol ] = decoders_[ icol ].decodeStream( dataIn_ );
            }
            row_ = row;
            return true;
        }
    }

    public Object[] getRow() {
        if ( row_ != null ) {
            return row_;
        }
        else {
            throw new IllegalStateException( "No next() yet" );
        }
    }

    public Object getCell( int icol ) {
        if ( row_ != null ) {
            return row_[ icol ];
        }
        else {
            throw new IllegalStateException( "No next() yet" );
        }
    }

    public void close() throws IOException {
        pIn_.close();
    }
}
