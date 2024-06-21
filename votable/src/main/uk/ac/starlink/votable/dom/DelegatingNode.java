package uk.ac.starlink.votable.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Node implementation which simply delegates all its methods to a base
 * Node instance.
 *
 * @author   Mark Taylor (Starlink)
 * @since    10 Sep 2004
 */
public class DelegatingNode implements Node {

    private final Node base_;
    private DelegatingDocument doc_;

    //
    // Constructors.
    //

    protected DelegatingNode( Node base, DelegatingDocument doc ) {
        base_ = base;
        setDocument( doc );
    }

    DelegatingNode( Node base ) {
        base_ = base;
    }

    //
    // Package-private methods for manipulating DelegatingNode and its
    // subclasses.
    //

    /**
     * Sets the value returned by the getOwnerDocument method.
     * This must be the parent of the root element, otherwise there
     * will be trouble later.
     *
     * @param  doc   root document
     */
    void setDocument( DelegatingDocument doc ) {

        /* Make sure that the document this node belongs to (the delegating
         * document) is not the same as the owner document of the base node.
         * While you might possibly be able to come up with a scenario in
         * which that was useful, it almost certainly is a result of 
         * getting your nodes in a twist (confusing delegate with delegator). */
        if ( base_ instanceof DelegatingNode && 
             ((DelegatingNode) base_).doc_ == this ) {
            throw new IllegalArgumentException( 
                "Attempt to use delegating node as delegate in same document" );
        }
        doc_ = doc;
    }

    /**
     * Returns the base node for a given node within a given document.
     * This method may also throw a DOMException depending on the result,
     * since methods that want to call it are likely to want this to 
     * happen.
     *
     * If the supplied document is not the same as the document that the
     * node is from, a WRONG_DOCUMENT_ERR exception is thrown; this 
     * isn't exactly required, but such a check for this is likely to be
     * useful where this method is called.  If you don't want the check,
     * you can supply <code>doc</code> as null.
     *
     * @param   node   delegating node whose base is required
     * @param   doc    document which is expected to own <code>node</code>
     * @throws  DOMException  <dl>
     *          <dt>WRONG_DOCUMENT_ERR
     *          <dd>if the resulting node is from a document other than 
     *              <code>doc</code>
     *          </dl>
     */
    static Node getBaseNode( Node node, Document doc ) {
        if ( node == null ) {
            return null;
        }
        else if ( node instanceof DelegatingNode ) {
            DelegatingNode dnode = (DelegatingNode) node;
            if ( dnode.doc_ == doc || doc == null ) { 
                return dnode.base_;
            }
            else {
                throw new DOMException( DOMException.WRONG_DOCUMENT_ERR,
                                        "Wrong document" );
            }
        }
        else {
            throw new DOMException( DOMException.WRONG_DOCUMENT_ERR,
                                    "Not a delegating node " + node + " (" +
                                    node.getClass().getName() + ")" );
        }
    }

    //
    // DOM Level 2 implementation.
    //

    public String getNodeName() {
        return base_.getNodeName();
    }

    public String getNodeValue() {
        return base_.getNodeValue();
    }

    public void setNodeValue( String nodeValue ) {
        base_.setNodeValue( nodeValue );
    }

    public short getNodeType() {
        return base_.getNodeType();
    }

    public Node getParentNode() {

        /* Normally, we return the delegator of the base node's parent.
         * However, as a special case, if the node corresponding to the
         * root document is required, the Document instance in use by
         * this DOM is returned.  It seems that the root node has to be
         * identical to the Document (at least for certain XPath
         * implementations). */
        Node baseParent = base_.getParentNode();
        if ( baseParent != null &&
             baseParent == base_.getOwnerDocument() ) {
            return doc_;
        }
        else {
            return doc_.getDelegator( baseParent );
        }
    }

    public NodeList getChildNodes() {
        return doc_.createDelegatingNodeList( base_.getChildNodes() );
    }

    public Node getFirstChild() {
        return doc_.getDelegator( base_.getFirstChild() );
    }

    public Node getLastChild() {
        return doc_.getDelegator( base_.getLastChild() );
    }

    public Node getPreviousSibling() {
        return doc_.getDelegator( base_.getPreviousSibling() );
    }

    public Node getNextSibling() {
        return doc_.getDelegator( base_.getNextSibling() );
    }

    public NamedNodeMap getAttributes() {
        return doc_.createDelegatingNamedNodeMap( base_.getAttributes() );
    }

    public Document getOwnerDocument() {
        return this == doc_ ? null : doc_;
    }

    public Node insertBefore( Node newChild, Node refChild ) {
        Node newChildBase = DelegatingNode.getBaseNode( newChild, doc_ );
        Node refChildBase = DelegatingNode.getBaseNode( refChild, null );
        return doc_.getDelegator( base_.insertBefore( newChildBase, 
                                                      refChildBase ) );
    }

    public Node replaceChild( Node newChild, Node oldChild ) {
        Node newChildBase = DelegatingNode.getBaseNode( newChild, doc_ );
        Node oldChildBase = DelegatingNode.getBaseNode( oldChild, null );
        return doc_.getDelegator( base_.replaceChild( newChildBase, 
                                                      oldChildBase ) );
    }

    public Node removeChild( Node oldChild ) {
        Node oldChildBase = DelegatingNode.getBaseNode( oldChild, null );
        return doc_.getDelegator( base_.removeChild( oldChildBase ) );
    }

    public Node appendChild( Node newChild ) {
        Node newChildBase = DelegatingNode.getBaseNode( newChild, doc_ );
        return doc_.getDelegator( base_.appendChild( newChildBase ) );
    }

    public boolean hasChildNodes() {
        return base_.hasChildNodes();
    }

    public Node cloneNode( boolean deep ) {
        return doc_.getDelegator( base_.cloneNode( deep ) );
    }

    public void normalize() {
        base_.normalize();
    }

    public boolean isSupported( String feature, String version ) {
        return base_.isSupported( feature, version );
    }

    public String getNamespaceURI() {
        return base_.getNamespaceURI();
    }

    public String getPrefix() {
        return base_.getPrefix();
    }

    public void setPrefix( String prefix ) {
        base_.setPrefix( prefix );
    }

    public String getLocalName() {
        return base_.getLocalName();
    }

    public boolean hasAttributes() {
        return base_.hasAttributes();
    }

    //
    // DOM Level 3 implementation.
    //

    public String getBaseURI() {
        return base_.getBaseURI();
    }

    public short compareDocumentPosition( Node other ) {
        if ( other instanceof DelegatingNode ) {
            return base_
                  .compareDocumentPosition( DelegatingNode
                                           .getBaseNode( other,
                                                         null ) );
        }
        else {
            throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
                                    "Wrong type of node" );
        }
    }

    public String getTextContent() {
        return base_.getTextContent();
    }

    public void setTextContent( String textContent ) {
        base_.setTextContent( textContent );
    }

    public String lookupPrefix( String namespaceURI ) {
        return base_.lookupPrefix( namespaceURI );
    }

    public boolean isDefaultNamespace( String namespaceURI ) {
        return base_.isDefaultNamespace( namespaceURI );
    }

    public String lookupNamespaceURI( String prefix ) {
        return base_.lookupNamespaceURI( prefix );
    }

    public boolean isSameNode( Node other ) {
        if ( other instanceof DelegatingNode ) {
            return base_.isSameNode( DelegatingNode
                                    .getBaseNode( other, doc_ ) );
        }
        else {
            return false;
        }
    }

    public boolean isEqualNode( Node other ) {
        return base_.isEqualNode( other );
    }

    public Object getFeature( String feature, String version ) {
        return base_.getFeature( feature, version );
    }

    public Object setUserData( String key, Object data, 
                               org.w3c.dom.UserDataHandler handler ) {
        return base_.setUserData( key, data, handler );
    }

    public Object getUserData( String key ) {
        return base_.getUserData( key );
    }
}
