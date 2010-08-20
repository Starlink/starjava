package uk.ac.starlink.table.storage;

import java.io.IOException;

/**
 * Interface for random access reading for data that has been written 
 * into a byte store.
 * This resembles {@link java.io.DataInput}, but omits some of the methods
 * there, and adds {@link #seek} and {@link skipBytes} methods.
 * A pointer is maintained, and is advanced appropriately by the various
 * read methods.
 *
 * @author   Mark Taylor
 * @since    20 Aug 2010
 */
interface ByteStoreAccess {

    /**
     * Reads a byte from the current position.
     *
     * @return  read value
     */
    public byte readByte() throws IOException;

    /**
     * Reads a short from the current position.
     *
     * @return  read value
     */
    public short readShort() throws IOException;

    /**
     * Reads a char from the current position.
     *
     * @return  read value
     */
    public char readChar() throws IOException;

    /**
     * Reads an int from the current position.
     *
     * @return  read value
     */
    public int readInt() throws IOException;

    /**
     * Reads a long from the current position.
     *
     * @return  read value
     */
    public long readLong() throws IOException;

    /**
     * Reads a float from the current position.
     *
     * @return  read value
     */
    public float readFloat() throws IOException;

    /**
     * Reads a double from the current position.
     *
     * @return  read value
     */
    public double readDouble() throws IOException;

    /**
     * Reads bytes into a buffer from the current position.
     *
     * @param   b  buffer to receive bytes
     * @param   off  offset into <code>b</code> for first byte
     * @param   len  number of bytes to read
     */
    public void readBytes( byte[] b, int off, int len ) throws IOException;

    /**
     * Sets the position to the given value.
     *
     * @param  pos  new position
     */
    public void seek( long pos ) throws IOException;

    /**
     * Advances the position by a given number of bytes.
     *
     * @param   len  number of bytes
     */
    public void skip( int len ) throws IOException;
}
