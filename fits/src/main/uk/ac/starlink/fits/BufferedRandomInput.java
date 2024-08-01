package uk.ac.starlink.fits;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * BasicInput implementation based on a RandomAccessFile.
 * Buffering of the underlying file is performed on the assumption that 
 * access will be largely sequential.
 *
 * @author   Mark Taylor
 * @since    8 Nov 2017
 */
public class BufferedRandomInput implements BasicInput {

    private final RandomAccessFile file_;
    private final long offset0_;
    private final byte[] array_;
    private final int bufsize_;
    private final ByteBuffer buf_;
    private final long filesize_;
    private long bufOffset_;
    public static final int DFLT_BUFSIZE = getDefaultBufferSize();

    /**
     * Constructs a BufferedRandomInput with a default sized buffer.
     *
     * @param  file   file
     * @param  offset0   offset into file of stream start
     */
    public BufferedRandomInput( RandomAccessFile file, long offset0 )
            throws IOException {
        this( file, offset0, DFLT_BUFSIZE );
    }

    /**
     * Constructs a BufferedRandomInput with a buffer of specified size.
     *
     * @param  file   file
     * @param  offset0   offset into file of stream start
     * @param  bufsize   buffer size
     */
    @SuppressWarnings("this-escape")
    public BufferedRandomInput( RandomAccessFile file, long offset0,
                                int bufsize ) throws IOException {
        file_ = file;
        offset0_ = offset0;
        bufsize_ = bufsize;
        filesize_ = file.length();
        array_ = new byte[ bufsize ];
        buf_ = ByteBuffer.wrap( array_ );
        buf_.clear();
        bufOffset_ = offset0_;
        buf_.limit( 0 );
        assert getOffset() == 0;
        assert buf_.remaining() == 0;
    }

    public byte readByte() throws IOException {
        return getAssuredBuffer( 1 ).get();
    }

    public short readShort() throws IOException {
        return getAssuredBuffer( 2 ).getShort();
    }

    public int readInt() throws IOException {
        return getAssuredBuffer( 4 ).getInt();
    }

    public long readLong() throws IOException {
        return getAssuredBuffer( 8 ).getLong();
    }

    public float readFloat() throws IOException {
        return getAssuredBuffer( 4 ).getFloat();
    }

    public double readDouble() throws IOException {
        return getAssuredBuffer( 8 ).getDouble();
    }

    public void readBytes( byte[] bbuf ) throws IOException {
        getAssuredBuffer( bbuf.length ).get( bbuf );
    }

    public boolean isRandom() {
        return true;
    }

    public void skip( long nbytes ) throws IOException {
        seek( getOffset() + nbytes );
    }

    public long getOffset() {
        return bufOffset_ + buf_.position() - offset0_;
    }

    public void seek( long offset ) throws IOException {
        if ( offset < 0 ) {
            throw new IOException( "Negative seek " + offset );
        }
        else if ( offset > filesize_ - offset0_ ) {
            throw new EOFException( "Seek out of range " + offset );
        }
        else {
            long bpos = offset - bufOffset_ + offset0_;
            if ( bpos >= 0 && bpos <= buf_.limit() ) {
                buf_.position( (int) bpos );
            }
            else {
                bufOffset_ = offset + offset0_;
                buf_.limit( 0 );
            }
            assert offset == getOffset();
        }
    }

    public void close() throws IOException {
        file_.close();
    }

    /**
     * Returns a buffer positioned at the current read position
     * guaranteed to be good for reading at least a given number of bytes.
     *
     * @param  nbyte  number of required bytes
     * @return   buffer positioned for read
     */
    private ByteBuffer getAssuredBuffer( int nbyte ) throws IOException {
        if ( buf_.remaining() >= nbyte ) {
            return buf_;
        }
        else {
            long bufoff = bufOffset_ + buf_.position();
            file_.seek( bufoff );
            int c = 0;
            while ( c < nbyte ) {
                int nb = file_.read( array_, c, bufsize_ - c );
                if ( nb < 0 ) {
                    throw new EOFException();
                }
                c += nb;
            }
            bufOffset_ = bufoff;
            buf_.position( 0 );
            buf_.limit( c );
            return buf_;
        }
    }

    /**
     * Returns the default buffer size.
     * Currently returns whatever value BufferedInputStream uses.
     *
     * @return   suitable size for a buffer
     */
    private static int getDefaultBufferSize() {
        final int[] sizer = new int[ 1 ];
        new BufferedInputStream( new ByteArrayInputStream( new byte[ 0 ] ) ) {
            { sizer[ 0 ] = buf.length; }
        };
        return sizer[ 0 ];
    }
}
