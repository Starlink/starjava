package uk.ac.starlink.fits;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Random-access BasicInput implementation that maps a given region of a file
 * as a number of byte buffers.
 * The most recently-used buffer is always kept, but there is a choice of
 * what to do with less-recently used ones.  Concrete subclasses are
 * provided that either discard them automatically or keep them around
 * for a period of time before discarding them.
 * If and when a buffer is discarded, an attempt is made to release resources.
 *
 * <p><strong>Note:</strong> <strong>DO NOT</strong> use an instance
 * of this class from multiple threads - see {@link Unmapper}.
 *
 * @author   Mark Taylor
 * @since    2 Dec 2014
 */
public abstract class BlockMappedInput extends BlockInput {

    private final BlockManager blockManager_;
    private final long blockSize_;
    private final int nblock_;

    /** Default time in milliseconds after which buffers will be discarded. */
    public static final long DEFAULT_EXPIRYMILLIS = 20 * 1000;

    /**
     * Constructor.
     *
     * @param   blockManager  manages file mapping using byte buffers
     */
    protected BlockMappedInput( BlockManager blockManager ) {
        super( blockManager.getBlockCount() );
        blockManager_ = blockManager;
        nblock_ = blockManager.getBlockCount();
        blockSize_ = blockManager.getBlockSize();
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

    /**
     * This does not close the BlockManager.
     */
    public void close() {
        super.close();
    }

    /**
     * Creates a new byte buffer for a given block.
     *
     * @param  iblock  block index
     * @return  new buffer
     */
    ByteBuffer createBuffer( int iblock ) throws IOException {
        return blockManager_.getBufferManager( iblock ).createBuffer();
    }

    /**
     * Disposes of a buffer formerly obtained by {@link #createBuffer}.
     * The buffer must not be used following this call.
     *
     * @param   iblock  block index for which buffer was obtained
     * @param   buf   buffer which will no longer be used
     */
    void disposeBuffer( int iblock, ByteBuffer buf ) {
        if ( iblock >= 0 ) {
            blockManager_.getBufferManager( iblock ).disposeBuffer( buf );
        }
    }

    /**
     * Constructs an instance that does or does not support caching.
     * If caching is requested, recently used block buffers are kept around
     * for a while in case they are needed again.  If not, as soon as
     * a new block is used, any others are discarded.
     *
     * @param   blockManager  manages buffer in blocks
     * @param   caching  whether buffers are cached
     * @return  new instance
     */
    public static BlockMappedInput createInput( BlockManager blockManager,
                                                boolean caching )
            throws IOException {
        return createInput( blockManager, caching ? DEFAULT_EXPIRYMILLIS : 0 );
    }

    /**
     * Constructs an instance with explicit configuration.
     * The <code>expiryMillis</code> parameter controls caching.
     * If zero, the current buffer is discarded as soon
     * as a different one is used.  Otherwise, an attempt is made to
     * discard buffers only after they have been unused for a certain
     * number of milliseconds.
     *
     * @param   blockManager  manages buffer in blocks
     * @param   expiryMillis  buffer caching period in milliseconds
     * @return  new instance
     */
    public static BlockMappedInput createInput( BlockManager blockManager,
                                                long expiryMillis )
            throws IOException {
        return expiryMillis > 0
             ? new CachingBlockMappedInput( blockManager, expiryMillis )
             : new UniqueBlockMappedInput( blockManager );
    }

    /**
     * Instance that only keeps one buffer at a time.
     */
    private static class UniqueBlockMappedInput extends BlockMappedInput {

        private int iblock_;
        private ByteBuffer buffer_;

        /**
         * Constructor.
         *
         * @param   blockManager  manages buffer in blocks
         */
        public UniqueBlockMappedInput( BlockManager blockManager ) {
            super( blockManager );
        }

        protected ByteBuffer acquireBlock( int iblock ) throws IOException {
            int oldIndex = iblock_;
            ByteBuffer oldBuffer = buffer_;
            if ( oldBuffer != null ) {
                disposeBuffer( oldIndex, oldBuffer );
            }
            buffer_ = createBuffer( iblock );
            iblock_ = iblock;
            return buffer_;
        }

        public void close() {
            super.close();
            int oldIndex = iblock_;
            ByteBuffer oldBuffer = buffer_;
            if ( oldBuffer != null ) {
                iblock_ = -1;
                buffer_ = null;
                disposeBuffer( oldIndex, oldBuffer );
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
         * @param   blockManager  manages buffer in blocks
         * @param   expiryMillis  buffer caching period in milliseconds
         */
        public CachingBlockMappedInput( BlockManager blockManager,
                                        long expiryMillis ) {
            super( blockManager );
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
                buf = createBuffer( iblock );
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
                    disposeBuffer( i, buf );
                }
            }
        }
    }
}
