package uk.ac.starlink.ndx;

import java.io.IOException;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.Requirements;

/**
 * Provides bulk data services for BridgeNdx. 
 * It supplies the image, variance and quality bulk data in two ways:
 * as individual {@link NDArray}s to access the image, variance and
 * quality parts separately, and as an {@link NdxAccess} object which
 * provides unified access to all the arrays.
 * <p>
 * Two implementations of <tt>BulkDataImpl</tt> are provided:
 * {@link ArraysBulkDataImpl} is constructed from the <tt>NDArray</tt>s
 * so it can serve them directly and and builds the <tt>NdxAccess</tt> 
 * on top of them, while
 * {@link AccessBulkDataImpl} is constructed from an {@link NdxAccessMaker}
 * which knows how to provide <tt>NdxAccess</tt> objects, and it builds
 * the <tt>NDArray</tt>s from from them.
 *
 * @author   Mark Taylor (Starlink)
 * @see  ArraysBulkDataImpl
 * @see  AccessBulkDataImpl
 */
public interface BulkDataImpl {

    /**
     * Indicates whether variance array data is present.
     *
     * @return   true if and only if variance data is available
     */
    boolean hasVariance();

    /**
     * Indicates whether quality array data is present.
     *
     * @return   true if and only if quality data is present
     */
    boolean hasQuality();

    /**
     * Gets an NDArray containing the image data.
     *
     * @return  image NDArray
     */
    NDArray getImage();

    /**
     * Gets an NDArray containing the variance data.
     * This method will only be called if {@link #hasVariance} returns true.
     *
     * @return  variance NDArray
     */
    NDArray getVariance();

    /**
     * Gets an NDArray containing the quality data.
     * This method will only be called if {@link #hasQuality} returns true.
     *
     * @return  quality NDArray
     */
    NDArray getQuality();

    /**
     * Gets an NdxAccess object for unified access to the array data held by 
     * this object.
     * As well as ensuring that access to the array components is uniform,
     * this method may be used to acquire a view of the data with
     * given characteristics by using a non-null Requirements object.
     *
     * @param   req    an object indicating requirements (type, shape,
     *                 pixel sequence) for the returned access object.
     *                 May be null, in which case the
     *                 natural characteristics of this object will be used.
     * @param   wantImage     whether access to the image array is required.
     *                        If true,
     *                        all read/write operations on the returned
     *                        object will be performed on the image array
     *                        in tandem with the other arrays.
     *                        If false image pixels will not be provided
     *                        by the returned accessor
     * @param   wantVariance  whether access to the variance array is required.
     *                        If true, and if a variance array exists,
     *                        all read/write operations on the returned
     *                        object will be performed
     *                        on the image and the variance array in tandem.
     *                        If false any existing variance array
     *                        will be ignored.
     * @param   wantQuality   whether access to the quality array is required.
     *                        If true, and a quality array exists,
     *                        all read/write operations on the returned
     *                        object will be performed
     *                        on the image and the quality array in tandem.
     *                        If false, any existing quality array will be
     *                        used to convert image (and variance, if present)
     *                        pixels to the bad value according to this
     *                        Ndx's badBits mask.  Note the value of the
     *                        badbits mask at the time this object is obtained
     *                        is the one used for this purpose.
     * @throws  IOException   if an I/O error occurs
     * @return   an object for accessing this Ndx's array data
     */
    NdxAccess getAccess( Requirements req, boolean wantImage, 
                         boolean wantVariance, boolean wantQuality, 
                         byte badbits ) throws IOException;
}
