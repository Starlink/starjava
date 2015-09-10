package uk.ac.starlink.fits;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

/**
 * Random-access BasicInput implementation that maps a given region of a file
 * as a number of byte buffers.
 * The most recently-used buffer is always kept, but there is a choice of
 * what to do with less-recently used ones.  Concrete subclasses are
 * provided that either discard them automatically or keep them around
 * for a period of time before discarding them.
 * If and when a buffer is discarded, an attempt is made to unmap it.
 *
 * <p><strong>Note:</strong> <strong>DO NOT</strong> use an instance
 * of this class from multiple threads - see {@link Unmapper}.
 *
 * @author   Mark Taylor
 * @since    2 Dec 2014
 */
public abstract class BlockMappedInput implements BasicInput {

    private final FileChannel channel_;
    private final long pos_;
    private final long size_;
    private final String logName_;
    private final long blockSize_;
    private final int nblock_;
    private final Unmapper unmapper_;

    /** Most recently used block index: do not use outside this class! */
    int iblock_;

    /** Most recently used block buffer: DO NOT use outside this class! */
    MappedByteBuffer buffer_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /** Default maximum size in bytes for mapped blocks. */
    public static final int DEFAULT_BLOCKSIZE = 256 * 1024 * 1024;

    /** Default time in milliseconds after which buffers will be discarded. */
    public static final long DEFAULT_EXPIRYMILLIS = 20 * 1000;

    /**
     * Constructor.
     *
     * @param   channel  file channel, preferably read-only
     * @param   pos   offset into file of stream start
     * @param   size  number of bytes in stream
     * @param   logName  name for mapped region used in logging messages
     * @param   blockSize  (maximum) number of bytes per mapped block
     */
    protected BlockMappedInput( FileChannel channel, long pos, long size,
                                String logName, int blockSize )
            throws IOException {
        channel_ = channel;
        pos_ = pos;
        size_ = size;
        logName_ = logName;
        blockSize_ = blockSize;
        long nb = ( ( size - 1 ) / blockSize_ ) + 1;
        nblock_ = (int) nb;
        if ( nblock_ != nb ) {
            throw new IllegalArgumentException( "Block count " + nb
                                              + " too high" );
        }
        logger_.info( logName_ + " mapping as " + nblock_ + " blocks of "
                    + blockSize_ + " bytes" );
        iblock_ = -1;
        buffer_ = channel.map( FileChannel.MapMode.READ_ONLY, pos, 0 );
        unmapper_ = Unmapper.getInstance();
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

    public boolean isRandom() {
        return true;
    }

    public void seek( long offset ) throws IOException {
        int ib = (int) ( offset / blockSize_ );
        int ioff = (int) ( offset % blockSize_ );

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
        assert ib * blockSize_ + ioff == offset
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
            return iblock_ * blockSize_ + buffer_.position();
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
     * Obtains a buffer corresponding to a named block of the stream.
     * The buffer's position must be zero.
     * This method is called whenever a buffer is required which is not
     * the same as the most recently used one.
     *
     * @param  iblock   block index
     * @return   byte buffer for given block, positioned at start
     */
    protected abstract MappedByteBuffer acquireBlock( int iblock )
            throws IOException;

    /**
     * Performs the actual file mapping to create a new mapped buffer.
     *
     * @param  iblock  block index
     * @return  newly mapped buffer
     */
    MappedByteBuffer mapBlock( int iblock ) throws IOException {
        long offset = iblock * blockSize_;
        long leng = Math.min( blockSize_, size_ - offset );
        logger_.config( "Mapping file region " + ( iblock + 1 ) + "/"
                      + nblock_ );
        return channel_.map( FileChannel.MapMode.READ_ONLY,
                             pos_ + offset, leng );
    }

    /**
     * Unmaps a block.  The buffer must not be used following this call.
     *
     * @param   iblock  block index
     * @param   buf  mapped buffer - must not be used subsequently
     */
    void unmapBlock( int iblock, MappedByteBuffer buf ) {
        if ( iblock >= 0 ) {
            boolean unmapped = unmapper_.unmap( buf );
            logger_.config( "Expiring cached buffer "
                          + ( iblock + 1 ) + "/" + nblock_
                          + " of " + logName_
                          + ( unmapped ? " (unmapped)"
                                       : " (not unmapped)" ) );
        }
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
            MappedByteBuffer buf = acquireBlock( iblock );
            assert buf.position() == 0;
            buffer_ = buf;
            iblock_ = iblock;
        }
        else {
            throw new EOFException();
        }
    }

    /**
     * Constructs an instance that does or does not support caching.
     * If caching is requested, recently used block buffers are kept around
     * for a while in case they are needed again.  If not, as soon as
     * a new block is used, any others are discarded.
     * A default block size is used.
     *
     * @param   channel  file channel, preferably read-only
     * @param   pos   offset into file of stream start
     * @param   size  number of bytes in stream
     * @param   logName  name for mapped region used in logging messages
     * @param   caching  whether buffers are cached
     * @return  new instance
     */
    public static BlockMappedInput createInput( FileChannel channel, long pos,
                                                long size, String logName,
                                                boolean caching )
            throws IOException {
        return createInput( channel, pos, size, logName, DEFAULT_BLOCKSIZE,
                            caching ? DEFAULT_EXPIRYMILLIS : 0 );
    }

    /**
     * Constructs an instance with explicit configuration.
     * The <code>expiryMillis</code> parameter controls caching.
     * If zero, the current buffer is discarded an unmapped as soon
     * as a different one is used.  Otherwise, an attempt is made to
     * discard buffers only after they have been unused for a certain
     * number of milliseconds.
     *
     * @param   channel  file channel, preferably read-only
     * @param   pos   offset into file of stream start
     * @param   size  number of bytes in stream
     * @param   logName  name for mapped region used in logging messages
     * @param   blockSize   maximum number of bytes per block
     * @param   expiryMillis  buffer caching period in milliseconds
     * @return  new instance
     */
    public static BlockMappedInput createInput( FileChannel channel, long pos,
                                                long size, String logName,
                                                int blockSize,
                                                long expiryMillis )
            throws IOException {
        return expiryMillis > 0
             ? new CachingBlockMappedInput( channel, pos, size, logName,
                                            blockSize, expiryMillis )
             : new UniqueBlockMappedInput( channel, pos, size, logName,
                                           blockSize );
    }

    /**
     * Instance that only keeps one buffer at a time.
     */
    private static class UniqueBlockMappedInput extends BlockMappedInput {

        /**
         * Constructor.
         *
         * @param   channel  file channel, preferably read-only
         * @param   pos   offset into file of stream start
         * @param   size  number of bytes in stream
         * @param   logName  name for mapped region used in logging messages
         * @param   blockSize   maximum number of bytes per block
         */
        public UniqueBlockMappedInput( FileChannel channel, long pos,
                                       long size, String logName,
                                       int blockSize ) throws IOException {
            super( channel, pos, size, logName, blockSize );
        }

        protected MappedByteBuffer acquireBlock( int iblock )
                throws IOException {
            int oldIndex = iblock_;
            MappedByteBuffer oldBuffer = buffer_;
            if ( oldBuffer != null ) {
                unmapBlock( oldIndex, oldBuffer );
            }
            return mapBlock( iblock );
        }

        public void close() {
            int oldIndex = iblock_;
            MappedByteBuffer oldBuffer = buffer_;
            if ( oldBuffer != null ) {
                iblock_ = -1;
                buffer_ = null;
                unmapBlock( oldIndex, oldBuffer );
            }
        }
    }

    /**
     * Instance that keeps buffers until a time period has expired.
     */
    private static class CachingBlockMappedInput extends BlockMappedInput {

        private final int nblock_;
        private final long expiryMillis_;
        private final long tidyMillis_;
        private final MappedByteBuffer[] bufs_;
        private final long[] useEpochs_;
        private long lastTidy_;

        /**
         * Constructor.
         *
         * @param   channel  file channel, preferably read-only
         * @param   pos   offset into file of stream start
         * @param   size  number of bytes in stream
         * @param   logName  name for mapped region used in logging messages
         * @param   blockSize   maximum number of bytes per block
         * @param   expiryMillis  buffer caching period in milliseconds
         */
        public CachingBlockMappedInput( FileChannel channel, long pos,
                                        long size, String logName,
                                        int blockSize, long expiryMillis )
                throws IOException {
            super( channel, pos, size, logName, blockSize );
            expiryMillis_ = expiryMillis;
            tidyMillis_ = expiryMillis / 4;
            nblock_ = getBlockCount();
            bufs_ = new MappedByteBuffer[ nblock_ ];
            useEpochs_ = new long[ nblock_ ];
            lastTidy_ = System.currentTimeMillis();
        }

        protected MappedByteBuffer acquireBlock( int iblock )
                throws IOException {
            MappedByteBuffer buf = bufs_[ iblock ];
            if ( buf == null ) {
                buf = mapBlock( iblock );
                long now = System.currentTimeMillis();
                if ( now - lastTidy_ > tidyMillis_ ) {
                    tidyCache( now - expiryMillis_ );
                    lastTidy_ = now;
                }
                bufs_[ iblock ] = buf;
                useEpochs_[ iblock ] = now;
            }
            else {
                buf.position( 0 );
            }
            return buf;
        }

        public void close() {
            tidyCache( Long.MAX_VALUE );
        }

        /**
         * Attempts to unmap buffers that have not been used more recently
         * than a given epoch.
         *
         * @param   lastOkUse  latest usage epoch at which a buffer will
         *                     not be discarded
         */
        private void tidyCache( long lastOkUse ) {
            for ( int i = 0; i < nblock_; i++ ) {
                MappedByteBuffer buf = bufs_[ i ];
                long useEpoch = useEpochs_[ i ];
                if ( buf != null && useEpoch < lastOkUse ) {
                    bufs_[ i ] = null;
                    unmapBlock( i, buf );
                }
            }
        }
    }
}
