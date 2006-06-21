package uk.ac.starlink.table.storage;

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
    private final NioDataAccess access_;

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
        access_ = new NioDataAccess( bbuf );
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
}
