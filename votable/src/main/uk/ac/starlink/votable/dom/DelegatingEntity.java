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

//DOM3     //
//DOM3     // Level 3 implementation.
//DOM3     //
//DOM3 
//DOM3     public String getInputEncoding() {
//DOM3         return base_.getInputEncoding();
//DOM3     }
//DOM3 
//DOM3     public String getXmlEncoding() {
//DOM3         return base_.getXmlEncoding();
//DOM3     }
//DOM3 
//DOM3     public String getXmlVersion() {
//DOM3         return base_.getXmlVersion();
//DOM3     }
}
