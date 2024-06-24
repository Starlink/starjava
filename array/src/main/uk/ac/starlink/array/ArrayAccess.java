package uk.ac.starlink.array;

import java.io.IOException;

/**
 * Provides methods to read and write pixels from an NDArray.
 * The actual read and write methods deal with reading from the NDArray
 * pixel data into a java primitive array or writing from a java 
 * primitive array into NDArray pixel data.  While this can be used
 * to do single pixel read/writes, it is more efficient to read/write
 * a moderate-sized bufferfull at once.  The {@link ChunkStepper}
 * class is provided as a convenience to assist with this sort of
 * processing.
 * <p>
 * The accessor maintains an offset position which determines the starting
 * position of the next read/write.  This may be modified by the 
 * setOffset or setPosition methods.  The mapping between offset values
 * and position vectors is determined by the pixel ordering scheme
 * of this ArrayAccess (as determined by its {@link OrderedNDShape}).
 * <p>
 * Not all methods will work on a given accessor, depending on what
 * it is capable of; if the isRandom method returns false then 
 * the offset may not be set to a position before its current position;
 * if isReadable returns false then the read and readTile methods will fail;
 * and if isWritable returns false then the write and writeTile methods will 
 * fail.  In each of these cases the illegal accesses will result in
 * an UnsupportedOperationException.
 * <p>
 * If the read or write methods result in an IOException this will have
 * the side effect of closing this accessor for further access
 * (since under these circumstances the current offset may not be known).
 * 
 * @author   Mark Taylor (Starlink)
 */
public interface ArrayAccess extends ArrayDescription {

    /**
     * Returns the current offset into the array for read/write.
     *
     * @return   the index of the next element to be read/written
     */
    long getOffset();

    /**
     * Sets the offset into the array for the next read/write to occur.
     * Attempting to set the offset to a lower value than its current
     * one will fail if random access is not available (isRandom is false).
     *
     * @param   off  the position at which the next read/write will start
     * @throws  IOException  if some unexpected I/O error occurs
     * @throws  IndexOutOfBoundsException  if off&lt;0 or off&gt;=npixel
     * @throws  IllegalStateException   if this accessor has been closed
     * @throws  UnsupportedOperationException  if an attempt is made to set
     *              the offset to a value lower than its current one,
     *              and random access is not available for this accessor
     */
    void setOffset( long off ) throws IOException;

    /**
     * Returns the coordinates at which the next read/write will occur.
     *
     * @return  an N-element array giving the coordinates of the next
     *          read/write
     */
    long[] getPosition();

    /**
     * Sets the coordinates for the next read/write to occur.
     * Attempting to set the coordinates so that the new offset corresponds
     * to a lower value than its current one will fail if
     * random access is not available (isRandom returns false).
     *
     * @param  pos  an N-element array giving the coordinates for the next
     *              read/write
     * @throws  IOException  if some unexpected I/O error occurs
     * @throws  IndexOutOfBoundsException  if pos is outside the array
     * @throws  IllegalStateException   if this accessor has been closed
     * @throws  UnsupportedOperationException  if an attempt is made to set
     *              the offset to a value lower than its current one,
     *              and random access is not available
     */
    void setPosition( long[] pos ) throws IOException;

    /**
     * Reads a number of pixels from the current offset into a specified
     * part of a supplied primitive array.  The current offset will be updated
     * accordingly (to the point after the read pixels).
     * <p>
     * An IOException during the read will have the effect of closing this
     * accessor for further access.
     *
     * @param   buffer an array of the appropriate primitive type for this
     *                 accessor into whose elements
     *                 <code>start..start+size</code> the next <code>size</code>
     *                 pixels will be read
     * @param   start  the starting offset into buffer into which the
     *                 pixels should be read
     * @param   size   the number of pixels to read (also the amount by
     *                 which the current offset will be incremented)
     * @throws  IOException   if there is an I/O error
     * @throws  UnsupportedOperationException  if this accessor is not
     *                 readable (isReadable returns false)
     * @throws  IllegalStateException   if this accessor has been closed
     * @throws  IllegalArgumentException  if buffer is not an array of
     *                 primitives with type matching the type of this accessor,
     *                 or if it has less than start+size elements
     */
    void read( Object buffer, int start, int size ) throws IOException;

    /**
     * Reads a tile of pixels into a supplied primitive array.  A tile is an
     * N-dimensional hypercuboid specified by an NDShape object.
     * The pixels are read into a given array in the order implied
     * by the ordering scheme of this object.
     * The specified tile must have the same dimensionality as this accessor,
     * but need not lie wholly or partially within its bounds --
     * pixels outside the intersection will be given the bad value.
     * <p>
     * The current offset will be updated to the point after the last
     * pixel in the intersection between the tile and this accessor.
     * <p>
     * It is possible to read a tile when random access is not
     * available, but only if the first pixel in the requested tile
     * is ahead of the current offset.
     * <p>
     * An IOException during the read will have the effect of closing this
     * accessor for further access.
     *
     * @param  buffer     an array of the appropriate primitive type for this
     *                    accessor and at least as long as the number of
     *                    pixels in tileShape
     * @param  tileShape  an NDShape object specifying the shape of the tile
     *                    to be read
     *
     * @throws  IOException   if there is an I/O error
     * @throws  UnsupportedOperationException
     *              if this object is not readable (isReadable returns false)
     *              or if it would be necessary to read a part of the data
     *              earlier than the current offset and random access is not
     *              available
     * @throws  IllegalArgumentException  if buffer is not an array of the
     *              right primitive type or has too few elements
     * @throws  IllegalStateException   if this accessor has been closed
     */
    void readTile( Object buffer, NDShape tileShape ) throws IOException;

    /**
     * Writes a number of pixels starting at the current offset from a
     * specified part of a supplied primitive array.  The current
     * offset will be updated accordingly (to the point after the last write).
     * <p>
     * If an IOException occurs during the read, this will have the
     * additional effect of closing this accessor for further access.
     *
     * @param   buffer an array of the appropriate primitive type for this
     *                 NDArray whose elements <code>start..start+size</code>
     *                 will be written out
     * @param   start  the starting point in buffer from which pixels
     *                 will be written
     * @param   size   the number of pixels to write (also the amount by
     *                 which the current offset will be incremented)
     * @throws  IOException   if there is an I/O error
     * @throws  UnsupportedOperationException  if this accessor is not
     *                 writable (isWritable returns false)
     * @throws  IllegalStateException   if this accessor has been closed
     * @throws  IllegalArgumentException  if buffer is not an array of
     *                 primitives with type matching the type of this accessor,
     *                 or if it has less than start+size elements
     */
    void write( Object buffer, int start, int size ) throws IOException;

    /**
     * Writes a tile of pixels from a supplied primitive array.  A tile is an
     * N-dimensional hypercuboid specified by an NDShape object.
     * The ordering of pixels in the array is implied
     * by the ordering scheme of this object.
     * The specified tile must have the same dimensionality as this accessor,
     * but need not lie wholly or partially within its bounds --
     * pixels outside the intersection will simply be ignored.
     * <p>
     * The current offset will be updated to the point after the last
     * pixel in the intersection between the tile and this accessor.
     * <p>
     * It is possible to read a tile when random access is not
     * available, but only if the first pixel in the requested tile
     * is ahead of the current offset.
     * <p>
     * If an IOException occurs during the write, this will have the
     * additional effect of closing this accessor for further access.
     *
     * @param  buffer     an array of the appropriate primitive type for this
     *                    NDArray and at least as long as the number of
     *                    pixels in tileShape (elements after this limit
     *                    will be ignored)
     * @param  tileShape  an NDShape object specifying the shape of the tile
     *                    to be written
     *
     * @throws  IOException   if there is an I/O error
     * @throws  UnsupportedOperationException
     *              if this accessor is not writable (isWritable returns false)
     *              or if it would be necessary to write a part of the data
     *              earlier than the current offset and random access is not
     *              available
     * @throws  IllegalArgumentException  if buffer is not an array of the
     *              right primitive type or has too few elements
     * @throws  IllegalStateException   if this accessor has been closed
     */
    void writeTile( Object buffer, NDShape tileShape ) throws IOException;

    /**
     * Indicates whether mapped access is available.  If true, the
     * getMapped method will return a reference to the java primitive
     * array containing all the pixels of this NDArray.
     * The return value must not change over the lifetime of this object.
     * 
     * @return  true if mapped access is possible
     */
    boolean isMapped();

    /**
     * Returns a single primitive array holding all the data of this array.
     * Calling this method does not do significant work, but returns
     * a reference to an existing mapped array.
     * Access to the array data using this method, if available, 
     * will be more efficient than by using the read/write methods.
     * In the case of a writable accessor, making changes to the returned
     * primitive array will result in changes to the accessor pixel data.
     * In the case of an accessor which is not writable, the effect of
     * making changes to the returned array is undefined; in particular
     * it may result in an exception.
     * <p>
     * The method will fail unless isMapped returns true.
     *
     * @return  a primitive array, of type implied by the Type of this
     *          object, containing all the pixels of this array,
     *          in its natural ordering
     * @throws  UnsupportedOperationException  if mapped access is not
     *          available
     * @throws  IllegalStateException   if this accessor has been closed
     */
    Object getMapped();

    /**
     * Shuts down this accessor for further data access.
     * Following a call to <code>close</code> the offset
     * will have an illegal value
     * and calls to any read, write or position setting methods
     * will fail with an IllegalStateException.
     * A <code>close</code> should always
     * be called on an ArrayAccess when it is finished with.
     * In the case of a readable
     * object it enables release of associated resources beyond those taken
     * care of by the garbage collector, and in the case of writable
     * object it may also be required to ensure that data is actually
     * flushed to the underlying data storage.
     * This method may harmlessly be called on an accessor which has already
     * been closed.
     *
     * @throws  IOException   if there is an I/O error
     */
    void close() throws IOException;

}
