package uk.ac.starlink.fits;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Partial random-access BasicInput implementation based on
 * a set of ByteBuffers.  These may be obtained eagerly or lazily,
 * as defined by the implementation.
 *
 * @author   Mark Taylor
 * @since    18 Mar 2021
 */
public abstract class BlockInput implements BasicInput {

    private final int nblock_;

    /** Most recently used block index. */
    private int iblock_;

    /** Most recently used block buffer. */
    private ByteBuffer buffer_;

    /**
     * Constructor.
     *
     * @param   nblock  number of buffers
     */
    protected BlockInput( int nblock ) {
        nblock_ = nblock;
        iblock_ = -1;
        buffer_ = ByteBuffer.allocate( 0 );
    }

    public byte readByte() throws IOException {
        try {
            return buffer_.get();
        }
        catch ( BufferUnderflowException e ) {
            return getAssuredBuffer( 1 ).get();
        }
    }

    public short readShort() throws IOException {
        try {
            return buffer_.getShort();
        }
        catch ( BufferUnderflowException e ) {
            return getAssuredBuffer( 2 ).getShort();
        }
    }

    public int readInt() throws IOException {
        try {
            return buffer_.getInt();
        }
        catch ( BufferUnderflowException e ) {
            return getAssuredBuffer( 4 ).getInt();
        }
    }

    public long readLong() throws IOException {
        try {
            return buffer_.getLong();
        }
        catch ( BufferUnderflowException e ) {
            return getAssuredBuffer( 8 ).getLong();
        }
    }

    public float readFloat() throws IOException {
        try {
            return buffer_.getFloat();
        }
        catch ( BufferUnderflowException e ) {
            return getAssuredBuffer( 4 ).getFloat();
        }
    }

    public double readDouble() throws IOException {
        try {
            return buffer_.getDouble();
        }
        catch ( BufferUnderflowException e ) {
            return getAssuredBuffer( 8 ).getDouble();
        }
    }

    public void readBytes( byte[] bbuf ) throws IOException {
        try {
            buffer_.get( bbuf );
        }
        catch ( BufferUnderflowException e ) {
            getAssuredBuffer( bbuf.length ).get( bbuf );
        }
    }

    public void close() {
        iblock_ = -1;
        buffer_ = null;
    }

    public boolean isRandom() {
        return true;
    }

    /**
     * Returns the block location of a given byte offset.
     *
     * <p>This does the opposite of {@link #getBlockOffset getBlockOffset}.
     *
     * @param   offset  offset into this input stream
     * @return  2-element array giving [blockIndex, offsetInBlock]
     */
    public abstract int[] getBlockPos( long offset );

    /**
     * Returns the offset into this stream corresponding to a given
     * block index and offset into that block.
     *
     * <p>This does the opposite of {@link #getBlockPos getBlockPos}.
     *
     * @param  iblock  block index
     * @param  offsetInBlock   offset into that block
     * @return   offset into stream
     */
    public abstract long getBlockOffset( int iblock, int offsetInBlock );

    /**
     * Obtains a buffer corresponding to a named block of the stream.
     * The buffer's position must be zero.
     * This method is called whenever a buffer is required which is not
     * the same as the most recently used one.
     *
     * @param  iblock   block index
     * @return   byte buffer for given block, positioned at start
     */
    protected abstract ByteBuffer acquireBlock( int iblock )
            throws IOException;

    public void seek( long offset ) throws IOException {
        int[] blockPos = getBlockPos( offset );
        int ib = blockPos[ 0 ];
        int ioff = blockPos[ 1 ];

        /* Ensure that the required block is current. */
        if ( ib != iblock_ ) {

            /* In most cases, that means resetting state by acquiring the
             * block with index offset/blocksize as the new current one. */
            if ( ioff > 0 ) {
                setCurrentBlock( ib );
            }
          
            /* However, if the sought position is right at the start
             * of a block, in some circumstances it's desirable to set
             * state to the equivalent case where block offset/blocksize-1
             * is current, and it's positioned at its end. */
            else if ( ioff == 0 ) {

                /* If we can manage it without changing block, do that. */
                if ( ib == iblock_ + 1 ) {
                    ib = iblock_;
                    ioff = buffer_.limit();
                }

                /* Otherwise if it's the last position in the stream,
                 * use the penultimate block. */
                else if ( ib == nblock_ ) {
                    setCurrentBlock( nblock_ - 1 );
                    ib = iblock_;
                    ioff = buffer_.limit();
                }

                /* Otherwise, do the normal thing. */
                else {
                    setCurrentBlock( ib );
                }
            }
            else {
                throw new IllegalArgumentException( ioff + " < 0" );
            }
        }
        assert ib == iblock_;
        assert getBlockOffset( ib, ioff ) == offset
            || ib == -1 && buffer_.limit() == 0 && buffer_.position() == 0;

        /* The current buffer is the one we want.  Set its position. */
        try {
            buffer_.position( ioff );
        }
        catch ( IllegalArgumentException e ) {
            throw (EOFException) new EOFException().initCause( e );
        }
    }

    public long getOffset() {
        if ( iblock_ >= 0 ) {
            return getBlockOffset( iblock_, buffer_.position() );
        }
        else {
            assert iblock_ == -1;
            assert buffer_.limit() == 0 && buffer_.position() == 0;
            return 0;
        }
    }

    public void skip( long nbyte ) throws IOException {
        seek( getOffset() + nbyte );
    }

    /**
     * Returns the number of mapped blocks used.
     *
     * @return  block count
     */
    public int getBlockCount() {
        return nblock_;
    }

    /**
     * Returns a ByteBuffer with content equivalent to that at the
     * current position, but which is guaranteed to contain at least
     * a given number of bytes.
     * This method is not expected to be called frequently,
     * only if a read happens to straddle blocks.  Since blocks are
     * expected to be large, that should constitute a very small fraction
     * of reads.
     *
     * @param  count   required number of bytes to read
     * @return  buffer containing at least count bytes
     */
    private ByteBuffer getAssuredBuffer( int count ) throws IOException {

        /* If the current buffer is at its end, advance to the next one. */
        if ( ! buffer_.hasRemaining() ) {
            setCurrentBlock( iblock_ + 1 );

            /* If that buffer has enough bytes, return that. */
            if ( buffer_.remaining() >= count ) {
                return buffer_;
            }
        }

        /* If we have arrived here, we need to construct a new
         * buffer with content that straddles multiple mapped buffers. */
        byte[] array = new byte[ count ];
        for ( int i = 0; i < count; ) {
            if ( ! buffer_.hasRemaining() ) {
                setCurrentBlock( iblock_ + 1 );
            }
            int nr = Math.min( count - i, buffer_.remaining() );
            buffer_.get( array, i, nr );
            i += nr;
        }
        return ByteBuffer.wrap( array );
    }

    /**
     * Sets the current block, updating the current buffer and block index.
     *
     * @param  iblock  block index
     */
    private void setCurrentBlock( int iblock ) throws IOException {
        if ( iblock < nblock_ ) {
            ByteBuffer buf = acquireBlock( iblock );
            assert buf.position() == 0;
            buffer_ = buf;
            iblock_ = iblock;
        }
        else {
            throw new EOFException();
        }
    }
}
