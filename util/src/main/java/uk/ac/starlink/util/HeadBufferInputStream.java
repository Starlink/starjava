package uk.ac.starlink.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Filter input stream that records the first few bytes read from
 * its base stream for later examination.
 *
 * @author   Mark Taylor
 * @since    5 Aug 2015
 */
public class HeadBufferInputStream extends FilterInputStream {

    private final byte[] headBuf_;
    private long pos_;

    /**
     * Constructor.
     *
     * @param  in   base input stream
     * @param  headSize  size of head buffer
     */
    public HeadBufferInputStream( InputStream in, int headSize ) {
        super( in );
        headBuf_ = new byte[ headSize ];
        pos_ = 0;
    }

    /**
     * Returns the actual buffer used for accumulating the first few bytes
     * in the stream.  If the read count is lower than the size of this
     * buffer, not all the buffer has been filled.
     *
     * @return   buffer, same length as size supplied at construction time
     */
    public byte[] getHeadBuffer() {
        return headBuf_;
    }

    /**
     * Returns the total number of bytes so far read from the base stream.
     *
     * @return  byte read count
     */
    public long getReadCount() {
        return pos_;
    }

    @Override
    public int read() throws IOException {
        int value = in.read();
        if ( value >= 0 ) {
            if ( pos_ < headBuf_.length ) {
                headBuf_[ (int) pos_ ] = (byte) value;
            }
            pos_++;
        }
        return value;
    }

    @Override
    public int read( byte[] b ) throws IOException {
        int c = in.read( b );
        if ( c >= 0 ) {
            for ( int i = 0; i + pos_ < headBuf_.length && i < c; i++ ) {
                headBuf_[ i + (int) pos_ ] = b[ i ];
            }
            pos_ += c;
        }
        return c;
    }

    @Override
    public int read( byte[] b, int off, int len ) throws IOException {
        int c = in.read( b, off, len );
        if ( c >= 0 ) {
            for ( int i = 0; i + pos_ < headBuf_.length && i < c; i++ ) {
                headBuf_[ i + (int) pos_ ] = b[ i + off ];
            }
            pos_ += c;
        }
        return c;
    }

    @Override
    public long skip( long n ) throws IOException {
        return pos_ < headBuf_.length
             ? read( new byte[ headBuf_.length - (int) pos_ ] )
             : in.skip( n );
    }

    /**
     * Mark/reset is not supported.
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void reset() throws IOException {
        throw new IOException( "Mark/reset not supported" );
    }

    public void mark( int readlimit ) {
        // no-op
    }
}
