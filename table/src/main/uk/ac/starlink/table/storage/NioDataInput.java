package uk.ac.starlink.table.storage;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import uk.ac.starlink.table.Tables;

/**
 * Adapts a {@link java.nio.ByteBuffer} to look like a 
 * {@link java.io.DataInput}.
 * As documented below, the <tt>DataInput</tt> implmentation 
 * is not quite complete; the unimplemented methods are not used
 * by the classes in this package for which this adapter class was
 * written.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Aug 2004
 */
class NioDataInput implements SeekableDataInput {

    private final ByteBuffer bbuf_;

    /**
     * Constructs a new DataInput based on a byte buffer.
     *
     * @param  bbuf  backing buffer
     */
    public NioDataInput( ByteBuffer bbuf ) {
        bbuf_ = bbuf;
    }

    public void seek( long pos ) throws IOException {
        try {
            bbuf_.position( Tables.checkedLongToInt( pos ) );
        }
        catch ( IllegalArgumentException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    public boolean readBoolean() throws EOFException {
        try {
            return bbuf_.get() != 0;
        }
        catch ( BufferUnderflowException e ) {
            throw new EOFException( "Buffer underflow" );
        }
    }

    public byte readByte() throws EOFException {
        try {
            return bbuf_.get();
        }
        catch ( BufferUnderflowException e ) {
            throw new EOFException( "Buffer underflow" );
        }
    }

    public int readUnsignedByte() throws EOFException {
        try {
            return bbuf_.get() & 0xff;
        }
        catch ( BufferUnderflowException e ) {
            throw new EOFException( "Buffer underflow" );
        }
    }

    public short readShort() throws EOFException {
        try {
            return bbuf_.getShort();
        }
        catch ( BufferUnderflowException e ) {
            throw new EOFException( "Buffer underflow" );
        }
    }

    public int readUnsignedShort() throws EOFException {
        try {
            return bbuf_.getShort() & 0xffff;
        }
        catch ( BufferUnderflowException e ) {
            throw new EOFException( "Buffer underflow" );
        }
    }

    public char readChar() throws EOFException {
        try {
            return bbuf_.getChar();
        }
        catch ( BufferUnderflowException e ) {
            throw new EOFException( "Buffer underflow" );
        }
    }

    public int readInt() throws EOFException {
        try {
            return bbuf_.getInt();
        }
        catch ( BufferUnderflowException e ) {
            throw new EOFException( "Buffer underflow" );
        }
    }

    public long readLong() throws EOFException {
        try {
            return bbuf_.getLong();
        }
        catch ( BufferUnderflowException e ) {
            throw new EOFException( "Buffer underflow" );
        }
    }

    public float readFloat() throws EOFException {
        try {
            return bbuf_.getFloat();
        }
        catch ( BufferUnderflowException e ) {
            throw new EOFException( "Buffer underflow" );
        }
    }

    public double readDouble() throws EOFException {
        try {
            return bbuf_.getDouble();
        }
        catch ( BufferUnderflowException e ) {
            throw new EOFException( "Buffer underflow" );
        }
    }

    public void readFully( byte[] b ) throws EOFException {
        try {
            bbuf_.get( b );
        }
        catch ( BufferUnderflowException e ) {
            throw new EOFException();
        }
    }

    public void readFully( byte[] b, int offset, int length )
            throws EOFException {
        try {
            bbuf_.get( b, offset, length );
        }
        catch ( BufferUnderflowException e ) {
            throw new EOFException();
        }
    }

    public int skipBytes( int n ) {
        try {
            bbuf_.position( bbuf_.position() + n );
            return n;
        }
        catch ( IllegalArgumentException e ) {
            return 0;
        }
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
