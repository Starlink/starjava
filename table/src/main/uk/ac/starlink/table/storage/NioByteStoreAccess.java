package uk.ac.starlink.table.storage;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Partial implementation of ByteStoreAccess.
 *
 * @author   Mark Taylor
 * @since    20 Aug 2010
 */
abstract class NioByteStoreAccess implements ByteStoreAccess {

    /**
     * Returns a buffer with at least the requested number of bytes
     * between the current position and the limit.
     * If no such buffer is available (the end has been reached),
     * then an IOException will be thrown.
     *
     * @param  nbyte  number of bytes required
     * @return  buffer with at least <code>nbyte</code> bytes remaining
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
}
