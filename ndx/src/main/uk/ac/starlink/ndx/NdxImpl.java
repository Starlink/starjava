package uk.ac.starlink.ndx;

import java.io.IOException;
import javax.xml.transform.Source;
import uk.ac.starlink.array.NDArray;

/**
 * Interface for the implementation end of the Ndx bridge pattern.
 * If you have an <tt>NdxImpl</tt> you can make an {@link Ndx} out of it.
 * This is the basic interface via which NDX implementations provide
 * services to the {@link BridgeNdx} class.
 * BridgeNdx is intende to be the only client of this, class, and it
 * does the necessary validation of arguments before passing them to
 * NdxImpl, so that implementations of this interface can in general
 * assume that the arguments they receive make sense.
 *
 * @author Mark Taylor
 * @author Peter W. Draper
 * @see   Ndx
 * @see   BridgeNdx
 */
public interface NdxImpl {

    /**
     * Returns the bad bits mask used to mask the image/variance arrays
     * against the quality array.  A value of 0 (quality has no effect)
     * should be returned if no other value is available.
     *
     * @return   the bad bits mask
     */
    byte getBadBits();

    /**
     * Indicates whether a title component is available.
     *
     * @return   true if and only if getTitle will return a string
     */
    boolean hasTitle();

    /**
     * Gets the title component.
     * This method will only be called if {@link #hasTitle} returns true.
     *
     * @return  a string containing the Ndx title
     */
    String getTitle();

    /**
     * Indicates whether a WCS component is available.
     *
     * @return   true if and only if getWCS will return a representation of
     *           the world coordinate system of this Ndx
     */
    boolean hasWCS();

    /**
     * Gets an object representing the world coordinate systems of this Ndx.
     * This may be returned in one of a number of forms; currently
     * <ul>
     * <li> {@link uk.ac.starlink.ast.FrameSet} an AST FrameSet
     * <li> {@link javax.xml.transform.Source} a &lt;wcs&gt; element holding 
     *      an XML representation of an AST FrameSet
     * </ul>
     * This method will only be called if {@link #hasWCS} returns true.
     *
     * @return   a FrameSet or Element object representing the WCS
     */
    Object getWCS();

    /**
     * Indicates whether an extensions DOM is available.
     *
     * @return  true if and only if getEtc will return a DOM giving 
     *          extension information for this Ndx
     */
    boolean hasEtc();

    /**
     * Gets an XML Source holding the extension information.
     * This method will only be called if {@link #hasEtc} returns true.
     * The result should not in general be enclosed in an all-purpose
     * container element; if multiple top-level elements are contained
     * in the extension information, DOM users can use the 
     * {@link org.w3c.dom.DocumentFragment} interface.
     *
     * @return  the extension information in XML form
     */
    Source getEtc();

    /**
     * Gets an object responsible for providing the bulk data (image,
     * variance, quality) of this Ndx.
     *
     * @return  a BulkDataImpl which provides the array data
     */
    BulkDataImpl getBulkData();
}
