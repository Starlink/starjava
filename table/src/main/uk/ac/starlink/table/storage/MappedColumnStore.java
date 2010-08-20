package uk.ac.starlink.table.storage;

import java.io.DataOutput;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * ColumnStore implementation which uses a mapped buffer to store
 * a fixed number of fixed-length data items.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
class MappedColumnStore implements ColumnStore {

    private final Codec codec_;
    private final int itemSize_;
    private final BufferIOAccess access_;

    /**
     * Constructor.
     *
     * @param   codec   encoder/decoder for the type of data stored in this
     *                  column
     * @param   bbuf    byte buffer into which the column data can be written;
     *                  must be big enough for all cells
     */
    public MappedColumnStore( Codec codec, ByteBuffer bbuf ) {
        codec_ = codec;
        itemSize_ = codec.getItemSize();
        access_ = new BufferIOAccess( bbuf );
        if ( itemSize_ < 0 ) {
            throw new IllegalArgumentException( "Must have fixed size codec" );
        }
    }

    public void acceptCell( Object value ) throws IOException {
        int nbyte = codec_.encode( value, access_ );
        assert nbyte == itemSize_;
    }

    public void endCells() {
    }

    public synchronized Object readCell( long lrow ) throws IOException {
        access_.seek( lrow * itemSize_ );
        return codec_.decode( access_ );
    }

    public void dispose() {
    }

    /**
     * ByteBuffer adapter which implements both ByteStoreAccess and DataOut.
     */
    private static class BufferIOAccess extends SingleNioAccess 
                                        implements DataOutput {

        /**
         * Constructor.
         *
         * @param   bbuf  backing buffer
         */
        BufferIOAccess( ByteBuffer bbuf ) {
            super( bbuf );
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

        public void writeBytes( String val ) throws IOException {
            throw new UnsupportedOperationException(
                          "Incomplete DataOutput implementation" );
        }

        public void writeChars( String val ) throws IOException {
            throw new UnsupportedOperationException(
                          "Incomplete DataOutput implementation" );
        }

        public void writeUTF( String val ) throws IOException {
            throw new UnsupportedOperationException(
                          "Incomplete DataOutput implementation" );
        }
    }
}
