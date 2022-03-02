package uk.ac.starlink.table.storage;

import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import uk.ac.starlink.util.DataBufferedOutputStream;

/**
 * ColumnStore implementation which uses a streamed file to store a 
 * variable number of fixed-length data items.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
public class StreamColumnStore implements ColumnStore {

    private final Codec codec_;
    private final File dataFile_;
    private final DataBufferedOutputStream dataOut_;
    private final int itemSize_;
    private long nrow_;
    private ByteBuffer[] bbufs_;

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
        dataOut_ =
            new DataBufferedOutputStream( new FileOutputStream( dataFile ) );
    }

    public void acceptCell( Object value ) throws IOException {
        int nbyte = codec_.encode( value, dataOut_ );
        assert nbyte == itemSize_;
        nrow_++;
    }

    public void endCells() throws IOException {
        dataOut_.close();
        bbufs_ = FileByteStore.toByteBuffers( dataFile_ );
    }

    public ColumnReader createReader() {
        ByteStoreAccess access =
            NioByteStoreAccess
           .createAccess( NioByteStoreAccess.copyBuffers( bbufs_ ) );
        return new ByteStoreColumnReader( codec_, access, nrow_ ) {
            public long getAccessOffset( long ix ) {
                return ix * itemSize_;
            }
        };
    }
}
