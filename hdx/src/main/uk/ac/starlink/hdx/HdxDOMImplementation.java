// $Id$

package uk.ac.starlink.hdx;

import org.w3c.dom.*;

/**
 * Provides methods for performing operations that are independent of
 * any particular instance of the document object model.
 *
 * <p>Note that only DOM Level 2 methods are currently implemented.
 * If this class is built using JDK1.5, then the DOM Level 3 methods
 * will be present, but they do not implement the functionality 
 * defined by the DOM Level 3 specification (mostly they throw 
 * NOT_SUPPORTED_ERR type DOMExceptions).
 */
public class HdxDOMImplementation
        implements DOMImplementation {

    private static HdxDOMImplementation instance;

    /** Private constructor for singleton instance */
    private HdxDOMImplementation() {}

    public static HdxDOMImplementation getInstance() {
        if (instance == null)
            instance = new HdxDOMImplementation();
        return instance;
    }

    /** Test if the DOM implementation implements a specific feature.
     *
     * <p>At present, this implementation does not claim conformance
     * with <em>any</em> modules in the DOM specification (see section <a
     * href="http://www.w3.org/TR/DOM-Level-2-Core/introduction.html#ID-Conformance" >Conformance</a>
     *  in the DOM2 specification).  It is likely largely conformant
     *  with the Core module at least, but this has not been checked.
     *
     * @param feature the name of the feature to test
     * @param version this is the version number of the feature to test
     * @return false in all cases
     */
    public boolean hasFeature(String feature,
                              String version) {
        return false;
    }

//DOM3    /** Get the implementation of a specific feature.
//DOM3     *  <p>
//DOM3     * At present there are none of these. See {@link #hasFeature}.
//DOM3     *
//DOM3     * @param feature the name of the feature to test
//DOM3     * @param version this is the version number of the feature to test
//DOM3     * @return null for all cases
//DOM3     */
//DOM3    public Object getFeature(String feature,
//DOM3                             String version) {
//DOM3        return null;
//DOM3    }

    /**
     * Does <em>not</em> create an empty <code>DocumentType</code>
     * node.  This method is not implemented, and always throws an
     * exception.
     *
     * @throws DOMException NOT_SUPPORTED_ERR: in all cases.
     */
    public DocumentType createDocumentType(String qualifiedName,
                                           String publicId,
                                           String systemId)
            throws DOMException {
        throw new DOMException (DOMException.NOT_SUPPORTED_ERR,
                                "DocumentType nodes not supported");
    }

    /**
     * Creates a DOM Document object of the specified type with its
     * document element.
     *
     * <p>The parameters and exceptions documented below are copied
     * from the {@link org.w3c.dom.DOMImplementation} documentation.
     * However, the current implementation of this method does not
     * implement the <code>namespaceURI</code> and
     * <code>doctype</code> parameters.  For future compatibility,
     * both these parameters should be <code>null</code>, and
     * parameter <code>qualifiedName</code> should be the
     * (<em>unqualified</em>) name of the
     * document element, though this is not currently checked.
     *
     * @param namespaceURI the namespace URI of the document element
     * to create
     *
     * @param qualifiedName the qualified name of the document
     * element to be created
     *
     * @param doctype the type of document to be created or null. When
     * doctype is not null, its <code>Node.ownerDocument</code>
     * attribute is set to the document being created.
     *
     * @return a new Document object
     *
     * @throws DOMException INVALID_CHARACTER_ERR: Raised if the
     * specified qualified name contains an illegal character.
     *
     * @throws DOMException NAMESPACE_ERR: Raised if the qualifiedName
     * is malformed, if the qualifiedName has a prefix and the
     * namespaceURI is null, or if the qualifiedName has a prefix that
     * is <code>xml</code> and the namespaceURI is different from
     * <code<http://www.w3.org/XML/1998/namespace</code> , or if the
     * DOM implementation does not support the "XML" feature but a
     * non-null namespace URI was provided, since namespaces were
     * defined by XML.
     *
     * @throws DOMException WRONG_DOCUMENT_ERR: Raised if doctype has
     * already been used with a different document or was created from
     * a different implementation.
     *
     * @throws NOT_SUPPORTED_ERR: May be raised by DOM implementations
     * which do not support the "XML" feature, if they choose not to
     * support this method. Other features introduced in the future,
     * by the DOM WG or in extensions defined by other groups, may
     * also demand support for this method; please consult the
     * definition of the feature to see if it requires this method
     */
    public Document createDocument(String namespaceURI,
                                   String qualifiedName,
                                   DocumentType doctype)
            throws DOMException {
        if (namespaceURI != null
            || doctype != null
            || qualifiedName == null
            || qualifiedName.indexOf(':') >= 0) {
            StringBuffer sb = new StringBuffer();
            sb.append("HdxDOMImplementation(");
            sb.append(namespaceURI);
            sb.append(',');
            sb.append(qualifiedName);
            sb.append(',');
            sb.append(doctype);
            sb.append("): unsupported arguments, should be (null,name,null)");
            throw new DOMException
                    (DOMException.NOT_SUPPORTED_ERR, sb.toString());
        }
        return (Document)new HdxDocument();
    }
}
