package uk.ac.starlink.votable.dom;

import org.w3c.dom.Notation;

public class DelegatingNotation extends DelegatingNode implements Notation {

    private final Notation base_;

    protected DelegatingNotation( Notation base, DelegatingDocument doc ) {
        super( base, doc );
        base_ = base;
    }

    public String getPublicId() {
        return base_.getPublicId();
    }

    public String getSystemId() {
        return base_.getSystemId();
    }
}
