package uk.ac.starlink.votable.dom;

import org.w3c.dom.Entity;

public class DelegatingEntity extends DelegatingNode implements Entity {

    private final Entity base_;

    protected DelegatingEntity( Entity base, DelegatingDocument doc ) {
        super( base, doc );
        base_ = base;
    }

    //
    // Level 2 implementation.
    //

    public String getNotationName() {
        return base_.getNotationName();
    }

    public String getPublicId() {
        return base_.getPublicId();
    }

    public String getSystemId() {
        return base_.getSystemId();
    }

    //
    // Level 3 implementation.
    //

    public String getInputEncoding() {
        return base_.getInputEncoding();
    }

    public String getXmlEncoding() {
        return base_.getXmlEncoding();
    }

    public String getXmlVersion() {
        return base_.getXmlVersion();
    }
}
