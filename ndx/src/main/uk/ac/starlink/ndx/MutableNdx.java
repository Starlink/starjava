package uk.ac.starlink.ndx;

import org.w3c.dom.Node;
import uk.ac.starlink.array.NDArray;

/**
 * Extends the <tt>Ndx</tt> interface to provide methods for setting 
 * the data and metadata.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface MutableNdx extends Ndx {

    /**
     * Sets the Image component of this Ndx.  It is an error to set it
     * to the <tt>null</tt> value.
     *
     * @param   image  the new Image component
     * @throws  NullPointerException  if <tt>image</tt> is <tt>null</tt>
     */
    void setImage( NDArray image );

    /**
     * Sets the Variance component of this Ndx.  
     * If set to <tt>null</tt> the Ndx will be considered to have no Variance
     * component.
     * 
     * @param   variance  the new Variance component
     */
    void setVariance( NDArray variance );

    /**
     * Sets the Quality component of this Ndx.
     * The supplied NDArray must be of an integer type.
     * If set to <tt>null</tt> the Ndx will be considered to have no Quality
     * component.
     *
     * @param   quality  the new Quality component
     * @throws  IllegalArgumentException  if <tt>quality</tt> is an NDArray
     *          with a {@link uk.ac.starlink.array.Type} other than
     *          <tt>Type.BYTE</tt>, <tt>Type.SHORT</tt> or <tt>Type.INT</tt>
     */
    void setQuality( NDArray quality );

    /**
     * Sets the title component of this Ndx.
     *
     * @param   title  the new title.  
     *          If <tt>null</tt>, this Ndx will be considered to have no title
     */
    void setTitle( String title );

    /**
     * Sets the label component of this Ndx.
     *
     * @param   label  the new label.
     *          if <tt>null</tt>, this Ndx will be considered to have no label
     */
    void setLabel( String label );

    /**
     * Sets the units component of this Ndx.
     *
     * @param   units  the new units string.
     *          if <tt>null</tt>, this Ndx will be considered to have no 
     *          units component
     */
    void setUnits( String units );

    /**
     * Sets the bad bits mask for this Ndx.
     *
     * @param   badbits  the new bad bits mask
     */
    void setBadBits( int badbits );

    /**
     * Sets the WCS component of this Ndx.
     * This may be provided in one of a number of forms, currently
     * <ul>
     * <li> {@link uk.ac.starlink.ast.FrameSet} an AST FrameSet
     * <li> {@link javax.xml.transform.Source} a &lt;wcs&gt; element holding
     *      an XML representation of an AST FrameSet
     * </ul>
     * @param  wcs  an object representing the new WCS component
     *          If <tt>null</tt>, a default WCS will be used
     * @throws IllegalArgumentException  if <tt>wcs</tt> is not one of 
     *         the permitted types or otherwise fails to represent a legal
     *         WCS component
     */
    void setWCS( Object wcs );

    /**
     * Sets the Etc component of this Ndx.  If not <tt>null</tt>
     * the supplied Node should be a Document or Element of type &lt;etc&gt;.
     *
     * @param  etc  the new user-defined extensions component as a DOM node.
     *         If <tt>null</tt>, this Ndx will be considered to have no
     *         extensions
     * @throws  IllegalArgumentException  if <tt>etc</tt> is not an Element
     *          or Document of type &lt;etc&gt;
     */
    void setEtc( Node etc );

}
