package uk.ac.starlink.ndx;

import java.io.IOException;
import uk.ac.starlink.array.ArrayDescription;
import uk.ac.starlink.array.Requirements;

/**
 * Interface for providing NdxAccess objects.
 * Objects implementing this interface can be used as the basis of an
 * Ndx via the {@link AccessBulkDataImpl} class.
 * 
 * @author   Mark Taylor (Starlink)
 */
public interface NdxAccessMaker extends ArrayDescription {

    /**
     * Indicates whether this object can provide variance data.
     *
     * @return   true if and only if variance data is available
     */
    boolean hasVariance();

    /**
     * Indicats whether this object can provide quality data.
     *
     * @return   true if and only if quality data is available
     */
    boolean hasQuality();

    /**
     * Indicates whether this object can provide multiple NdxAccess objects
     *
     * @return   true if multiple invocations of {@link #getAccess}
     *           may be made
     */
    boolean multipleAccess();

    /**
     * Returns an accessor capable of accessing the array data.
     * @param   req    an object indicating requirements (type, shape,
     *                 pixel sequence) for the returned access object.
     *                 May be null, in which case the image array's
     *                 natural characteristics will be used.
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
     * @param  badbits   the bad bit value with which to mask the quality
     *                   array when modifying the image and variance arrays
     */
    NdxAccess getAccess( Requirements req, boolean wantImage, 
                         boolean wantVariance, boolean wantQuality,
                         byte badbits ) throws IOException;
}
