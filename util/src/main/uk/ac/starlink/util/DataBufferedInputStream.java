package uk.ac.starlink.util;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream which provides buffering and an efficient DataInput
 * implementation.
 * It can be much more efficient than layering a DataInputStream on top of
 * a BufferedInputStream.
 *
 * @author   Mark Taylor
 * @since    1 Mar 2022
 */
public class DataBufferedInputStream extends FilterInputStream
                                     implements DataInput {

    private final byte[] buf_;
    private int count_;
    private int pos_;

    /** Default buffer size {@value}, same as java.io.BufferedInputStream. */
    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

    /**
     * Constructs an instance with a default buffer size.
     *
     * @param  in  base input stream
     */
    public DataBufferedInputStream( InputStream in ) {
        this( in, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Constructs an instance with a given buffer size.
     *
     * @param  in  base input stream
     * @param  size  buffer length in bytes
     */
    public DataBufferedInputStream( InputStream in, int size ) {
        super( in );
        buf_ = new byte[ size ];
    }

    @Override
    public int read() throws IOException {
        if ( checkBuf( 1 ) ) {
            return buf_[ pos_++ ] & 0xff;
        }
        else {
            return -1;
        }
    }

    @Override
    public int read( byte[] b, int off, int len ) throws IOException {
        if ( checkBuf( len ) ) {
            System.arraycopy( buf_, pos_, b, off, len );
            pos_ += len;
            return len;
        }
        else if ( count_ > pos_ ) {
            int has = count_ - pos_;
            System.arraycopy( buf_, pos_, b, off, has );
            pos_ = 0;
            count_ = 0;
            return has;
        }
        else {
            pos_ = 0;
            count_ = 0;
            return in.read( b, off, len );
        }
    }

    @Override
    public int read( byte[] b ) throws IOException {
        return read( b, 0, b.length );
    }

    @Override
    public long skip( long n ) throws IOException {
        int has = count_ - pos_;
        if ( has > n ) {
            pos_ += n;
            return n;
        }
        else if ( has > 0 ) {
            pos_ = 0;
            count_ = 0;
            return has;
        }
        else {
            pos_ = 0;
            count_ = 0;
            return in.skip( n );
        }
    }

    @Override
    public int available() throws IOException {
        return count_ - pos_ + in.available();
    }

    /**
     * Returns false;
     */
    @Override
    public boolean markSupported() {
        return false;  // should be possible, but too fiddly
    }

    @Override
    public void mark( int limit ) {
    }

    @Override
    public void reset() throws IOException {
        throw new IOException( "Mark not supported" );
    }

    public boolean readBoolean() throws IOException {
        requireBuf( 1 );
        return buf_[ pos_++ ] != 0;
    }

    public byte readByte() throws IOException {
        requireBuf( 1 );
        return buf_[ pos_++ ];
    }

    // Note: the bit wrangling expressions here are copied from the
    // java.io.DataInput javadocs.

    public int readUnsignedByte() throws IOException {
        requireBuf( 1 );
        return buf_[ pos_++ ] & 0xff;
    }

    public short readShort() throws IOException {
        requireBuf( 2 );
        return (short) ( ( buf_[ pos_++ ] << 8 )
                       | ( buf_[ pos_++ ] & 0xff ) );
    }

    public int readUnsignedShort() throws IOException {
        requireBuf( 2 );
        return ( ( buf_[ pos_++ ] & 0xff ) << 8 )
             | ( buf_[ pos_++ ] & 0xff );
    }

    public char readChar() throws IOException {
        requireBuf( 2 );
        return (char) ( ( buf_[ pos_++ ] << 8 )
                      | ( buf_[ pos_++ ] & 0xff ) );
    }

    public int readInt() throws IOException {
        requireBuf( 4 );
        return ( ( buf_[ pos_++ ] & 0xff ) << 24 )
             | ( ( buf_[ pos_++ ] & 0xff ) << 16 )
             | ( ( buf_[ pos_++ ] & 0xff ) <<  8 )
             | ( ( buf_[ pos_++ ] & 0xff )       );
    }

    public long readLong() throws IOException {
        requireBuf( 8 );
        return ( (long) ( buf_[ pos_++ ] & 0xff ) << 56 )
             | ( (long) ( buf_[ pos_++ ] & 0xff ) << 48 )
             | ( (long) ( buf_[ pos_++ ] & 0xff ) << 40 )
             | ( (long) ( buf_[ pos_++ ] & 0xff ) << 32 )
             | ( (long) ( buf_[ pos_++ ] & 0xff ) << 24 )
             | ( (long) ( buf_[ pos_++ ] & 0xff ) << 16 )
             | ( (long) ( buf_[ pos_++ ] & 0xff ) <<  8 )
             | ( (long) ( buf_[ pos_++ ] & 0xff )       );
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat( readInt() );
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble( readLong() );
    }

    public void readFully( byte[] b ) throws IOException {
        readFully( b, 0, b.length );
    }

    public void readFully( byte[] b, int off, int len ) throws IOException {
        if ( checkBuf( len ) ) {
            System.arraycopy( buf_, pos_, b, off, len );
            pos_ += len;
        }
        else {
            int has = count_ - pos_;
            assert len > has;
            System.arraycopy( buf_, pos_, b, off, has );
            pos_ = 0;
            count_ = 0;
            len -= has;
            off += has;
            while ( len > 0 ) {
                int got = in.read( b, off, len );
                if ( got < 0 ) {
                    throw new EOFException();
                }
                len -= got;
                off += got;
            }
        }
    }

    /**
     * This method is deprecated and unsupported.
     *
     * @throws   UnsupportedOperationException  always
     */
    @Deprecated
    public String readLine() throws IOException {
        throw new UnsupportedOperationException();
    }

    public String readUTF() throws IOException {
        return DataInputStream.readUTF( this );
    }

    public int skipBytes( int n ) throws IOException {
        return (int) skip( n );
    }

    /**
     * Ensures that the buffer contains at least a given number of bytes
     * ready to read.
     *
     * @param  need  number of bytes required, must be smaller than
     *               the buffer size
     * @throws  EOFException  if the underlying stream does not contain
     *                        enough bytes
     */
    private void requireBuf( int need ) throws IOException {
        assert need <= buf_.length;
        if ( ! checkBuf( need ) ) {
            throw new EOFException();
        }
    }

    /**
     * Tries to ensure that the buffer contains a given number of bytes
     * ready to read.
     * The return status indicates whether it has succeeded.
     * If the requested byte count <code>need</code> is small
     * (less than the buffer size), it will only fail if the underlying
     * input stream has ended.  If the requested byte count is large
     * (greater than or equal to the buffer size), a failure may or may not
     * mean that the input stream has ended.
     *
     * @param  need  number of bytes requested
     * @return  true if <code>need</code> bytes are now in the buffer
     *          ready to read
     */
    private boolean checkBuf( int need ) throws IOException {
        int has = count_ - pos_;
        if ( has >= need ) {
            return true;
        }
        else {
            System.arraycopy( buf_, pos_, buf_, 0, has );
            pos_ = 0;
            count_ = has;
            while ( count_ - pos_ < need ) {
                int got = in.read( buf_, count_, buf_.length - count_ );
                if ( got <= 0 ) {
                    return false;
                }
                count_ += got;
            }
            return true;
        }
    }
}
