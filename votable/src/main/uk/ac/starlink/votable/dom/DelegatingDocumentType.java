package uk.ac.starlink.votable.dom;

import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class DelegatingDocumentType extends DelegatingNode
        implements DocumentType {

    private final DocumentType base_;
    private final DelegatingDocument doc_;

    protected DelegatingDocumentType( DocumentType base,
                                      DelegatingDocument doc ) {
        super( base, doc );
        base_ = base;
        doc_ = doc;
    }

    public String getName() {
        return base_.getName();
    }

    public NamedNodeMap getEntities() {
        return doc_.createDelegatingNamedNodeMap( base_.getEntities() );
    }

    public NamedNodeMap getNotations() {
        return doc_.createDelegatingNamedNodeMap( base_.getNotations() );
    }

    public String getPublicId() {
        return base_.getPublicId();
    }

    public String getSystemId() {
        return base_.getSystemId();
    }

    public String getInternalSubset() {
        return base_.getInternalSubset();
    }
}
