package uk.ac.starlink.ndx;

import java.io.IOException;
import javax.xml.transform.Source;
import org.w3c.dom.Element;
import uk.ac.starlink.array.Requirements;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.ast.FrameSet;

/**
 * N-dimensional astronomical data.
 * An Ndx represents an N-dimensional hypercuboid of pixels 
 * (the <i>image</i> data), plus associated auxiliary information,
 * in particular an optional array of pixel variances, and of 
 * pixel quality flags.  Metadata such as history information and
 * coordinate system information may also be included, as well as
 * user-defined extension data of unrestricted scope, represented in XML.
 *
 * @author Norman Gray
 * @author Mark Taylor
 * @author Peter W. Draper.
 * @version $Id$
 */
public interface Ndx {

    /**
     * Returns an object via which this Ndx's array data can be accessed.
     * This method should normally be used as an intermediary between
     * user code and the array data of the Ndx.
     * As well as ensuring that access to the array components is uniform,
     * this method may be used to acquire a view of the data with 
     * given characteristics by using a non-null Requirements object.
     *
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
     * @throws   IOException  if there is an I/O error
     * @return   an object for accessing this Ndx's array data
     */
    NdxAccess getAccess( Requirements req, boolean wantImage, 
                         boolean wantVariance, boolean wantQuality )
        throws IOException;

    /**
     * Returns the title of this Ndx.
     *
     * @return  the title, or null if it has none
     */
    String getTitle();

    /**
     * Gets the value of the badBits mask.
     * This value is used in conjunction
     * with the quality array to determine which pixels are bad; 
     * a pixel is bad if the logical AND of its quality value and the
     * bad bits mask is not zero; hence a value of zero has no effect.
     * Has no effect if there is no quality array.
     * 
     * @return   the bad bits mask
     */
    byte getBadBits();

    /**
     * Returns the image component of this NDX.
     *
     * @return   the NDArray representing the image component
     */
    NDArray getImage();

    /**
    * Returns the variance component of this NDX.
    *
    * @return   an NDArray representing the variance component, 
    *           or null if there is no such component.
    */
    NDArray getVariance();

    /**
     * Returns the quality component of this NDX.
     *
     * @return  an NDArray of byte type representing the quality component, 
     *          or null if there is no such component.
     */
    NDArray getQuality();

    /**
     * Get the world coordinate system of the NDX as an AST FrameSet.
     * See the JNIAST documentation about how to manipulate this.
     *
     * @return the AST FrameSet or null if none are available. 
     */
    FrameSet getWCS();

    /** Indicates whether there is a variance component.
     *
     * @return   true if {@link #getVariance} will not return null
     */
    boolean hasVariance();

    /**
     * Indicates whether there is a quality component. 
     *
     * @return   true if {@link #getQuality} will not return null
     */
    boolean hasQuality();

    /**
     * Indicates whether there is a title component.
     *
     * @return   true if {@link #getTitle} will not return null
     */
    boolean hasTitle();

    /**
     * Find out if the NDX contains a world coordinate system.
     *
     * @return true if a world coordinate system is available.
     */
    boolean hasWCS();

    /**
     * Indicates whether this Ndx represents a persistent object.
     * If this returns true, then the array components in the DOM 
     * generated by the toDOM method all contain URLs referencing 
     * genuine resources.  If false, then this Ndx is in some sense 
     * virtual, and one or more of the array elements in the the DOM 
     * generated by toDOM will reference phantom resources.
     *
     * @return   true if a DOM capable of containing the full state of
     *           this Ndx can be generated
     */
    boolean isPersistent();
 
    /**
     * Generates an XML view of this Ndx object as a 
     * {@link javax.xml.transform.Source}.  Note that the
     * array components (image, variance, quality) of this Ndx will only
     * be recoverable from the returned DOM in the case that the 
     * {@link #isPersistent} method returns true.
     *
     * @return   an XML Source representation of this Ndx
     */
    Source toXML();

}
