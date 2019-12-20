package uk.ac.starlink.table.storage;

import java.io.IOException;

/**
 * ColumnReader implementation based on a ByteStoreAccess.
 *
 * @author   Mark Taylor
 * @since    18 Dec 2019
 */
abstract class ByteStoreColumnReader implements ColumnReader {

    private final Codec codec_;
    private final ByteStoreAccess access_;
    private final long nrow_;

    /**
     * Constructor.
     *
     * @param  codec   decoder for value type
     * @param  access   can read byte data
     * @para   nrow    row count
     */
    public ByteStoreColumnReader( Codec codec, ByteStoreAccess access,
                                  long nrow ) {
        codec_ = codec;
        access_ = access;
        nrow_ = nrow;
    }

    /**
     * Gives the byte offset into the ByteStoreAccess from which a given
     * element can be read.
     *
     * @param   ix   element index
     * @return   byte buffer offset
     */
    public abstract long getAccessOffset( long ix ) throws IOException;

    public long getRowCount() {
        return nrow_;
    }

    public Object getObjectValue( long ix ) throws IOException {
        seek( ix );
        return codec_.decodeObject( access_ );
    }

    public double getDoubleValue( long ix ) throws IOException {
        seek( ix );
        return codec_.decodeDouble( access_ );
    }

    public int getIntValue( long ix ) throws IOException {
        seek( ix );
        return codec_.decodeInt( access_ );
    }

    public long getLongValue( long ix ) throws IOException {
        seek( ix );
        return codec_.decodeLong( access_ );
    }

    public boolean getBooleanValue( long ix ) throws IOException {
        seek( ix );
        return codec_.decodeBoolean( access_ );
    }

    /**
     * Positions the ByteStoreAccess ready to read an object from a given
     * offset.
     *
     * @param  ioff  byte offset
     */
    private void seek( long ioff ) throws IOException {
        access_.seek( getAccessOffset( ioff ) );
    }
}
