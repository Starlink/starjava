package uk.ac.starlink.ndx;

import java.io.IOException;
import org.w3c.dom.Node;

/**
 * Extends the <tt>Ndx</tt> interface to provide methods for setting 
 * the data and metadata.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface MutableNdx extends Ndx {

    /**
     * Sets the title component of this Ndx.
     *
     * @param   title  the new title.  
     *          If <tt>null</tt>, this Ndx will be considered to have no title
     */
    void setTitle( String title );

    /**
     * Sets the bad bits mask for this Ndx.
     *
     * @param   badbits  the new bad bits mask
     */
    void setBadBits( byte badbits );

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
     * @throws IOException  if there is an error converting wcs into a FrameSet
     * @throws IllegalArgumentException  if <tt>wcs</tt> is not one of 
     *         the permitted types
     */
    void setWCS( Object wcs ) throws IOException;

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

    /**
     * Sets the bulk data (array compnents) for this Ndx.
     *
     * @param  the new bulk data implementation
     */
    void setBulkData( BulkDataImpl datimp );
    
}
