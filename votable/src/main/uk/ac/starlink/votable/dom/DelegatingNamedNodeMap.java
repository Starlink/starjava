package uk.ac.starlink.votable.dom;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

class DelegatingNamedNodeMap implements NamedNodeMap {

    private final NamedNodeMap base_;
    private final DelegatingDocument doc_;

    public DelegatingNamedNodeMap( NamedNodeMap base, DelegatingDocument doc ) {
        base_ = base;
        doc_ = doc;
    }

    public Node getNamedItem( String name ) {
        return doc_.getDelegator( base_.getNamedItem( name ) );
    }

    public Node setNamedItem( Node arg ) {
        return doc_.getDelegator( base_.setNamedItem( arg ) );
    }

    public Node removeNamedItem( String name ) {
        return doc_.getDelegator( base_.removeNamedItem( name ) );
    }

    public Node item( int index ) {
        return doc_.getDelegator( base_.item( index ) );
    }

    public int getLength() {
        return base_.getLength();
    }

    public Node getNamedItemNS( String namespaceURI, String localName ) {
        return doc_.getDelegator( base_.getNamedItemNS( namespaceURI,
                                                        localName ) );
    }

    public Node setNamedItemNS( Node arg ) {
        return doc_.getDelegator( base_.setNamedItemNS( arg ) );
    }

    public Node removeNamedItemNS( String namespaceURI, String localName ) {
        return doc_.getDelegator( base_.removeNamedItemNS( namespaceURI,
                                                           localName ) );
    }

}
