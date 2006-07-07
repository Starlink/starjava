package uk.ac.starlink.votable.dom;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class DelegatingNodeList implements NodeList {

    private final NodeList base_;
    private final DelegatingDocument doc_;

    public DelegatingNodeList( NodeList base, DelegatingDocument doc ) {
        base_ = base;
        doc_ = doc;
    }

    public int getLength() {
        return base_.getLength();
    }

    public Node item( int index ) {
        return doc_.getDelegator( base_.item( index ) );
    }
}
