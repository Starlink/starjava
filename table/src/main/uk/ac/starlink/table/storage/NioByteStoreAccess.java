package uk.ac.starlink.table.storage;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Partial implementation of ByteStoreAccess.
 * Not thread-safe.
 *
 * @author   Mark Taylor
 * @since    20 Aug 2010
 */
abstract class NioByteStoreAccess implements ByteStoreAccess {

    /**
     * Returns a buffer with at least the requested number of bytes
     * between the current position and the limit.
     * When <code>nbyte</code> bytes have been read from the returned 
     * buffer, the current position of this ByteStoreAccess will have
     * advanced by <code>nbyte</code> bytes.  The position in the case
     * that this call is made with no corresponding read is undefined,
     * so it's important that the read is actually done (don't call
     * this method speculatively).
     *
     * <p>If no such buffer is available (the end of the storage has 
     * been reached), then an IOException will be thrown.
     *
     * @param  nbyte  number of bytes required
     * @return  buffer from which <code>nbyte</code> bytes can be read
     */
    protected abstract ByteBuffer getBuffer( int nbyte ) throws IOException;

    public byte readByte() throws IOException {
        return getBuffer( 1 ).get();
    }

    public short readShort() throws IOException {
        return getBuffer( 2 ).getShort();
    }

    public char readChar() throws IOException {
        return getBuffer( 2 ).getChar();
    }

    public int readInt() throws IOException {
        return getBuffer( 4 ).getInt();
    }

    public long readLong() throws IOException {
        return getBuffer( 8 ).getLong();
    }

    public float readFloat() throws IOException {
        return getBuffer( 4 ).getFloat();
    }

    public double readDouble() throws IOException {
        return getBuffer( 8 ).getDouble();
    }

    public void readBytes( byte[] b, int offset, int length )
            throws IOException {
        getBuffer( length ).get( b, offset, length );
    }

    /**
     * Returns a reader implementation for an array of ByteBuffers.
     *
     * @param  bbufs  buffer array
     * @return   reader implementation
     */
    public static ByteStoreAccess createAccess( ByteBuffer[] bbufs ) {
        if ( bbufs.length == 1 ) {
            return new SingleNioAccess( bbufs[ 0 ] );
        }
        else {
            return new MultiNioAccess( bbufs );
        }
    }
}
