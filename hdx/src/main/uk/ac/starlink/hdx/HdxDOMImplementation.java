// $Id$

package uk.ac.starlink.hdx;

import org.w3c.dom.*;

/**
 * Provides methods for performing operations that are independent of
 * any particular instance of the document object model.
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

    public boolean hasFeature(String feature, 
                              String version) {
        return false;
    }
    
    public DocumentType createDocumentType(String qualifiedName, 
                                           String publicId, 
                                           String systemId)
            throws DOMException {
        throw new DOMException (DOMException.NOT_SUPPORTED_ERR,
                                "DocumentType nodes not supported");
    }
    
    public Document createDocument(String namespaceURI, 
                                   String qualifiedName, 
                                   DocumentType doctype)
            throws DOMException {
        return (Document)new HdxDocument();
    }
}
