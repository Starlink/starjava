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
public abstract class BlockMappedInput extends BlockInput {

    private final FileChannel channel_;
    private final long pos_;
    private final long size_;
    private final String logName_;
    private final long blockSize_;
    private final int nblock_;
    private final Unmapper unmapper_;

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
        super( (int) ( ( size - 1 ) / blockSize ) + 1 );
        long nb = ( ( size - 1 ) / blockSize ) + 1;
        channel_ = channel;
        pos_ = pos;
        size_ = size;
        logName_ = logName;
        blockSize_ = blockSize;
        nblock_ = getBlockCount();
        if ( nblock_ != nb ) {
            throw new IllegalArgumentException( "Block count " + nb
                                              + " too high" );
        }
        logger_.info( logName_ + " mapping as " + nblock_ + " blocks of "
                    + blockSize_ + " bytes" );
        unmapper_ = Unmapper.getInstance();
    }

    public int[] getBlockPos( long offset ) {
        return new int[] {
            (int) ( offset / blockSize_ ),
            (int) ( offset % blockSize_ ),
        };
    }

    public long getBlockOffset( int iblock, int offsetInBlock ) {
        return iblock * blockSize_ + offsetInBlock;
    }

    public void close() {
        super.close();
    }

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
    void unmapBlock( int iblock, ByteBuffer buf ) {
        if ( iblock >= 0 ) {
            assert buf instanceof MappedByteBuffer;
            if ( buf instanceof MappedByteBuffer ) {
                boolean unmapped = unmapper_.unmap( (MappedByteBuffer) buf );
                logger_.config( "Expiring cached buffer "
                              + ( iblock + 1 ) + "/" + nblock_
                              + " of " + logName_
                              + ( unmapped ? " (unmapped)"
                                           : " (not unmapped)" ) );
            }
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

        private int iblock_;
        private MappedByteBuffer buffer_;

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

        protected ByteBuffer acquireBlock( int iblock ) throws IOException {
            int oldIndex = iblock_;
            ByteBuffer oldBuffer = buffer_;
            if ( oldBuffer != null ) {
                unmapBlock( oldIndex, oldBuffer );
            }
            buffer_ = mapBlock( iblock );
            iblock_ = iblock;
            return buffer_;
        }

        public void close() {
            super.close();
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
        private final ByteBuffer[] bufs_;
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
            bufs_ = new ByteBuffer[ nblock_ ];
            useEpochs_ = new long[ nblock_ ];
            lastTidy_ = System.currentTimeMillis();
        }

        protected ByteBuffer acquireBlock( int iblock ) throws IOException {
            ByteBuffer buf = bufs_[ iblock ];
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
            super.close();
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
                ByteBuffer buf = bufs_[ i ];
                long useEpoch = useEpochs_[ i ];
                if ( buf != null && useEpoch < lastOkUse ) {
                    bufs_[ i ] = null;
                    unmapBlock( i, buf );
                }
            }
        }
    }
}
