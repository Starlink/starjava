package uk.ac.starlink.votable.dom;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DelegatingAttr extends DelegatingNode implements Attr {

    private final Attr base_;
    private final DelegatingDocument doc_;

    protected DelegatingAttr( Attr base, DelegatingDocument doc ) {
        super( base, doc );
        base_ = base;
        doc_ = doc;
    }

    public String getName() {
        return base_.getName();
    }

    static Attr getBaseAttr( Attr attr, Document doc ) {
        return (Attr) DelegatingNode.getBaseNode( attr, doc );
    }

    public boolean getSpecified() {
        return base_.getSpecified();
    }

    public String getValue() {
        return base_.getValue();
    }

    public void setValue( String value ) {
        base_.setValue( value );
    }

    public Element getOwnerElement() {
        return (Element) doc_.getDelegator( base_.getOwnerElement() );
    }
}
