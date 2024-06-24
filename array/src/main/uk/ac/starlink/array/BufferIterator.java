package uk.ac.starlink.array;

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * Provides buffers for convenient stepping through an array.
 * At each step a primitive buffer of a given type of just the right
 * size to match the current chunk is returned.
 * This class is provided as a convenience for applications code which
 * wishes to iterate through an array in sections, using a buffer 
 * of the size matching the section at each step.
 * <p>
 * This class provides a thin convenience wrapper around 
 * {@link ChunkStepper}, which is itself a simple class which steps
 * from zero to a given limit in chunks.  The only additional functionality 
 * provided by a <code>BufferIterator</code> is that it will ensure a 
 * suitable primitive buffer is available at each step, and (since the
 * <code>next</code> method actually returns something, namely the buffer),
 * it implements the {@link java.util.Iterator} interface which
 * <code>ChunkStepper</code> does not.
 * <p>
 * A typical use of <code>BufferIterator</code> is as follows:
 * <pre>
 *     ArrayAccess acc = ndarray.getAccess();
 *     for ( BufferIterator bufIt = new BufferIterator( npix, Type.DOUBLE );
 *           bufIt.hasNext(); ) {
 *         double[] buf = (double[]) bIt.next();
 *         acc.read( buf, 0, buf.length );
 *         doStuff( buf );
 *     }
 * </pre>
 *
 * @author   Mark Taylor (Starlink)
 * @see      ChunkStepper
 */
public class BufferIterator implements Iterator {

    private ChunkStepper chunkIt;
    private Object buffer;
    private Type type;
    private long base = -1;

    /**
     * Create a new <code>BufferIterator</code> with a given chunk size.
     *
     * @param   length     the total number of elements to iterate over
     * @param   type       the type of the primitive buffer which the
     *                     <code>next</code> method will return
     *                     at each iteration
     * @param   chunkSize  the size of buffer which will be used (except
     *                     perhaps for the last chunk)
     * @throws  IllegalArgumentException  if <code>chunkSize&lt;=0</code>
     *                                    or <code>length&lt;0</code>
     */
    public BufferIterator( long length, Type type, int chunkSize ) {
        chunkIt = new ChunkStepper( length, chunkSize );
        this.type = type;
        if ( type == null ) {
            throw new NullPointerException();
        }
    }

    /**
     * Create a new <code>BufferIterator</code> with a default chunk size.
     *
     * @param   length     the total number of elements to iterate over
     */
    public BufferIterator( long length ) {
        chunkIt = new ChunkStepper( length );
        this.type = type;
        if ( type == null ) {
            throw new NullPointerException();
        }
    }

    /**
     * Returns a primitive buffer of this object's type, with a length
     * matching that of this chunk.
     * Note it is not necessarily a new buffer created for each iteration,
     * the one returned by this method may be the same one that it 
     * returned last time (in fact it will be, except perhaps for the
     * last iteration when a smaller one may be required).
     *
     * @return   a primitive array of the same size as this chunk
     * @throws  java.util.NoSuchElementException if hasNext would return false
     */
    public Object next() {
        int size = chunkIt.getSize();
        base = chunkIt.getBase();
        chunkIt.next();
        if ( buffer == null || Array.getLength( buffer ) != size ) {
            buffer = type.newArray( size );
        }
        return buffer;
    }

    /**
     * See if iteration has finished.
     *
     * @return  true iff there are more chunks
     */
    public boolean hasNext() {
        return chunkIt.hasNext();
    }

    /**
     * Remove functionality is not implemented by this class.
     *
     * @throws  UnsupportedOperationException  always
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * The offset of the base of the chunk most recently returned by
     * <code>next</code>.  This will be zero for the first
     * chunk, and increasing by the size of the buffer returned by 
     * <code>next</code> with each iteration after that.
     *
     * @return  the base of the current chunk
     * @throws  IllegalStateException   if called before the first call of
     *          <code>next</code>
     */
    public long getBase() {
        if ( base >= 0 ) {
            return base;
        }
        else {
            throw new IllegalStateException( "getBase() called before next()" );
        }
    }

}
