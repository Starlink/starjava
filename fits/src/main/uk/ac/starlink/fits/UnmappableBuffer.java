package uk.ac.starlink.fits;

import java.nio.ByteBuffer;

/**
 * Aggregates a ByteBuffer representing a mapped region of a file
 * and a way to attempt to reclaim its resources (off-heap memory).
 *
 * @author   Mark Taylor
 * @since    23 Jul 2026
 */
public interface UnmappableBuffer {

    /**
     * Returns the buffer.
     *
     * @return  buffer
     */
    ByteBuffer getBuffer();

    /**
     * Attempts to reclaim resources used by the buffer.
     * DO NOT attempt to use the buffer following a call to this method;
     * depending on the implementation, very bad things could happen.
     *
     * @return   true iff resources are actually reclaimed as a result
     *           of this call
     */
    boolean unmapBuffer();
}
