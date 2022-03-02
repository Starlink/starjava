package uk.ac.starlink.table.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.DataBufferedOutputStream;

/**
 * ColumnStore implementation which uses a mapped buffer and an additional
 * auxiliary file to store a fixed number of variable length data items.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
class IndexedMappedColumnStore implements ColumnStore {

    private final Codec codec_;
    private final File auxFile_;
    private final DataBufferedOutputStream auxOut_;
    private ByteBuffer indexBuf_;
    private long auxOffset_;
    private long nrow_;
    private ByteBuffer[] auxBufs_;

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
        indexBuf_ = bbuf;
        auxFile_ = auxFile;
        auxOut_ =
            new DataBufferedOutputStream( new FileOutputStream( auxFile ) );
    }

    public void acceptCell( Object value ) throws IOException {
        indexBuf_.putLong( auxOffset_ );
        auxOffset_ += codec_.encode( value, auxOut_ );
        nrow_++;
    }

    public void endCells() throws IOException {
        auxOut_.close();
        indexBuf_ = indexBuf_.asReadOnlyBuffer();
        auxBufs_ = FileByteStore.toByteBuffers( auxFile_ );
    }

    public ColumnReader createReader() {
        final ByteBuffer ixBuf = indexBuf_.duplicate();
        ByteStoreAccess auxAccess =
            NioByteStoreAccess
           .createAccess( NioByteStoreAccess.copyBuffers( auxBufs_ ) );
        return new ByteStoreColumnReader( codec_, auxAccess, nrow_ ) {
            public long getAccessOffset( long ix ) {
                return ixBuf.getLong( Tables.checkedLongToInt( 8 * ix ) );
            }
        };
    }
}
