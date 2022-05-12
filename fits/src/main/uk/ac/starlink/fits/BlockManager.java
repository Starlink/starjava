package uk.ac.starlink.fits;

import java.nio.channels.FileChannel;

/**
 * Manages access to a region of a file in blocks using multiple
 * BufferManagers to cover it.
 *
 * @author   Mark Taylor
 * @since    12 May 2022
 */
public class BlockManager {

    private final int nblock_;
    private final int blockSize_;
    private final BufferManager[] bufManagers_;

    /** Default size of a block. */
    public static final int DEFAULT_BLOCKSIZE = 256 * 1024 * 1024;

    /**
     * Constructor.
     *
     * @param  chan  file channel
     * @param  offset   offset into file of start of mapped region
     * @param  size      length of mapped region
     * @param  logName    description of mapped region
     *                    suitable for use in logging messages
     * @param  unmapper  used to unmap buffers, may be null for safety
     * @param  blockSize  size of each block (except the last)
     */
    public BlockManager( FileChannel chan, long offset, long size,
                         String logName, Unmapper unmapper, int blockSize ) {
        blockSize_ = blockSize;
        long blockSizeL = blockSize;
        long nb = ( ( size - 1 ) / blockSizeL ) + 1;
        nblock_ = (int) nb;
        if ( nblock_ != nb ) {
            throw new IllegalArgumentException( "Block count " + nb
                                              + " too high" );
        }
        bufManagers_ = new BufferManager[ nblock_ ];
        for ( int ib = 0; ib < nblock_; ib++ ) {
            long off1 = ib * blockSizeL;
            long leng1 = Math.min( blockSizeL, size - off1 );
            int ileng1 = (int) leng1;
            assert ileng1 == leng1;
            String label1 = "file region " + ( ib + 1 ) + "/" + nblock_
                          + " of " + logName;
            bufManagers_[ ib ] =
                new BufferManager( chan, offset + off1, ileng1, label1,
                                   unmapper );
        }
    }

    /**
     * Returns the number of blocks used by this mananger.
     *
     * @return  block count
     */
    public int getBlockCount() {
        return nblock_;
    }

    /**
     * Returns the size of blocks used by this manager.
     * All blocks are the same size, except (probably) the last one,
     * which may be shorter.
     *
     * @return  block size
     */
    public int getBlockSize() {
        return blockSize_;
    }

    /**
     * Returns the BufferManager for a given block.
     *
     * @param  ib  block index
     * @return  buffer manager
     */
    public BufferManager getBufferManager( int ib ) {
        return bufManagers_[ ib ];
    }

    /**
     * Closes all the BufferManagers maintained by this object.
     * Only use when no buffers will be used any more.
     */
    public void close() {
        for ( BufferManager bufMan : bufManagers_ ) {
            bufMan.close();
        }
    }
}
