package uk.ac.starlink.table.storage;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * ByteStoreAccess based on a single ByteBuffer.
 *
 * @author   Mark Taylor
 * @since    20 Aug 2010
 */
class SingleNioAccess extends NioByteStoreAccess {

    private final ByteBuffer bbuf_;

    /**
     * Constructor.
     *
     * @param   bbuf  backing buffer
     */
    public SingleNioAccess( ByteBuffer bbuf ) {
        bbuf_ = bbuf;
    }

    protected ByteBuffer getBuffer( int nbyte ) throws IOException {
        int remaining = bbuf_.remaining();
        if ( remaining >= nbyte ) {
            return bbuf_;
        }
        else if ( remaining == 0 ) {
            throw new EOFException();
        }
        else {
            throw new EOFException( "Requested " + nbyte + " bytes, "
                                  + "only " + remaining + " left" );
        }
    }

    public void seek( long pos ) throws IOException {
        if ( pos >= 0 && pos < bbuf_.limit() ) {
            bbuf_.position( (int) pos );
        }
        else {
            throw new IOException( "Out of range " + pos );
        }
    }

    public void skip( int n ) throws IOException {
        getBuffer( n );
        bbuf_.position( bbuf_.position() + n );
    }
}
