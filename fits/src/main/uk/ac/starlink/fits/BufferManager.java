package uk.ac.starlink.fits;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages access to a region of a file using monolithic byte buffers.
 * The region is only mapped once, and subsequent buffer requests
 * are handled by duplicating the original mapped buffer.
 *
 * <p>Buffers are acquired using {@link #createBuffer}, and should be
 * disposed of using {@link #disposeBuffer} when no longer in use.
 * When none of the buffers are in use any more, {@link #close} should
 * be called.
 *
 * <p><strong>Note:</strong> the mapped buffer is unmapped using
 * the supplied {@link Unmapper} when all created buffers have been disposed,
 * or when close() is called.
 * If any of the created buffers is used after this has happened,
 * and unmapping is actually attempted, very bad consequences may ensue.
 * Therefore this class, or at least its {@link #disposeBuffer} and
 * {@link #close} methods, should be used with extreme caution,
 * or a null Unmapper should be supplied.
 *
 * @author   Mark Taylor
 * @since    12 May 2022
 */
public class BufferManager {

    private final FileChannel channel_;
    private final long offset_;
    private final int leng_;
    private final String logLabel_;
    private final Unmapper unmapper_;
    private final Map<ByteBuffer,Void> dupBuffers_;
    private MappedByteBuffer buffer0_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     *
     * @param  channel  file channel
     * @param  offset   offset into file of start of mapped region
     * @param  leng     length of mapped region
     * @param  logLabel   description of mapped region
     *                    suitable for use in logging messages
     * @param  unmapper  used to unmap buffers, may be null for safety
     */
    public BufferManager( FileChannel channel, long offset, int leng,
                          String logLabel, Unmapper unmapper ) {
        channel_ = channel;
        offset_ = offset;
        leng_ = leng;
        logLabel_ = logLabel;
        unmapper_ = unmapper;

        /* Note ByteBuffer equality methods depend on their current state
         * (mark, position etc) which may change during their lifetimes,
         * so we need to use an IdentityHashMap not an normal Map to keep
         * track of them between being issued and being disposed. */
        dupBuffers_ = new IdentityHashMap<ByteBuffer,Void>();
    }

    /**
     * Returns a new read-only ByteBuffer providing access to the
     * file region.
     * The returned object should if possible be passed to
     * {@link #disposeBuffer} after it is known that it will
     * no longer be used.
     *
     * @return  buffer
     */
    public ByteBuffer createBuffer() throws IOException {
        ByteBuffer buf = getMappedBuffer().duplicate();
        assert buf.position() == 0;
        assert buf.isDirect();
        synchronized ( this ) {
            dupBuffers_.put( buf, null );
        }
        return buf;
    }

    /**
     * Asserts that the supplied buffer, originally acquired from
     * {@link #createBuffer}, will no longer be used.
     * Resources may be reclaimed.
     *
     * @param  buf  previously created buffer
     */
    public void disposeBuffer( ByteBuffer buf ) {
        final boolean removed;
        synchronized ( this ) {
            removed = dupBuffers_.keySet().remove( buf );
            if ( dupBuffers_.isEmpty() ) {
                unmapBuffer();
            }
        }
        if ( ! removed ) {
            logger_.warning( "Attempting to dispose unknown buffer "
                           + logLabel_ );
        }
    }

    /**
     * Asserts that neither this manager, nor any of the buffers it has
     * supplied, will be used again.
     */
    public void close() {
        unmapBuffer();
    }

    /**
     * Returns the single buffer mapping the file region.
     * This buffer is lazily acquired.
     *
     * @return   mapped buffer
     */
    private synchronized MappedByteBuffer getMappedBuffer()
            throws IOException {
        if ( buffer0_ == null ) {
            logger_.config( "Mapping " + logLabel_ );
            if ( offset_ + leng_ > channel_.size() ) {
                String msg = "File too short mapping " + logLabel_
                           + " (" + channel_.size() + " < "
                           + ( offset_ + leng_ ) + ") - truncated/corrupted?";
                throw new EOFException( msg );
            }
            buffer0_ = channel_.map( FileChannel.MapMode.READ_ONLY,
                                     offset_, leng_ );
        }
        return buffer0_;
    }

    /**
     * Attempts to unmap the single buffer mapping the file region.
     * Depending on this object's Unmapper, this may or may not be a NOP.
     * However, in case of actual umapping, it is essential not to use
     * any of the returned buffers created from it following this operation.
     */
    private synchronized void unmapBuffer() {
        if ( buffer0_ != null ) {
            boolean unmapped = unmapper_ == null ? false
                                                 : unmapper_.unmap( buffer0_ );
            buffer0_ = null;
            logger_.config( "Dispose of mapped buffer " + logLabel_
                          + ( unmapped ? " (unmapped)" : " (no effect)" ) );
        }
    }
}
