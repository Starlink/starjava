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
     * May only be called if {@link #hasTitle} returns <tt>true</tt>.
     *
     * @return  the title
     * @throws  UnsupportedOperationException  if <tt>hasTitle</tt> 
     *          returns <tt>false</tt>
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
    * May only be called if {@link #hasVariance} returns <tt>true</tt>.
    *
    * @return   an NDArray representing the variance component, 
    * @throws   UnsupportedOperationException  if <tt>hasVariance</tt>
    *           returns <tt>false</tt>
    */
    NDArray getVariance();

    /**
     * Returns the quality component of this NDX.
     * May only be called if {@link #hasQuality} returns <tt>true</tt>.
     *
     * @return  an NDArray of byte type representing the quality component, 
     * @throws  UnsupportedOperationException  if <tt>hasQuality</tt>
     *          returns <tt>false</tt>
     */
    NDArray getQuality();

    /**
     * Returns the XML containing extension information for this NDX.
     * The base element of the returned Source is an element of type 
     * <tt>&lt;etc&gt;</tt> which contains an element for each extension.
     * May only be called if {@link #hasEtc} returns <tt>true</tt>.
     *
     * @return  an XML Source containing any user-defined extension information.
     * @throws  UnsupportedOperationException  if <tt>hasEtc</tt>
     *          returns <tt>false</tt>
     * @see     uk.ac.starlink.util.SourceReader
     */
    Source getEtc();

    /**
     * Get the world coordinate system of the NDX as an AST <tt>FrameSet</tt>.
     * May only be called if {@link #hasWCS} returns <tt>true</tt>.
     *
     * @return the AST FrameSet representing the world coordinate system
     *         information
     * @throws UnsupportedOperationException if <tt>hasWCS</tt>
     *         returns <tt>false</tt>
     */
    FrameSet getWCS();

    /** Indicates whether there is a variance component.
     *
     * @return   true if {@link #getVariance} may be called
     */
    boolean hasVariance();

    /**
     * Indicates whether there is a quality component. 
     *
     * @return   true if {@link #getQuality} may be called
     */
    boolean hasQuality();

    /**
     * Indicates whether there is a title component.
     *
     * @return   true if {@link #getTitle} may be called
     */
    boolean hasTitle();

    /**
     * Find out if the NDX contains user-defined extension information.
     *
     * @return true if {@link #getEtc} may be called
     */
    boolean hasEtc();

    /**
     * Find out if the NDX contains a world coordinate system.
     *
     * @return true if {@link getWCS} may be called
     */
    boolean hasWCS();

    /**
     * Indicates whether this Ndx represents a persistent object.
     * If this returns true, then the array components in the XML source
     * generated by the {@link #toXML} method all contain URLs referencing 
     * genuine resources.  If false, then this Ndx is in some sense 
     * virtual, and one or more of the array elements in the the XML
     * generated by <tt>toXML</tt> will reference phantom resources.
     *
     * @return   true if an XML representation capable of containing the 
     *           full state of this Ndx can be generated
     */
    boolean isPersistent();
 
    /**
     * Generates an XML view of this Ndx object as a 
     * {@link javax.xml.transform.Source}.  Note that the
     * array components (image, variance, quality) of this Ndx will only
     * be recoverable from the returned XML in the case that the 
     * {@link #isPersistent} method returns true.
     *
     * @return   an XML Source representation of this Ndx
     * @see     uk.ac.starlink.util.SourceReader
     */
    Source toXML();

}
