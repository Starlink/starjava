package uk.ac.starlink.table.storage;

import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import uk.ac.starlink.util.DataBufferedOutputStream;

/**
 * ColumnStore implementation which uses two streamed files to store a 
 * variable number of variable-length data items.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
public class IndexedStreamColumnStore implements ColumnStore {

    private final Codec codec_;
    private final File dataFile_;
    private final File indexFile_;
    private final DataBufferedOutputStream dataOut_;
    private final DataBufferedOutputStream indexOut_;
    private long dataOffset_;
    private long nrow_;
    private ByteBuffer[] dataBufs_;
    private ByteBuffer[] indexBufs_;

    /**
     * Constructor.
     *
     * @param  codec   encoder/decoder for the type of data stored in this
     *                 column
     * @param  dataFile  filename used for storing the data;
     *                   it is the caller's responsibility to clear this up
     * @param  indexFile filename used for storing offset values;
     *                   it is the caller's responsibility to clear this up
     */
    public IndexedStreamColumnStore( Codec codec, File dataFile,
                                     File indexFile ) throws IOException {
        codec_ = codec;
        dataFile_ = dataFile;
        indexFile_ = indexFile;
        dataOut_ =
            new DataBufferedOutputStream( new FileOutputStream( dataFile ) );
        indexOut_ =
            new DataBufferedOutputStream( new FileOutputStream( indexFile ) );
    }

    public void acceptCell( Object value ) throws IOException {
        indexOut_.writeLong( dataOffset_ );
        dataOffset_ += codec_.encode( value, dataOut_ );
        nrow_++;
    }

    public void endCells() throws IOException {
        dataOut_.close();
        indexOut_.close();
        dataBufs_ = FileByteStore.toByteBuffers( dataFile_ );
        indexBufs_ = FileByteStore.toByteBuffers( indexFile_ );
    }

    public ColumnReader createReader() {
        final ByteStoreAccess dataAccess =
            NioByteStoreAccess
           .createAccess( NioByteStoreAccess.copyBuffers( dataBufs_ ) );
        final ByteStoreAccess indexAccess =
            NioByteStoreAccess
           .createAccess( NioByteStoreAccess.copyBuffers( indexBufs_ ) );
        return new ByteStoreColumnReader( codec_, dataAccess, nrow_ ) {
            public long getAccessOffset( long ix ) throws IOException {
                indexAccess.seek( 8 * ix );
                return indexAccess.readLong();
            }
        };
    }
}
