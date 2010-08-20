package uk.ac.starlink.table.storage;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * ColumnStore implementation which uses a streamed file to store a 
 * variable number of fixed-length data items.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
class StreamColumnStore implements ColumnStore {

    private final Codec codec_;
    private final File dataFile_;
    private final DataOutputStream dataOut_;
    private final int itemSize_;
    private long nrow_;
    private ByteStoreAccess dataIn_;

    /**
     * Constructor.
     *
     * @param  codec  encoder/decoder for the type of data stored in this
     *                column
     * @param  dataFile  filename used for storing the data;
     *                   it is the caller's responsibility to clear this up
     */
    public StreamColumnStore( Codec codec, File dataFile ) throws IOException {
        codec_ = codec;
        dataFile_ = dataFile;
        itemSize_ = codec.getItemSize();
        if ( itemSize_ < 0 ) {
            throw new IllegalArgumentException( "Must have fixed size codec" );
        }
        dataOut_ = new DataOutputStream(
                       new BufferedOutputStream(
                           new FileOutputStream( dataFile ) ) );
    }

    public void acceptCell( Object value ) throws IOException {
        int nbyte = codec_.encode( value, dataOut_ );
        assert nbyte == itemSize_;
        nrow_++;
    }

    public void endCells() throws IOException {
        dataOut_.close();
        ByteBuffer bbuf = new RandomAccessFile( dataFile_, "r" )
                         .getChannel()
                         .map( FileChannel.MapMode.READ_ONLY, 0,
                               itemSize_ * nrow_ );
        dataIn_ = new SingleNioAccess( bbuf );
    }

    public synchronized Object readCell( long lrow ) throws IOException {
        dataIn_.seek( lrow * itemSize_ );
        return codec_.decode( dataIn_ );
    }

    public void dispose() {
        try {
            dataOut_.close();
        }
        catch ( IOException e ) {
        }
    }
}
