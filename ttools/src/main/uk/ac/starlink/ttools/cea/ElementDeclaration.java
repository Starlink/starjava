package uk.ac.starlink.ttools.cea;

import uk.ac.starlink.util.XmlWriter;

/**
 * Encapsulates the declaration of an XML element.
 *
 * @author   Mark Taylor
 * @since    1 Nov 2007
 */
public class ElementDeclaration {

    private final String elName_;
    private final String attList_;

    /**
     * Constructs a declaration with a name but no attributes.
     *
     * @param  elName  element name (possibly prefixed)
     */
    public ElementDeclaration( String elName ) {
        this( elName, "" );
    }

    /**
     * Constructs a declaration with a name and attributes.
     * The supplied attribute list is exactly as it will be inserted into
     * the output, so it must start with a space (if it's not empty) and
     * any relevant escaping must have been done.
     *
     * @param  elName  element name (possibly prefixed)
     * @param  attList attribute list string
     */
    public ElementDeclaration( String elName, String attList ) {
        elName_ = elName;
        attList_ = attList;
    }

    /**
     * Returns the element name.
     *
     * @return  element name, possibly prefixed
     */
    public String getElementName() {
        return elName_;
    }

    /**
     * Returns the element attribute list.  It appears exactly as it should
     * be inserted into the output, so it should start with a space
     * (if it's not empty) and any relevant escaping should have been done.
     *
     * @return   element attribute list
     */
    public String getElementAttributes() {
        return attList_;
    }

    /**
     * Convenience method to create an element with an attribute list
     * defining a default namespace for this element and its descendents.
     *
     * @param   elName  element name, possibly prefixed
     * @param   ns    default namespace URI
     * @return   new element declaration
     */
    public static ElementDeclaration createNamespaceElement( String elName,
                                                             String ns ) {
        String atts = XmlWriter.formatAttribute( "xmlns", ns );
        return new ElementDeclaration( elName, atts );
    }
}
