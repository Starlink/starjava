package uk.ac.starlink.votable.dom;

import org.w3c.dom.Entity;

public class DelegatingEntity extends DelegatingNode implements Entity {

    private final Entity base_;

    protected DelegatingEntity( Entity base, DelegatingDocument doc ) {
        super( base, doc );
        base_ = base;
    }

    public String getNotationName() {
        return base_.getNotationName();
    }

    public String getPublicId() {
        return base_.getPublicId();
    }

    public String getSystemId() {
        return base_.getSystemId();
    }
}
