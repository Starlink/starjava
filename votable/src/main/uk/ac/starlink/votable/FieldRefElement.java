package uk.ac.starlink.votable;

import org.w3c.dom.Element;

/**
 * Object representing a FIELDref element.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Sep 2004
 */
public class FieldRefElement extends VOElement {

    FieldRefElement( Element base, VODocument doc ) {
        super( base, doc, "FIELDref" );
    }

    /**
     * Returns the FIELD element referenced by this FIELDref.
     * If this element has no ref attribute, or if it doesn't refer
     * to a FIELD element
     * (neither of which ought to happen for a sensible document)
     * then null is returned.
     *
     * @return  referent FIELD
     */
    public FieldElement getField() {
        return (FieldElement) getReferencedElement( "ref", "FIELD" );
    }

    /**
     * Returns the value of the <code>ucd</code> attribute,
     * or <code>null</code> if there is none.
     * Note that (since VOTable 1.2) this may differ from the ucd of
     * the referenced FIELD.
     *
     * @return  the ucd string
     * @see     uk.ac.starlink.table.UCD
     */
    public String getUcd() {
        return hasAttribute( "ucd" ) ? getAttribute( "ucd" ) : null;
    }

    /**
     * Returns the value of the <code>utype</code> attribute,
     * or <code>null</code> if there is none.
     * Note that (since VOTable 1.2) this may differ from the utype of
     * the referenced FIELD.
     *
     * @return  the utype string
     */
    public String getUtype() {
        return hasAttribute( "utype" ) ? getAttribute( "utype" ) : null;
    }
}
