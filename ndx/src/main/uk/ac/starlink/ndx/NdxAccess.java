package uk.ac.starlink.ndx;

import java.io.IOException;
import uk.ac.starlink.array.ArrayDescription;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Type;

/**
 * Unified access to Ndx array data.
 * An NdxAccess object should normally be used to access the pixel data
 * of an Ndx object.  Although it is possible to examine the pixels
 * of the component Image, Variance and Quality NDArrays directly,
 * use of an NdxAccess will usually be more convenient and may be more
 * efficient.
 * The main jobs an NdxAccess does are:
 * <ul>
 * <li>Provide a chosen set of arrays (one or more of image, variance, quality)
 * <li>Ensure that they are of compatible shape, pixel sequence, bad value
 *     handler and primitive type
 * <li>Ensure that corresponding pixels from the image, variance,
 *     quality arrays are accessed at the same time
 * <li>Modify the image and variance pixels in accordance with the
 *     corresponding quality pixels if appropriate
 * </ul>
 * 
 * @author   Mark Taylor (Starlink)
 * @see  Ndx
 */
public interface NdxAccess extends ArrayDescription {

    // Methods from ArrayDescription

    /**
     * Gets the OrderedNDShape object which describes the origin, dimensions
     * and pixel ordering scheme of the components of this accessor.
     * The return value must not change over the lifetime of this object.
     *
     * @return  an OrderedNDShape object appropriate to this array
     */
    OrderedNDShape getShape();

    /**
     * Returns the type of the primitive data in the image and variance
     * components of this accessor.
     * The return value must not change over the lifetime of this object.
     *
     * @return  a Type object indicating the primitive element type
     */
    Type getType();

    /**
     * Gets an object capable of handling bad pixel values for 
     * the image and variance components of this accessor.
     * The return value must not change over the lifetime of this object.
     *
     * @return  the bad value handler
     */
    BadHandler getBadHandler();

    /**
     * Indicates whether random access is available.  If true, it is
     * possible to set the offset to any point within the bounds of the
     * array, but if false it can only ever move forward.
     * The return value must not change over the lifetime of this object.
     *
     * @return   true if random access is possible
     */
    boolean isRandom();

    /**
     * Indicates whether the pixels of this array may be read.
     * The return value must not change over the lifetime of this object.
     *
     * @return   true if this array is readable
     */
    boolean isReadable();

    /**
     * Indicates whether the pixels of this array may be written.
     * The return value must not change over the lifetime of this object.
     *
     * @return   true if this array is writable
     */
    boolean isWritable();



    // Methods only in NdxAccess

    /**
     * Indicates whether this access object has Image component pixels. 
     *
     * @return  true if and only if image pixels are provided
     */
    boolean hasImage();

    /**
     * Indicates whether this access object has Variance component pixels.
     *
     * @return  true if and only if variance pixels are provided
     */
    boolean hasVariance();

    /**
     * Indicates whether this access object has Quality component pixels.
     *
     * @return  true if and only if quality pixels are provided
     */
    boolean hasQuality();

    /**
     * Indicates whether mapped access is available for this accessor.
     *
     * @return  true if and only if all of the image, variance, quality
     *          components provided by this accessor can be mapped
     * @see  getMappedImage
     * @see  getMappedVariance
     * @see  getMappedQuality
     */
    boolean isMapped();

    /**
     * Returns the current offset into the arrays for read/write.
     *
     * @return  the index of the next pixel to be read/written
     *
    long getOffset();

    /**
     * Sets the offset into the arrays for the next read/write to occur.
     * Attempting to set the offset to a lower value than its current
     * one will fail if isRandom returns false.
     *
     * @param  off   the offset at which the next read/write will start
     * @throws  IOException  if an I/O error occurred
     * @throws  IndexOutOfBoundsException   if off<0 or off>=npixel
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
     * Returns a single primitive array holding all the image pixels.
     * Calling this method should not do significant work, but returns
     * a reference to an existing mapped array.
     * Access to array data using this method, if available, will be
     * more efficient than by using the read/write methods.
     * In the case of a writable accessor, making changes to the returned
     * primitive array will result in changes to the accessor pixel data.
     * In the case of an accessor which is not writable, the effect of
     * making changes to the returned array is undefined; in particular
     * it may result in an exception.
     * <p>
     * This method will fail unless both {@link #isMapped} and 
     * {@link #hasImage} return true.
     * 
     * @return  a java primitive array of the appropriate type for this
     *          accessor holding the pixels of the image component in 
     *          pixel sequence order
     * @throws  UnsupportedOperationException  if mapped access is not
     *          available or this accessor does not provide image data
     * @throws  IllegalStateException   if this accessor has been closed
     */
    Object getMappedImage();

    /**
     * Returns a single primitive array holding all the variance pixels.
     * Calling this method should not do significant work, but returns
     * a reference to an existing mapped array.
     * Access to array data using this method, if available, will be
     * more efficient than by using the read/write methods.
     * In the case of a writable accessor, making changes to the returned
     * primitive array will result in changes to the accessor pixel data.
     * In the case of an accessor which is not writable, the effect of
     * making changes to the returned array is undefined; in particular
     * it may result in an exception.
     * <p>
     * This method will fail unless both both {@link #isMapped} and 
     * {@link #hasVariance} return true.
     * 
     * @return  a java primitive array of the appropriate type for this
     *          accessor holding the pixels of the variance component in 
     *          pixel sequence order
     * @throws  UnsupportedOperationException  if mapped access is not
     *          available or this accessor does not provide variance data
     * @throws  IllegalStateException   if this accessor has been closed
     */
    Object getMappedVariance();

    /**
     * Returns a single primitive array holding all the quality pixels.
     * Calling this method should not do significant work, but returns
     * a reference to an existing mapped array.
     * Access to array data using this method, if available, will be
     * more efficient than by using the read/write methods.
     * In the case of a writable accessor, making changes to the returned
     * primitive array will result in changes to the accessor pixel data.
     * In the case of an accessor which is not writable, the effect of
     * making changes to the returned array is undefined; in particular
     * it may result in an exception.
     * <p>
     * This method will fail unless both both {@link #isMapped} and 
     * {@link #hasQuality} return true.
     * 
     * @return  a java byte array holding the pixels of the quality 
     *          component in pixel sequence order
     * @throws  UnsupportedOperationException  if mapped access is not
     *          available or this accessor does not provide variance data
     * @throws  IllegalStateException   if this accessor has been closed
     */
    byte[] getMappedQuality();

    /**
     * Reads the pixel data for this accessor into specified parts of
     * supplied parallel arrays.  The current offset will be updated
     * accordingly (to the point after the read pixels).
     * <p>
     * Supplied buffer arguments corresponding to components not provided by
     * this accessor <i>must</i> be given as <tt>null</tt>.
     * <p>
     * An IOException during the read will have the effect of closing this
     * accessor for further access.
     *
     * @param  ibuf   if image access is provided, a primitive array of 
     *                the appropriate type into whose elements 
     *                <tt>start..start+size</tt> the next <tt>size</tt>
     *                image elements will be read.
     *                If image access is not provided, must be null
     * @param  vbuf   if variance access is provided, a primitive array of 
     *                the appropriate type into whose elements 
     *                <tt>start..start+size</tt> the next <tt>size</tt>
     *                variance elements will be read.
     *                If variance access is not provided, must be null
     * @param  qbuf   if quality access is provided, a byte array
     *                into whose elements 
     *                <tt>start..start+size</tt> the next <tt>size</tt>
     *                quality elements will be read.
     *                If quality access is not provided, must be null
     * @param  start  the starting offset into the buffers at which 
     *                pixels should be read
     * @param  size   the number of pixels to read
     * @throws     IOException  if there is an I/O error
     * @throws     UnsupportedOperationException  if this accessor is not
     *                 readable
     * @throws     IllegalStateException  if this accessor has been closed
     * @throws     IllegalArgumentException   if the buffers are too short
     *                 or the wrong type, or if one is not null when the
     *                 corresponding component is not provided
     */
    void read( Object ibuf, Object vbuf, byte[] qbuf, int start, int size )
        throws IOException;

    /**
     * Reads a tile of pixel data into supplied parallel arrays.
     * A tile is an N-dimensional hypercuboid specified by an NDShape object.
     * The pixels are read into the arrays in the order implied by
     * the ordering scheme of this object.
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
     * Supplied buffer arguments corresponding to components not provided by
     * this accessor <i>must</i> be given as <tt>null</tt>.
     * <p>
     * An IOException during the read will have the effect of closing this
     * accessor for further access.
     *
     * @param  ibuf   if image access is provided, a primitive array of 
     *                the appropriate type at least as long as the 
     *                number of pixels in <tt>tile</tt>
     *                If image access is not provided, must be null
     * @param  vbuf   if variance access is provided, a primitive array of 
     *                the appropriate type at least as long as the 
     *                number of pixels in <tt>tile</tt>
     *                If variance access is not provided, must be null
     * @param  qbuf   if quality access is provided, a byte array
     *                at least as long as the nubmer of pixels in <tt>tile</tt>
     *                If quality access is not provided, must be null
     * @param  start  the starting offset into the buffers at which 
     *                pixels should be read
     * @param  size   the number of pixels to read
     * @throws     IOException  if there is an I/O error
     * @throws     UnsupportedOperationException  if this accessor is not
     *                 readable
     * @throws     IllegalStateException  if this accessor has been closed
     * @throws     IllegalArgumentException   if the buffers are too short
     *                 or the wrong type, or if one is not null when the
     *                 corresponding component is not provided
     */
    void readTile( Object ibuf, Object vbuf, byte[] qbuf, NDShape tile )
        throws IOException;

    /**
     * Writes pixel data for this accessor from specified parts of
     * supplied parallel arrays.  The current offset will be updated
     * accordingly (to the point after the written pixels).
     * <p>
     * Supplied buffer arguments corresponding to components not provided by
     * this accessor <i>must</i> be given as <tt>null</tt>.
     * <p>
     * An IOException during the write will have the effect of closing this
     * accessor for further access.
     *
     * @param  ibuf   if image access is provided, a primitive array of 
     *                the appropriate type from whose elements 
     *                <tt>start..start+size</tt> the next <tt>size</tt>
     *                image elements will be written.
     *                If image access is not provided, must be null
     * @param  vbuf   if variance access is provided, a primitive array of 
     *                the appropriate type from whose elements 
     *                <tt>start..start+size</tt> the next <tt>size</tt>
     *                variance elements will be written.
     *                If variance access is not provided, must be null
     * @param  qbuf   if quality access is provided, a byte array
     *                from whose elements 
     *                <tt>start..start+size</tt> the next <tt>size</tt>
     *                quality elements will be written.
     *                If quality access is not provided, must be null
     * @param  start  the starting offset into the buffers from which 
     *                pixels should be written
     * @param  size   the number of pixels to write
     * @throws     IOException  if there is an I/O error
     * @throws     UnsupportedOperationException  if this accessor is not
     *                 writable
     * @throws     IllegalStateException  if this accessor has been closed
     * @throws     IllegalArgumentException   if the buffers are too short
     *                 or the wrong type, or if one is not null when the
     *                 corresponding component is not provided
     */
    void write( Object ibuf, Object vbuf, byte[] qbuf, int start, int size )
        throws IOException;

    /**
     * Writes a tile of pixel data into supplied parallel arrays.
     * A tile is an N-dimensional hypercuboid specified by an NDShape object.
     * The pixels are written from the arrays in the order implied by
     * the ordering scheme of this object.
     * The specified tile must have the same dimensionality as this accessor,
     * but need not lie wholly or partially within its bounds --
     * pixels outside the intersection will simply be ignored.
     * <p>
     * The current offset will be updated to the point after the last
     * pixel in the intersection between the tile and this accessor.
     * <p>
     * It is possible to write a tile when random access is not
     * available, but only if the first pixel in the requested tile
     * is ahead of the current offset.
     * <p>
     * Supplied buffer arguments corresponding to components not provided by
     * this accessor <i>must</i> be given as <tt>null</tt>.
     * <p>
     * An IOException during the write will have the effect of closing this
     * accessor for further access.
     *
     * @param  ibuf   if image access is provided, a primitive array of 
     *                the appropriate type at least as long as the 
     *                number of pixels in <tt>tile</tt>
     *                If image access is not provided, must be null
     * @param  vbuf   if variance access is provided, a primitive array of 
     *                the appropriate type at least as long as the 
     *                number of pixels in <tt>tile</tt>
     *                If variance access is not provided, must be null
     * @param  qbuf   if quality access is provided, a byte array
     *                at least as long as the nubmer of pixels in <tt>tile</tt>
     *                If quality access is not provided, must be null
     * @param  start  the starting offset into the buffers from which 
     *                pixels should be written
     * @param  size   the number of pixels to write
     * @throws     IOException  if there is an I/O error
     * @throws     UnsupportedOperationException  if this accessor is not
     *                 writable
     * @throws     IllegalStateException  if this accessor has been closed
     * @throws     IllegalArgumentException   if the buffers are too short
     *                 or the wrong type, or if one is not null when the
     *                 corresponding component is not provided
     */
    void writeTile( Object ibuf, Object vbuf, byte[] qubf, NDShape tile )
        throws IOException;

    /**
     * Shuts down this accessor for further data access.
     * Following a call to <tt>close</tt> the offset will have an illegal
     * value and calls to any read, write or position setting methods
     * will fail with an <tt>IllegalStateException</tt>.  A <tt>close</tt>
     * should always be called on an <tt>NdxAccess</tt> when it is
     * finished with.
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
