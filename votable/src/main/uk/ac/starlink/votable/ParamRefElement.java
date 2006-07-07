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
        return ref.getTagName().equals( "PARAM" ) ? (ParamElement) ref
                                                  : null;
    }
}
