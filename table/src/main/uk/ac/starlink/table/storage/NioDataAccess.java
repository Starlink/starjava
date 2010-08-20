package uk.ac.starlink.table.storage;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import uk.ac.starlink.table.Tables;

/**
 * Adapts a {@link java.nio.ByteBuffer} to look like a 
 * {@link java.io.DataInput}.
 * As documented below, the <tt>DataInput</tt> and <tt>DataOut</tt> 
 * implementations are not quite complete; the unimplemented methods 
 * are not used by the classes in the package for which this adapter
 * class was written.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Aug 2004
 */
class NioDataAccess extends NioDataInput
                    implements SeekableDataInput, DataOutput {

    private final ByteBuffer bbuf_;

    /**
     * Constructs a new DataAccess based on a byte buffer.
     *
     * @param  bbuf  backing buffer
     */
    public NioDataAccess( ByteBuffer bbuf ) {
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

    public int skipBytes( int n ) {
        int nb = Math.min( Math.max( n, 0 ), bbuf_.remaining() );
        bbuf_.position( bbuf_.position() + nb );
        return nb;
    }

    public void write( int b ) throws IOException {
        getBuffer( 1 ).put( (byte) b );
    }

    public void write( byte[] b, int off, int len ) throws IOException {
        getBuffer( len ).put( b, off, len );
    }

    public void write( byte[] b ) throws IOException {
        getBuffer( b.length ).put( b );
    }

    public void writeBoolean( boolean val ) throws IOException {
        getBuffer( 1 ).put( val ? (byte) 1 : (byte) 0 );
    }

    public void writeByte( int val ) throws IOException {
        getBuffer( 1 ).put( (byte) val );
    }

    public void writeChar( int val ) throws IOException {
        getBuffer( 2 ).putChar( (char) val );
    }

    public void writeShort( int val ) throws IOException {
        getBuffer( 2 ).putShort( (short) val );
    }

    public void writeInt( int val ) throws IOException {
        getBuffer( 4 ).putInt( val );
    }

    public void writeLong( long val ) throws IOException {
        getBuffer( 8 ).putLong( val );
    }

    public void writeFloat( float val ) throws IOException {
        getBuffer( 4 ).putFloat( val );
    }

    public void writeDouble( double val ) throws IOException {
        getBuffer( 8 ).putDouble( val );
    }

    /**
     * Not implemented.
     *
     * @throws UnsupportedOperationException
     */
    public void writeBytes( String val ) throws IOException {
        throw new UnsupportedOperationException( 
                      "Incomplete DataInput implementation" );
    }

    /**
     * Not implemented.
     *
     * @throws UnsupportedOperationException
     */
    public void writeChars( String val ) throws IOException {
        throw new UnsupportedOperationException( 
                      "Incomplete DataInput implementation" );
    }

    /**
     * Not implemented.
     *
     * @throws UnsupportedOperationException
     */
    public void writeUTF( String val ) throws IOException {
        throw new UnsupportedOperationException( 
                      "Incomplete DataInput implementation" );
    }
}
