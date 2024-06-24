package uk.ac.starlink.array;

import java.io.IOException;
import java.net.URL;

/**
 * N-dimensional array access.
 * An NDArray represents a hyper-cuboidal array of java primitive
 * values.  This array is potentially large, in that it may contain
 * more than Integer.MAX_VALUE (2^31) elements, and so its data
 * cannot in general be assumed to fit in a java array.
 * <p>
 * This interface defines the methods used to enquire information
 * about an NDArray and to acquire access to its pixels for read and/or write.
 * <p>
 * The bulk data of an NDArray can be considered as a vector of values
 * of a given primitive type each labelled with a scalar offset (starting
 * at zero), or as an N-dimensional hyper-rectangle of such primitive values
 * each labelled with an N-element coordinate vector.  The mapping between
 * the offsets and the coordinates is determined by an {@link OrderedNDShape}
 * object belonging to the NDArray.
 * <p>
 * To obtain acess to the pixel data itself it is necessary to obtain
 * an ArrayAccess object using the {@link #getAccess} method.  This provides
 * methods to move around within the NDArray and read/write arrays full
 * of pixels.
 * It is possible to access data a pixel at a time in this way, but this
 * is generally not very efficient, and it is better to read/write a 
 * block of pixels to/from an array of moderate size.  Since there
 * may be a large number of pixels (perhaps &gt;2**31) in an NDArray 
 * it is not in general possible to read/write all the pixel data 
 * to/from a single primitive array, so for general processing it is
 * necessary to step through the pixel data processing chunks at a time.
 * The {@link ChunkStepper} utility class is provided as a convenience
 * for this sort of processing.  Here is a code snippet which uses
 * this technique to calculate statistics on all the pixels in an NDArray:
 * <pre>
 *     // Obtain the NDArray from somewhere.
 *     NDArray nda = ...
 *
 *     // Get a reader object to access the pixel data.
 *     ArrayAccess reader = nda.getAccess();
 *
 *     // Get a ChunkStepper to assist in working through the data.
 *     long npix = nda.getShape().getNumPixels();
 *     ChunkStepper chunkIt = new ChunkStepper( npix );
 *
 *     // Create a primitive buffer array of the right type for the NDArray, 
 *     // long enough to hold the biggest chunk.
 *     Object buffer = nda.getType().newArray( chunkIt.getSize() );
 * 
 *     // Step through the array in chunks, reading then processing a 
 *     // buffer-full each time.
 *     for ( chunkIt.hasNext(); chunkIt.next() ) {
 *         int size = chunkIt.getSize();
 *         reader.read( buffer, 0, size );
 *         accumulateStatistics( buffer, 0, size );
 *     }
 *
 *     // Relinquish the reader to allow resources to be reclaimed.
 *     reader.close();
 * </pre>
 * In the above the {@link Type#newArray} method is used to obtain a
 * buffer array of the same primitive type as the NDArray - this makes
 * it easier to write code which can process multiple numeric types
 * without having to write special cases for each numeric type.
 * However, if the NDArray was known to have a type of Type.FLOAT say,
 * this line could be replaced by
 * <pre>
 *     float[] buffer = new float[ chunkIt.getSize() ];
 * </pre>
 * <p>
 * Read and write access are not always available; use the 
 * {@link #isReadable} and {@link #isWritable} methods to determine
 * read/writablity.
 * <p>
 * While all NDArrays support one-time sequential access to the pixels
 * from start to finish, some support other access modes.
 * <dl>
 * <dt>random access
 * <dd>If the {@link #isRandom} method returns true,
 *     then it is possible to use the {@link ArrayAccess#setPosition} or 
 *     {@link ArrayAccess#setOffset}
 *     methods of the reader/writer to move backwards within the array
 *     as well as forwards between accesses.
 * <dt>multiple accessors
 * <dd>If {@link #multipleAccess} returns true,
 *     it is possible to call the getAccess method more than once
 *     for independent accesses to the data.  This will always be the 
 *     case if isRandom is true.
 * <dt>mapped access
 * <dd>If the {@link ArrayAccess#isMapped} method of the obtained ArrayAccess 
 *     object returns true, the NDArray 
 *     provides <i>mapped</i> access.  This means that the 
 *     {@link ArrayAccess#getMapped} method will return a java primitive array 
 *     containing the entire content of the NDArray
 * </dl>
 * <p>
 * If mapped access exists, it will be the most efficent way of 
 * accessing the data.  The following code calculates statistics as above
 * in the most efficent way it can, by using mapped access if available.
 * <pre>
 *     ArrayAccess reader = nda.getAccess();
 *     if ( reader.isMapped() ) {
 *         accumulateStatistics( reader.getMapped() );
 *     }
 *     else {
 *         ... step through in chunks as previous example
 *     }
 * </pre>
 * <p>
 * An NDArray is normally created using the {@link NDArrayFactory} class.
 * This class can also be used to obtain an NDArray with random access
 * from one which doesn't have it.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface NDArray extends ArrayDescription {

    /**
     * Returns the URL of this NDArray, or null if it does not have one.
     * An NDArray will normally only have a URL if it has been created
     * using one of the calls to NDArrayFactory which takes a URL argument.
     * Feeding the returned URL to the NDArrayFactory will obtain the
     * same NDArray as this one.
     * Scratch NDArrays, and virtual NDArrays created by wrapping another
     * to give access using different characteristics (type, shape etc)
     * will in general not have a URL.
     * The return value must not change over the lifetime of this object.
     *
     * @return   the URL at which this NDArray resides, or null if it is
     *           not persistent
     */
    URL getURL();

    /**
     * Indicates whether multiple calls to the getAccess method may be
     * made.  This method will always return true if the {@link #isRandom}
     * method returns true, but may do so even for non-random arrays.
     *
     * @return  true if multiple independent accessor objects are available
     */
    boolean multipleAccess();

    /**
     * Returns an object which can read and/or write the pixels of this
     * NDArray.  
     * The returned ArrayReader should be closed when it is no longer
     * required; this enables resources it may hold to be released.
     * <p>
     * Each call to this method returns a new and independent 
     * ArrayAccess object.  However it may or may not be possible to
     * call it more than once; the {@link #multipleAccess} method 
     * indicates whether this is the case.
     *
     * @return  an accessor for the pixel data
     * @throws  IOException  if there is some I/O error
     * @throws  IllegalStateException  if close has been called on this
     *              NDArray
     * @throws  UnsupportedOperationException  if multipleAccess returns 
     *              false and getAccess has already been invoked once
     */
    ArrayAccess getAccess() throws IOException;

    /**
     * Declares that this NDArray will not be required for further use;
     * in particular that no further invocations will be made of the 
     * getAccess method.
     * <p>
     * This method should be invoked on an NDArray
     * when it is no longer required for pixel access.
     * This allows reclamation of non-memory resources, and in the case
     * of writable arrays it may also be required to ensure that data
     * is flushed from buffers back to the actual pixel array.
     * <p>
     * An array should not however be closed if some other object might
     * still require pixel access to it via a retained reference.
     * The general rule is that an application
     * which obtains a new NDArray from a URL should arrange for 
     * <code>close</code> to be called 
     * on it at the end of its lifetime, but that utility routines which
     * perform operations on an NDArray should not close it after use
     * (though they should close any <code>ArrayAccess</code> objects
     * which they take out).
     * Note that closing an NDArray will normally result in closing any
     * arrays which it wraps.
     * <p>
     * Multiple calls of this method may harmlessly be made.
     *
     * @throws  IOException  if some I/O error occurred
     */
    void close() throws IOException;

    /**
     * Obtains a DOMFacade representing this array.
     *
     * <p>Since the array does not `know' which type of element it is
     * representing, this must be passed to it when the
     * <code>DOMFacade</code> is created.
     *
     * @param hdxType the type of the element which this array is
     * to represent
     */
    uk.ac.starlink.hdx.HdxFacade getHdxFacade
            (uk.ac.starlink.hdx.HdxResourceType hdxType);
}
