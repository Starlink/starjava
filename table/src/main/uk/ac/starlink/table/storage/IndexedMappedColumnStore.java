package uk.ac.starlink.table.storage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import uk.ac.starlink.table.Tables;

/**
 * ColumnStore implementation which uses a mapped buffer and an additional
 * auxiliary file to store a fixed number of variable length data items.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
class IndexedMappedColumnStore implements ColumnStore {

    private final Codec codec_;
    private final RandomAccessFile auxRaf_;
    private final LongBuffer indexBuf_;
    private long auxOffset_;
    private long nrow_;
    private ByteStoreAccess auxAccess_;

    /**
     * Constructor.
     *
     * @param   codec  encoder/decoder for the type of data stored in this
     *                 column
     * @param   bbuf   byte buffer used to store <code>long</code> offset
     *                 values; must be big enough to store 8 bytes for
     *                 each cell
     * @param   auxFile  file used to store the data itself; it is the
     *                   caller's responsibility to clear this up
     */
    public IndexedMappedColumnStore( Codec codec, ByteBuffer bbuf,
                                     File auxFile )
            throws IOException {
        codec_ = codec;
        indexBuf_ = bbuf.asLongBuffer();
        auxRaf_ = new RandomAccessFile( auxFile, "rw" );
    }

    public void acceptCell( Object value ) throws IOException {
        indexBuf_.put( auxOffset_ );
        auxOffset_ += codec_.encode( value, auxRaf_ );
        nrow_++;
    }

    public void endCells() throws IOException {
        ByteBuffer auxBuf = auxRaf_.getChannel()
                           .map( FileChannel.MapMode.READ_ONLY, 0, auxOffset_ );
        auxAccess_ = new SingleNioAccess( auxBuf );
        auxRaf_.close();
    }

    public synchronized Object readCell( long lrow ) throws IOException {
        auxAccess_.seek( indexBuf_.get( Tables.checkedLongToInt( lrow ) ) );
        return codec_.decode( auxAccess_ );
    }

    public void dispose() {
        try {
            auxRaf_.close();
        }
        catch ( IOException e ) {
        }
    }
}
