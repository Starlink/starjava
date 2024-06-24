package uk.ac.starlink.array;

import java.util.NoSuchElementException;

/**
 * Allows convenient stepping through an array.
 * This class is provided as a convenience for applications code which 
 * wishes to iterate through an array in sections, at each stage 
 * obtaining a java primitive array containing a contiguous chunk of
 * its pixels.  This may be more efficient than using the
 * single-element read/write methods of NDArray.
 * <p>
 * This class does not do anything very clever; it simply provides at
 * each iteration base and length of a block which will take you from
 * the start to the end of an array of given size over the lifetime of
 * the iterator.  These blocks will
 * be of the same (user-defined or default) size with the possible 
 * exception of the last one, which will just mop up any remaining
 * elements.
 * <p>
 * The simplest use of this class would therefore look something like this
 * <pre>
 *     ArrayAccess acc = nda.getAccess(); 
 *     long npix = acc.getShape().getNumPixels();
 *     for ( ChunkStepper cIt = new ChunkStepper( npix ); 
 *           cIt.hasNext(); cIt.next() ) {
 *         int size = cIt.getSize();
 *         Object buffer = acc.getType().newArray( size );
 *         acc.read( buffer, 0, size );
 *         doStuff( buffer );
 *     }
 * </pre>
 * A more efficient loop would reuse the same buffer array to save on
 * object creation/collection costs as follows:
 * <pre>
 *     ChunkStepper cIt = new ChunkStepper( npix );
 *     Object buffer = acc.getType().newArray( cIt.getSize() );
 *     for ( ; cIt.hasNext(); cIt.next() ) {
 *         acc.read( buffer, 0, cIt.getSize() );
 *         doStuff( buffer );
 *     }
 * </pre>
 * The {@link BufferIterator} class provides very similar functionality
 * in a way which may be slightly more convenient to use.
 * 
 * @author   Mark Taylor
 * @see      BufferIterator
 * @version  $Id$
 */
public class ChunkStepper {

    private long chunkBase = 0L;
    private final long length;
    private final int chunkSize;

    /** The default size of chunks if not otherwise specified. */
    public static int defaultChunkSize = 16384;

    /**
     * Create a new ChunkStepper with a given chunk size.
     *
     * @param   length     the total number of elements to iterate over
     * @param   chunkSize  the size of chunk which will be used (except
     *                     perhaps for the last chunk)
     * @throws  IllegalArgumentException  if <code>chunkSize&lt;=0</code>
     *                                    or <code>length&lt;0</code>
     */
    public ChunkStepper( long length, int chunkSize ) {
        if ( chunkSize <= 0 ) {
            throw new IllegalArgumentException( 
                "chunkSize " + chunkSize + " <= 0" );
        }
        if ( length < 0L ) {
            throw new IllegalArgumentException(
                "length " + length + " < 0" );
        }
        this.length = length;
        this.chunkSize = chunkSize;
    }

    /**
     * Create a new ChunkStepper with the default chunk size.
     *
     * @param   length     the total number of elements to iterate over
     */
    public ChunkStepper( long length ) {
        this( length, defaultChunkSize );
    }

    /**
     * See if iteration has finished.
     *
     * @return   true iff there are more chunks
     */
    public boolean hasNext() {
        return chunkBase < length;
    }

    /**
     * Get the size of the current chunk.  It will be equal to the size
     * specified in the constructor (or the default if none was specified),
     * except for the last chunk, when it may be smaller.
     *
     * @return   the current chunk size
     */
    public int getSize() {
        return (int) Math.min( length - chunkBase, (long) chunkSize );
    }

    /**
     * The offset of the base of the current chunk.  Zero for the first 
     * chunk, and increasing by getSize with each iteration after that.
     *
     * @return  the base of the current chunk
     */
    public long getBase() {
        return chunkBase;
    }

    /**
     * Iterates to the next chunk.
     *
     * @throws  NoSuchElementException if hasNext would return false
     */
    public void next() {
        if ( chunkBase < length ) {
            chunkBase += getSize();
        }
        else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the length of this ChunkStepper as supplied to the constructor -
     * the total number of elements over which it will iterate.
     */
    public long getTotalLength() {
        return length;
    }
}
