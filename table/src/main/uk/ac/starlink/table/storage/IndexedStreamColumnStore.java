package uk.ac.starlink.table.storage;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import uk.ac.starlink.table.Tables;

/**
 * ColumnStore implementation which uses two streamed files to store a 
 * variable number of variable-length data items.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
class IndexedStreamColumnStore implements ColumnStore {

    private final Codec codec_;
    private final File dataFile_;
    private final File indexFile_;
    private final DataOutputStream dataOut_;
    private final DataOutputStream indexOut_;
    private long dataOffset_;
    private long nrow_;
    private ByteStoreAccess dataIn_;
    private LongBuffer indexIn_;

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
        dataOut_ = new DataOutputStream(
                       new BufferedOutputStream(
                           new FileOutputStream( dataFile ) ) );
        indexOut_ = new DataOutputStream(
                        new BufferedOutputStream(
                            new FileOutputStream( indexFile ) ) );
    }

    public void acceptCell( Object value ) throws IOException {
        indexOut_.writeLong( dataOffset_ );
        dataOffset_ += codec_.encode( value, dataOut_ );
        nrow_++;
    }

    public void endCells() throws IOException {
        dataOut_.close();
        indexOut_.close();
        ByteBuffer dataBuf = new RandomAccessFile( dataFile_, "r" )
                            .getChannel()
                            .map( FileChannel.MapMode.READ_ONLY,
                                  0, dataOffset_ );
        dataIn_ = new SingleNioAccess( dataBuf );
        ByteBuffer indexBuf = new RandomAccessFile( indexFile_, "r" )
                             .getChannel()
                             .map( FileChannel.MapMode.READ_ONLY,
                                   0, 8 * nrow_ );
        indexIn_ = indexBuf.asLongBuffer();
    }

    public synchronized Object readCell( long lrow ) throws IOException {
        dataIn_.seek( indexIn_.get( Tables.checkedLongToInt( lrow ) ) );
        return codec_.decode( dataIn_ );
    }

    public void dispose() {
        try {
            dataOut_.close();
            indexOut_.close();
        }
        catch ( IOException e ) {
        }
    }
}
