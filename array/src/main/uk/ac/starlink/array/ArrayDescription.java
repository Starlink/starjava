package uk.ac.starlink.array;

/**
 * Describes the basic characteristics of an N-dimensional array.
 * The shape, pixel ordering scheme, data type, and bad value handler
 * are available.  The immutable components of an array described
 * by this interface are:
 * <dl>
 * <dt>type:
 * <dd>indicates the type of the primitive data
 * <dt>shape:
 * <dd>gives the origin, dimensions and pixel sequence of the array
 * <dt>badHandler:
 * <dd>provides intelligence about how bad pixel values are stored
 * <dt>isRandom:
 * <dd>flag indicating whether random access (backwards seeks) is available
 * <dt>isReadable:
 * <dd>flag indicating whether pixels can be read
 * <dt>isWritable:
 * <dd>flag indicating whether pixels can be written
 * </dl>
 *
 * @author   Mark Taylor
 */
public interface ArrayDescription {

    /**
     * Gets the OrderedNDShape object which describes the origin, dimensions
     * and pixel ordering scheme of this array.
     * The return value must not change over the lifetime of this object.
     *
     * @return  an OrderedNDShape object appropriate to this array
     */
    OrderedNDShape getShape();

    /**
     * Returns the type of the primitive data in this array.
     * The return value must not change over the lifetime of this object.
     *
     * @return  a Type object indicating the primitive element type
     */
    Type getType();

    /**
     * Gets an object capable of handling bad pixel values for this array.
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

}
