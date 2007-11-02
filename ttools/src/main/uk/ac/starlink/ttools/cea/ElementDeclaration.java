package uk.ac.starlink.ttools.cea;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
    private Collection attNames_;

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
     * Sets a list of permitted attribute names associated with this 
     * declaration.  This does not necessarily give all the permitted 
     * attributes, but it can be used to designate a list of attributes
     * which may be in doubt.
     *
     * @param   attNames  list of attribute names which are permitted 
     *          on this element
     */
    public void setAttributeNames( String[] attNames ) {
        attNames_ =
             Collections
            .unmodifiableCollection( new HashSet( Arrays.asList( attNames ) ) );
    }

    /**
     * Queries whether a given attribute is known to be permitted on
     * this element. 
     *
     * @param  attName  attribute name
     * @return  true iff attName is permitted
     */
    public boolean hasAttribute( String attName ) {
        return attNames_.contains( attName );
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
