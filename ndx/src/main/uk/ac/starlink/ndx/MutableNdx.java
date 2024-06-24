package uk.ac.starlink.ndx;

import org.w3c.dom.Node;
import uk.ac.starlink.array.NDArray;

/**
 * Extends the <code>Ndx</code> interface to provide methods for setting 
 * the data and metadata.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface MutableNdx extends Ndx {

    /**
     * Sets the Image component of this Ndx.  It is an error to set it
     * to the <code>null</code> value.
     *
     * @param   image  the new Image component
     * @throws  NullPointerException  if <code>image</code> is <code>null</code>
     */
    void setImage( NDArray image );

    /**
     * Sets the Variance component of this Ndx.  
     * If set to <code>null</code> the Ndx will be considered
     * to have no Variance component.
     * 
     * @param   variance  the new Variance component
     */
    void setVariance( NDArray variance );

    /**
     * Sets the Quality component of this Ndx.
     * The supplied NDArray must be of an integer type.
     * If set to <code>null</code> the Ndx will be considered to have
     * no Quality component.
     *
     * @param   quality  the new Quality component
     * @throws  IllegalArgumentException  if <code>quality</code> is an NDArray
     *          with a {@link uk.ac.starlink.array.Type} other than
     *          <code>Type.BYTE</code>, <code>Type.SHORT</code>
     *          or <code>Type.INT</code>
     */
    void setQuality( NDArray quality );

    /**
     * Sets the title component of this Ndx.
     *
     * @param   title  the new title.  
     *          If <code>null</code>, this Ndx will be considered to have
     *          no title
     */
    void setTitle( String title );

    /**
     * Sets the label component of this Ndx.
     *
     * @param   label  the new label.
     *          if <code>null</code>, this Ndx will be considered to have
     *          no label
     */
    void setLabel( String label );

    /**
     * Sets the units component of this Ndx.
     *
     * @param   units  the new units string.
     *          if <code>null</code>, this Ndx will be considered to have no 
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
     *          If <code>null</code>, a default WCS will be used
     * @throws IllegalArgumentException  if <code>wcs</code> is not one of 
     *         the permitted types or otherwise fails to represent a legal
     *         WCS component
     */
    void setWCS( Object wcs );

    /**
     * Sets the Etc component of this Ndx.  If not <code>null</code>
     * the supplied Node should be a Document or Element of type &lt;etc&gt;.
     *
     * @param  etc  the new user-defined extensions component as a DOM node.
     *         If <code>null</code>, this Ndx will be considered to have no
     *         extensions
     * @throws  IllegalArgumentException  if <code>etc</code> is not an Element
     *          or Document of type &lt;etc&gt;
     */
    void setEtc( Node etc );

}
