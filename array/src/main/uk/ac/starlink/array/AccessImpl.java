package uk.ac.starlink.array;

import java.io.IOException;

/**
 * Interface for the implementation of read/write access to the pixels 
 * of an array.
 * This is the interface via which array implementations provide 
 * pixel read/write services to the BridgeNDArray class.  BridgeNDArray,
 * which performs parameter validations, is expected to be the only
 * client of AccessImpl, so that implementations of this interface
 * can in general assume that the arguments they receive make sense.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface AccessImpl {

    /**
     * Sets the offset into the array at which the next read/write will occur.
     * Parameter validation will have occurred prior to this call,
     * so it may be assumed that the offset is a legal value (between 0
     * and the array length implied by the dimensions of its owner
     * ArrayImpl, and in the case of non-random access, &gt;= the
     * current offset).
     *
     * @param  off  the offset into the data array
     */
    void setOffset( long off ) throws IOException;

    /**
     * Reads a number of pixels from the current offset into
     * a specified part of a supplied java array.  The offset will be
     * updated accordingly (to the point after the read pixels).
     * Parameter validation will have occurred prior to this call,
     * so it may be assumed that the buffer is an array of the right type
     * and long enough, and the requested size will not exceed the
     * number of pixels remaining between the length and offset of
     * this reader.
     * This method will not be called if this accessor was obtained
     * from a non-readable ArrayImpl.
     *
     * @param   buffer an array of the appropriate primitive type for this
     *                 NDArray into whose elements
     *                 <code>start..start+size</code> the pixels will be
     *                 read
     * @param   start  the starting offset into array into which the
     *                 pixels should be read
     * @param   size   the number of pixels to read (also the amount by
     *                 which the current offset will be incremented)
     * @throws  IOException   if there is an I/O error
     */
    void read( Object buffer, int start, int size ) throws IOException;

    /**
     * Writes a number of pixels starting at the current offset from a
     * specified part of a supplied array.  The current offset will be
     * updated accordingly (to the point after the last write).
     * Parameter validation will have occurred prior to this call,
     * so it may be assumed that the buffer of the right type and long
     * enough, and the requested size will not exceed the number of
     * pixels remaining between the length and offset of this writer.
     * This method will not be called if this accessor was obtained
     * from a non-writable AccessImpl.
     *
     * @param   buffer an array of the appropriate primitive type for this
     *                 ArrayImpl whose elements <code>start..start+size</code>
     *                 will be written out
     * @param   start  the starting point in the array from which pixels
     *                 will be written
     * @param   size   the number of pixels to write (also the amount by
     *                 which the current offset will be incremented)
     * @throws  IOException   if there is an I/O error
     */
    void write( Object buffer, int start, int size ) throws IOException;

    /**
     * This method will be called when read/write access to this object
     * is no longer required.  It should free such non-memory resources
     * and flush such buffers associated with this accessor (not with
     * the parent ArrayImpl) as are required.  Following this call
     * no write or set method invocations will be attempted.
     * <p>
     * This method will not be invoked more than once.
     *
     * @throws  IOException   if there is an I/O error
     */
    void close() throws IOException;

}
