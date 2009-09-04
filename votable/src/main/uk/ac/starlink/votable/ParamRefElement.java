package uk.ac.starlink.votable;

import org.w3c.dom.Element;

/**
 * Object representing a PARAMref element.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Sep 2004
 */
public class ParamRefElement extends VOElement {

    ParamRefElement( Element base, VODocument doc ) {
        super( base, doc, "PARAMref" );
    }

    /**
     * Returns the PARAM element referenced by this PARAMref.
     * If this element has no ref attribute, or if it doesn't refer 
     * to a PARAM element
     * (neither of which ought to happen for a sensible document)
     * then null is returned.
     *
     * @return  referent PARAM
     */
    public ParamElement getParam() {
        Element ref = getOwnerDocument()
                     .getElementById( getAttribute( "ref" ) );
        return ref instanceof ParamElement ? (ParamElement) ref
                                           : null;
    }

    /**
     * Returns the value of the <tt>ucd</tt> attribute,
     * or <tt>null</tt> if there is none.
     * Note that (since VOTable 1.2) this may differ from the ucd of
     * the referenced PARAM.
     *
     * @return  the ucd string
     * @see     uk.ac.starlink.table.UCD
     */
    public String getUcd() {
        return hasAttribute( "ucd" ) ? getAttribute( "ucd" ) : null;
    }

    /**
     * Returns the value of the <tt>utype</tt> attribute,
     * or <tt>null</tt> if there is none.
     * Note that (since VOTable 1.2) this may differ from the utype of
     * the referenced PARAM.
     *
     * @return  the utype string
     */
    public String getUtype() {
        return hasAttribute( "utype" ) ? getAttribute( "utype" ) : null;
    }
}
