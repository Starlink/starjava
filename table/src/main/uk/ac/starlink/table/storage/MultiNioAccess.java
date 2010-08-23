package uk.ac.starlink.table.storage;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * ByteStoreAccess based on an array of ByteBuffers.
 * The data is viewed as a single block composed of all the bytes from
 * the first buffer, followed by all from the second, etc.
 * It is desirable (more efficient) if all the buffers have the same
 * length apart from the final one, but it's not essential.
 *
 * @author   Mark Taylor
 * @since    23 Aug 2010
 */
class MultiNioAccess extends NioByteStoreAccess {

    private final ByteBuffer[] bbufs_;
    private final int[] bufLengs_;
    private final int isoLeng_;
    private final long totLeng_;
    private int ibuf_;

    /**
     * Constructor.
     *
     * @param   bbufs  backing buffers
     */
    public MultiNioAccess( ByteBuffer[] bbufs ) {
        bbufs_ = bbufs;
        bufLengs_ = new int[ bbufs.length ];
        long tlen = 0;
        boolean isIso = true;
        for ( int ib = 0; ib < bbufs.length; ib++ ) {
            int len = bbufs[ ib ].limit();
            bufLengs_[ ib ] = len;
            tlen += len;
            isIso = isIso && ( ( ib == bbufs.length - 1 ) ||
                               ( len == bufLengs_[ 0 ] ) );
        }
        totLeng_ = tlen;
        isoLeng_ = isIso ? bufLengs_[ 0 ] : -1;
    }

    protected ByteBuffer getBuffer( int nbyte ) throws IOException {

        /* First deal with the common case in which there are enough bytes
         * left in the current buffer; just return the current buffer. */
        ByteBuffer buf = bbufs_[ ibuf_ ];
        int nleft = buf.remaining();
        if ( nleft >= nbyte ) {
            return buf;
        }

        /* Next, the case in which we are at the end of the current buffer,
         * but there are enough bytes in the next buffer; increment the
         * buffer counter, and return the (new) current buffer. */
        else if ( nleft == 0 &&
                  ibuf_ + 1 < bbufs_.length &&
                  nbyte <= bbufs_[ ibuf_ + 1 ].limit() ) {
            bbufs_[ ++ibuf_ ].position( 0 );
            return bbufs_[ ibuf_ ];
        }

        /* Otherwise, we need to straddle more than one buffer to satisfy
         * the request.  This means we have to create a new temporary buffer
         * (presumably a small one), populate it from the contents of
         * this object's buffer array, and return the temporary one. */
        else {
            byte[] tmp = new byte[ nbyte ];
            int pos = 0;
            while ( pos < nbyte ) {
                if ( ! bbufs_[ ibuf_ ].hasRemaining() ) {
                    if ( ibuf_ + 1 >= bbufs_.length ) {
                        throw new EOFException();
                    }
                    else {
                        bbufs_[ ++ibuf_ ].position( 0 );
                    }
                }
                int n = Math.min( nbyte - pos, bbufs_[ ibuf_ ].remaining() );
                bbufs_[ ibuf_ ].get( tmp, pos, n );
                pos += n;
            }
            return ByteBuffer.wrap( tmp );
        }
    }

    public void seek( long pos ) throws IOException {
        if ( pos >= 0 && pos < totLeng_ ) {

            /* In case of equal sized buffers, we can calculate which one
             * to target in one step. */
            if ( isoLeng_ > 0 ) {
                ibuf_ = (int) ( pos / isoLeng_ );
                bbufs_[ ibuf_ ].position( (int) ( pos % isoLeng_ ) );
            }

            /* Otherwise, we have to step through them all to find the
             * one containing the requested destination. */
            else {
                long ip0 = 0;
                for ( int ib = 0; ib < bbufs_.length; ib++ ) {
                    long ip1 = ip0 + bufLengs_[ ib ];
                    if ( pos >= ip0 && pos < ip1 ) {
                        ibuf_ = ib;
                        bbufs_[ ibuf_ ].position( (int) ( pos - ip0 ) );
                        return;
                    }
                    ip0 = ip1;
                }
            }
        }
        else {
            throw new IOException( "Out of range" );
        }
    }

    public void skip( int nskip ) throws IOException {
        for ( int isk = 0; isk < nskip; ) {
            if ( ! bbufs_[ ibuf_ ].hasRemaining() ) {
                bbufs_[ ++ibuf_ ].position( 0 );
            }
            int n = Math.min( nskip - isk, bbufs_[ ibuf_ ].remaining() );
            bbufs_[ ibuf_ ].position( bbufs_[ ibuf_ ].position() + n );
            isk += n;
        }
    }
}
