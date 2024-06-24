package uk.ac.starlink.array;

import java.io.IOException;

/**
 * Interface for implementation end of the <code>NDArray</code> Bridge pattern.
 * If you have an <code>ArrayImpl</code>, you can make an <code>NDArray</code> 
 * out of it using {@link BridgeNDArray}.
 * This is the basic interface via which array implementations provide
 * services to the <code>BridgeNDArray</code> class.
 * The <code>BridgeNDArray</code> class is intended to be
 * the only client of <code>ArrayImpl</code> instances, 
 * and it does the necessary validation of arguments before passing them
 * to ArrayImpl, so that implementations of this interface can in
 * general assume that the arguments they receive make sense.
 * Thus it is not necessary for an <code>ArrayImpl</code>
 * implementation to check
 * that it is writable before attempting a write, or that a
 * requested offset is within the known bounds of the array.
 * <p>
 * <code>BridgeNDArray</code> also makes guarantees about the order in which 
 * calls will be made:
 * <ul>
 * <li>There will be a maximum of one call to {@link #open}; it will not
 *     happen after a call to <code>close</code>
 * <li>There will be a maximum of one call to {@link #close}
 * <li>Certain methods ({@link #canMap}, {@link #getMapped}, {@link #getAccess})
 *     will only be made following an open call and before any close call
 * <li>{@link #getAccess} will not be called more than once if 
 *     {@link #multipleAccess} returns false
 * </ul>
 * This means that the open method may be used to do any expensive setup
 * which may be required by <code>getAccess</code>, <code>canMap</code> 
 * or <code>getMapped</code>.
 * The <code>close</code> method should be used for corresponding tear-down 
 * and/or tidying up of resources allocated at construction time; however
 * it cannot be guaranteed that a careless user will cause 
 * <code>close</code> to be invoked, so a responsible <code>ArrayImpl</code> 
 * implementation 
 * may wish to do such essential tear-down in the finalizer as well as
 * in close (<i>But&nbsp;note:</i> don't just do it in the finalizer,
 * since the finalizer may never be invoked either).
 *
 * @author   Mark Taylor (Starlink)
 * @see  NDArray
 * @see  BridgeNDArray
 */
public interface ArrayImpl {

    /**
     * Returns an object representing the shape (origin and dimensions)
     * and pixel sequence of this object.
     * The return value must not change over the lifetime of the object.
     *
     * @return  the ordered shape
     */
    OrderedNDShape getShape();

    /**
     * Returns the primitive type of the data held by this object.
     * The return value must not change over the lifetime of the object.
     *
     * @return  an object representing the type of data held.
     */
    Type getType();

    /**
     * The magic bad value for data.  The returned type should be one
     * of the primitive wrapper types, Byte, Short, Integer, Float, Double
     * as appropriate for the type of this array.  It may be <code>null</code>
     * if there is no bad value.
     * The return value must not change over the lifetime of the object.
     *
     * @return  the bad value
     */
    Number getBadValue();

    /**
     * Indicates whether read access is possible.  Reads will only be
     * attempted if this method returns true.
     * The return value must not change over the lifetime of the object.
     *
     * @return  whether read access is available
     */
    boolean isReadable();

    /**
     * Indicates whether write access is possible.  Writes will only be
     * attempted if this method returns true.
     * The return value must not change over the lifetime of the object.
     *
     * @return  whether write access is available
     */
    boolean isWritable();

    /**
     * Indicates whether random access is possible.  If this method returns
     * true, then it is permissible to set the offset to a value lower than
     * its current value.  If it is false, then no such invocations will
     * be attempted.
     * The return value must not change over the lifetime of the object.
     *
     * @return  whether random access is available
     */
    boolean isRandom();

    /**
     * Indicates whether the getAccess method may be called more than once.
     * 
     * @return  true if getAccess may be called more than once
     */
    boolean multipleAccess();

    /**
     * Prepares this ArrayImpl for pixel access.  
     * This method will be called no more than once, and it will be called
     * prior to any calls of the getAccess method.
     * 
     * @throws IOException  if there is an IO error
     */
    void open() throws IOException;

    /**
     * Indicates whether mapped access is available.  If true, then 
     * following an open call, the getMapped method will return a 
     * reference to the java primitive array containing all the
     * pixels of this array.
     * <p>
     * Will only be called after an open call, and before any close call.
     */
    boolean canMap();

    /**
     * Returns a single primitive array holding all the pixel data of
     * this array.  This should be a cheap operation, returning a 
     * reference to an existing array rather than doing work to 
     * generate one.
     * In the case of a writable accessor, making changes to the returned
     * primitive array will result in changes to the accessor pixel data.
     * In the case of an NDArray which is not writable, the effect of
     * making changes to the returned array is undefined; in particular
     * it may result in an exception.
     * <p>
     * Will only be called if canMap returns true, and only after an 
     * open call and before any close call.
     */
    Object getMapped();

    /**
     * Returns an object which can access the pixels of this ArrayImpl.
     * Each call to this method returns a new and independent AccessImpl,
     * with an offset initialised to 0 (the start of the array data).
     * <p>
     * This method will only be called after the sole call to open
     * and before the sole call to close.
     * <p>
     * This method will only be called more than once if the 
     * multipleAccess method returns true.
     * <p>
     * It is the responsibility of the caller to close the returned 
     * AccessImpl when it is no longer required; this enables resources 
     * it may hold to be released.
     *
     * @return  an accessor for the pixel data
     * @throws IOException  if there is an IO error
     */
    AccessImpl getAccess() throws IOException;

    /**
     * Shuts down this ArrayImpl for pixel access.
     * This method will be called no more than once.
     * No calls to getAccess, getMapped or open will be 
     * made after it is called.  If the user makes proper use
     * of the NDArray classes, it will be called after any AccessImpl 
     * objects and references to the mapped array are no longer required.
     * If the user misbehaves 
     * however it may not get called at all, so an effort should be
     * made to realease non-memory resources and flush buffers where
     * appropriate in the finalizer.
     * 
     * @throws IOException  if there is an IO error
     */
    void close() throws IOException;

}
