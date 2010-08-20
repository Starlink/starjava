package uk.ac.starlink.table.storage;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Partial implementation of DataInput based on ByteBuffers.
 *
 * As documented below, the <tt>DataInput</tt> implementation is
 * not quite complete; the unimplemented methods are not used by 
 * the classes in this package.
 */
abstract class NioDataInput implements DataInput {

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

    public boolean readBoolean() throws IOException {
        return getBuffer( 1 ).get() != 0;
    }

    public byte readByte() throws IOException {
        return getBuffer( 1 ).get();
    }

    public int readUnsignedByte() throws IOException {
        return getBuffer( 1 ).get() & 0xff;
    }

    public short readShort() throws IOException {
        return getBuffer( 2 ).getShort();
    }

    public int readUnsignedShort() throws IOException {
        return getBuffer( 2 ).getShort() & 0xffff;
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

    public void readFully( byte[] b ) throws IOException {
        getBuffer( b.length ).get( b );
    }

    public void readFully( byte[] b, int offset, int length )
            throws IOException {
        getBuffer( length ).get( b, offset, length );
    }

    /**
     * Not implemented.
     *
     * @throws UnsupportedOperationException
     */
    public String readLine() throws IOException {
        throw new UnsupportedOperationException(
                      "Incomplete DataInput implementation" );
    }

    /**
     * Not implemented.
     *
     * @throws UnsupportedOperationException
     */
    public String readUTF() throws IOException {
        throw new UnsupportedOperationException(
                      "Incomplete DataInput implementation" );
    }
}
