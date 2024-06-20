// $Id$

package uk.ac.starlink.hdx;

import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * An node in an HDX DOM.
 *
 * <p>This implements the W3C {@link Node} interface, with the
 * addition of the {@link #getNodeObject} method.
 *
 * <p>Package private.
 *
 * <p>Note that only DOM Level 2 methods are currently implemented.
 * If this class is built using JDK1.5, then the DOM Level 3 methods
 * will be present, but they do not implement the functionality
 * defined by the DOM Level 3 specification (mostly they throw
 * NOT_SUPPORTED_ERR type DOMExceptions).
 *
 * @see HdxDocument
 */
class HdxNode
        implements Node, Cloneable {

    /*
     * Between them, these following variables implement a
     * doubly-linked list of children.
     */
    private HdxNode parent;
    private HdxNode firstChild;
    private HdxNode nextSibling;
    private HdxNode previousSibling;

    private short nodeType;

    private Document ownerDocument;

    private static java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger( "uk.ac.starlink.hdx" );

    /**
     * Creates a new Node.  Protected constructor should be called
     * from extending classes only.
     */
    protected HdxNode(short nodeType, Document ownerDocument) {
        if (nodeType != Node.DOCUMENT_NODE && ownerDocument == null)
            throw new IllegalArgumentException
                ("HdxNode: No owner for non-Document node");
        assert nodeType == Node.DOCUMENT_NODE || ownerDocument != null;
        this.nodeType = nodeType;
        this.ownerDocument = ownerDocument;
        parent = firstChild = nextSibling = previousSibling = null;
    }

    public String toString() {
        return "HdxNode(" + getNodeName() + ':' + getNodeValue() + ")";
    }

    /* ******************** EXTENSION METHODS ******************** */
    /* These are useful methods which are not part of the Node interface */

    /**
     * Obtains the Java object corresponding to this node, if any.
     * The object may have been registered with this node, or may have
     * to be constructed.
     *
     * <p>This is an extension to <code>org.w3c.dom.Node</code>
     *
     * <p>Package private: the only (?) extender of HdxNode which
     * overrides this is the class which is returned by {@link
     * HdxDocument#createElement(HdxResourceType,DOMFacade)}, and the
     * only caller of this method is {@link HdxFactory#getObject}.
     *
     * @return a Java object which this node corresponds to, or null
     * if there is no such node
     * @see DOMFacade#getObject(Element)
     */
    Object getNodeObject() {
        if (logger.isLoggable(Level.FINE))
            logger.fine("HdxNode.getNodeObject -- null");
        return null;
    }

    /* ******************** ELEMENT METHODS ******************** */

    /* Following methods are generally inherited from Element, and
     * should be overridden by extending classes */

    public String getNodeName() {
        Class c = getClass();
        throw new UnsupportedOperationException
            ("Method getNodeName() not supported on objects of type "
             + c.getName()
             + " (no." + nodeType + ")");
    }

    public String getNodeValue()
            throws DOMException {
        return null;
    }

    public void setNodeValue(String nodeValue)
            throws DOMException {
        switch (getNodeType()) {
          case Node.DOCUMENT_NODE:
          case Node.DOCUMENT_FRAGMENT_NODE:
          case Node.DOCUMENT_TYPE_NODE:
          case Node.ELEMENT_NODE:
          case Node.ENTITY_NODE:
          case Node.ENTITY_REFERENCE_NODE:
          case Node.NOTATION_NODE:
            // These are all node types which have a null nodeValue:
            // thus setNodeValue(anything) is defined to have no
            // effect, see org.w3c.dom.Node#setNodeValue()
            break;

          default:
            // For everything else, throw an exception.  Classes which
            // implement other types should override this method.
            unimplementedMethod("setNodeValue");
        }
        return;
    }

    public short getNodeType() {
        return nodeType;
    }

    public boolean hasAttributes() {
        return false;
    }

    public NamedNodeMap getAttributes() {
        return null;
    }

    /* The following will typically not be overridden */

    public Node getParentNode() {
        return parent;
    }

    public NodeList getChildNodes() {
        return new NodeList() {
            private java.util.ArrayList list = new java.util.ArrayList();
            {
                for (HdxNode t = firstChild; t!=null; t=t.nextSibling)
                    list.add(t);
            }
            public int getLength() { return list.size(); }
            public Node item(int index) {
                if (index < 0 || index >= getLength())
                    return null;
                else
                    return (Node)list.get(index);
            }
        };
    }

    public Node getFirstChild() {
        return firstChild;
    }

    public Node getLastChild() {
        HdxNode t = firstChild;
        if (t == null)
            return null;
        while (t.nextSibling != null)
            t = t.nextSibling;
        return t;
    }

    public Node getPreviousSibling() {
        return previousSibling;
    }

    public Node getNextSibling() {
        return nextSibling;
    }

    public Document getOwnerDocument() {
        return ownerDocument;
    }

    /* Child insertion and deletion methods */

    /**
     * Finds the given child.  Uses equals().
     *
     * @return the child, or null if it's not there
     */
    private HdxNode findChild (HdxNode n) {
        HdxNode kid = firstChild;
        while (kid != null) {
            if (kid.equals(n))
                return kid;
            kid = kid.nextSibling;
        }
        return null;
    }

    /**
     * Determines if the given node is an ancestor of this one.  Also
     * true if the node is equal to the current node.  Uses equals().
     *
     * @return true if the node is an ancestor, false otherwise.
     */
    private boolean isAncestor(Node n) {
        if (n.equals(this))
            return true;
        return (parent == null ? false : parent.isAncestor(n));
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException (unchecked) if newChild is null
     */
    public Node insertBefore(Node newChild, Node refChild)
            throws DOMException {

        if (newChild == null)
            throw new IllegalArgumentException("newChild is null");
        assertModifiableNode();

        // Handle DocumentFragment nodes specially
        if (newChild.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
            if (logger.isLoggable(Level.FINE))
                logger.fine("HdxNode:" + toString()
                            + ".insertBefore(DocumentFragment)...");
            ArrayList l = new ArrayList();
            // Build a list of children before inserting them --
            // insertion changes the nextSibling field, so if we
            // insert while we work through the loop, we can only ever
            // insert a single node before the loop terminates.
            for (Node kid = newChild.getFirstChild();
                 kid != null;
                 kid = kid.getNextSibling())
                l.add(kid);
            for (Iterator li = l.iterator(); li.hasNext(); )
                insertBefore((Node)li.next(), refChild);
            return newChild;
        }

        assertInsertableNode(newChild);
        if (refChild != null)
            assertInsertableNode(refChild);

        HdxNode n = (HdxNode)newChild;
        if (this.ownerDocument != null)
            // Our owner owns this node, too.  Avoid this if
            // ownerDocument is null -- that is, if n is the document
            // element being added to a Document.
            n.ownerDocument = this.ownerDocument;

        if (refChild == null) {
            HdxNode lastChild = (HdxNode)getLastChild();
            n.parent = this;
            n.nextSibling = null;
            n.previousSibling = lastChild;
            if (lastChild == null)  // this is first
                firstChild = n;
            else
                lastChild.nextSibling = n;
        } else {
            HdxNode kid = findChild((HdxNode)refChild);
            if (kid == null)
                throw new DOMException
                    (DOMException.NOT_FOUND_ERR,
                     "insertBefore: refChild node not found");
            HdxNode prev = kid.previousSibling;
            n.parent = this;
            n.nextSibling = kid;
            n.previousSibling = prev;
            if (prev == null) {  // kid is first
                firstChild = n;
            } else {
                prev.nextSibling = n;
            }
            kid.previousSibling = n;
        }
        if (logger.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer("insertBefore(");
            sb.append(newChild.toString())
                    .append(',')
                    .append(refChild==null ? "<null>" : refChild.toString())
                    .append("): kids now");
            for (HdxNode kid=firstChild; kid!=null; kid=kid.nextSibling) {
                sb.append(' ');
                sb.append(kid.toString());
            }
            logger.fine(sb.toString());
        }
        return newChild;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException (unchecked) if newChild or oldChild is null
     */
    public Node replaceChild(Node newChild, Node oldChild)
            throws DOMException {
        if (newChild == null || oldChild == null)
            throw new IllegalArgumentException
                    ("replaceChild: null newChild or oldChild");
        insertBefore(newChild, oldChild);
        return removeChild(oldChild);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException (unchecked) if oldChild is null
     */
    public Node removeChild(Node oldChild)
            throws DOMException {
        if (oldChild == null)
            throw new IllegalArgumentException("oldChild is null");
        assertModifiableNode();
        HdxNode kid = findChild((HdxNode)oldChild);
        if (kid == null)
            throw new DOMException
                (DOMException.NOT_FOUND_ERR,
                 "oldChild is not a child of this node");
        HdxNode prev = kid.previousSibling;
        if (prev == null)       // kid is first
            firstChild = kid.nextSibling;
        else
            prev.nextSibling = kid.nextSibling;
        if (kid.nextSibling != null)
            kid.nextSibling.previousSibling = prev;
        return oldChild;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException (unchecked) if newChild is null
     */
    public Node appendChild(Node newChild)
            throws DOMException {
        return insertBefore(newChild, null);
    }

    public boolean hasChildNodes() {
        return firstChild != null;
    }

    /**
     * Returns a duplicate of this Node.  The difference between this
     * method and the {@link #clone(boolean)} method is that the node returned
     * by this method has <em>no</em> parent.
     *
     * <p>Although it's not completely specified in the {@link
     * org.w3c.dom.Node#cloneNode documentation}, here, the cloned node does
     * not not retain references to the original's siblings.  If the
     * clone is a shallow one, then the result node has no children either.
     *
     * <p><em>Implementation node</em>: classes which extend
     * <code>HdxNode</code> should preserve the distinction between
     * <code>cloneNode()</code> and <code>clone()</code>.  You may use
     * <code>clone(false)</code> to copy the object fields if that is
     * convenient, but we <em>recurse</em> using
     * <code>cloneNode(true)</code>. This means that we can implement
     * different behaviour for the two methods if we need to, in
     * extending classes.  This distinction is admittedly
     * under-specified right now: if you are writing code where the
     * details matter, you might want to contact the code authors to
     * see if (a) you are attempting something you oughtn't (wicked!), or
     * (b) it's time to pin this specification down more precisely.
     *
     * @param deep if true, recursively clone the subtree under the
     * specified node; if false, clone only the node itself (and its
     * attributes, if it is an Element)
     * @return the duplicate node
     */
//     public Node cloneNode(boolean deep) {
//         // It is specifically
//         // HdxDocument.HdxFacadeElement which gives the
//         // two methods significantly different behavour
//         HdxNode n = new HdxNode(nodeType, ownerDocument);
//         if (deep) {
//             for (HdxNode kid = (HdxNode)getFirstChild();
//                  kid != null;
//                  kid = (HdxNode)kid.getNextSibling())
//             {
//                 n.appendChild(kid.cloneNode(true));
//             }
//         }
//         return n;
//     }
    public Node cloneNode(boolean deep) {
        HdxNode n = (HdxNode)clone(deep);
        n.parent = null;    // cloned node has no parent
        n.nextSibling = null; // ...and no siblings
        n.previousSibling = null;
        return n;
    }

    /**
     * Creates and returns a copy of this Node.  The difference
     * between this method and a deep clone using {@link #cloneNode}
     * is that the clone and the original <em>share</em> a reference to their parent.
     *
     * @return a clone of this instance
     */
    public Object clone() {
        return clone(true);
    }

    /**
     * Creates and returns a copy of this Node.  The difference
     * between this method and a deep clone using {@link #cloneNode}
     * is that the clone and the original <em>share</em> a reference to their parent.
     *
     * @param deep if true, recursively clone the subtree under
     * this node; if false, clone only the node itself
     * @return a clone of this instance
     */
    protected Object clone(boolean deep) {
        try {
            HdxNode n = (HdxNode)super.clone();
            // disconnect this from its children (original still points to them)
            n.firstChild = null;
            if (deep) {
                for (HdxNode kid = (HdxNode)getFirstChild();
                     kid != null;
                     kid = (HdxNode)kid.getNextSibling())
                    n.appendChild((Node)kid.clone());
            }
            return n;
        } catch (CloneNotSupportedException e) {
            // From super.clone().  This can't happen: Since HdxNode
            // does implement Cloneable, Object.clone() does not in
            // fact throw this exception.
            throw new AssertionError
                    ("Can't happen: Object.clone() threw exception in HdxNode.clone");
        }
    }

    /**
     * Indicates whether some other object is `equal to' this one.
     *
     * <p>Two Nodes are equal to one another if their types, {@link
     * Node#getNodeName names} and
     * {@link Node#getNodeValue values} are equal.  However, to avoid
     * a potentially very expensive recursion, this test does
     * <em>not</em> depend on whether the two nodes' <em>children</em>
     * are also equal.
     *
     * <p>Note that this is overridden for <code>Element</code> tests.
     *
     * @param t an object to be tested for equality with this one
     * @return true if the Nodes should be regarded as equivalent
     * @see uk.ac.starlink.hdx.HdxElement#equals
     */
    public boolean equals(Object t) {
        assert !(this instanceof HdxElement); // HdxElement has its own equals()
        if (t == null)
            return false;
        if (! (t instanceof HdxNode))
            return false;
        Node tn = (Node)t;
        if (getNodeType() != tn.getNodeType())
            return false;
        if (! getNodeName().equals(tn.getNodeName()))
            return false;
        String myvalue = getNodeValue();
        String tnvalue = tn.getNodeValue();
        // These must be either both null or both equals()
        if (myvalue != null && tnvalue != null) {
            if (! myvalue.equals(tnvalue))
                return false;
        } else if (myvalue != null || tnvalue != null) {
            return false;
        }

        assert !tn.hasAttributes(); // ...since it's not an HdxElement

        return true;            // whew!
    }

    public int hashCode() {
        // This hashCode should be overridden by HdxElement, which
        // needs to check attributes.  So check that:
        assert getNodeType() != Node.ELEMENT_NODE;
        HdxNode.HashCode hc = new HdxNode.HashCode(getNodeName().hashCode());
        for (Node kid=getFirstChild(); kid!=null; kid=kid.getNextSibling())
            hc.add(kid.hashCode());
        return hc.value();
    }

    public void normalize() {
        normalizeTextChildren();
    }

    private void normalizeTextChildren() {
        Node nextnode;
        for (Node kid=getFirstChild(); kid!=null; kid=nextnode) {
            nextnode = kid.getNextSibling();
            if (kid instanceof Text) {
                // this is either a TEXT_NODE or a CDATA_SECTION_NODE
                Text t = (Text)kid;
                if (t.getLength() == 0)
                    removeChild(kid);
                else if (nextnode != null && nextnode instanceof Text) {
                    t.appendData(nextnode.getNodeValue());
                    Node nextnextnode = nextnode.getNextSibling();
                    removeChild(nextnode);
                    nextnode = nextnextnode;
                }
            } else if (kid.getNodeType() == Node.ELEMENT_NODE)
                ((HdxNode)kid).normalizeTextChildren();
        }
    }

    public boolean isSupported(String feature,
                               String version) {
        return false;           // correct thing to do?
    }

    public String getNamespaceURI() {
        /* This is null, not HdxResourceType.HDX_NAMESPACE.  The
           current element should appear as if it is in the default
           namespace. */
        return null;
    }

    public String getPrefix() {
        return null;
    }

    public void setPrefix(String prefix)
            throws DOMException {
        cannotModifyException("setPrefix");
        assertModifiableNode();
        return;
    }

    /**
     * Returns the local part of the qualified name of this node.
     * Since Hdx elements are always in no namespace, this is always
     * null.
     * @return null
     */
    public String getLocalName() {
        return null;
    }


//DOM3     /* ** DOM Level 3 NOT supported ** */
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public Object setUserData( String key, Object data,
//DOM3                                UserDataHandler handler ) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "setUserData not implemented" );
//DOM3     }
//DOM3     
//DOM3     /** Not implemented */
//DOM3     public Object getUserData( String key ) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "getUserData not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public Object getFeature( String feature, String version ) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "getFeature not implemented" );
//DOM3     }
//DOM3 
//DOM3     public boolean isEqualNode( Node node ) {
//DOM3         return equals( node );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public String lookupNamespaceURI( String prefix ) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "lookupNamespaceURI not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public boolean isDefaultNamespace( String namespaceURI ) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "isDefaultNamespace not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public String lookupPrefix( String namespaceURI ) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "lookupPrefix not implemented" );
//DOM3     }
//DOM3 
//DOM3     public boolean isSameNode( Node other ) {
//DOM3         return this == other;
//DOM3     }
//DOM3 
//DOM3     public void setTextContent( String textContent ) {
//DOM3         setNodeValue( textContent );
//DOM3     }
//DOM3 
//DOM3     public String getTextContent() {
//DOM3         return getNodeValue();
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public short compareDocumentPosition( Node other ) {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                             "compareDocumentPosition not implemented" );
//DOM3     }
//DOM3 
//DOM3     /** Not implemented */
//DOM3     public String getBaseURI() {
//DOM3         throw new DOMException( DOMException.NOT_SUPPORTED_ERR,
//DOM3                                 "getBaseURI not implemented" );
//DOM3     }



    /* ******************** PRIVATE HELPERS ******************** */

    /**
     * Throws a DOMException, explaining that HdxElement is read-only.
     * HdxElement isn't really read-only, but can't be modified using
     * the method that invoked this.
     */
    private void cannotModifyException(String methodName)
            throws DOMException {
        throw new DOMException
            (DOMException.NO_MODIFICATION_ALLOWED_ERR,
             "HdxNode can't be modified using method " + methodName);
    }

    private void notSupportedException(String methodName)
            throws DOMException {
        throw new DOMException
            (DOMException.NOT_SUPPORTED_ERR,
             "HdxNode doesn't support method " + methodName);
    }

    protected void unimplementedMethod(String methodName)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException
            ("Method " + methodName + " not supported on this class ("
             + getClass().getName() + ")");
    }

    /**
     * Asserts that the current node is modifiable.
     *
     * @throws DOMException.NO_MODIFICATION_ALLOWED_ERR if the Node is
     * read-only.
     */
    private void assertModifiableNode()
            throws DOMException {
        return;
    }

    /**
     * Checks that we can edit this node into the current DOM.  Throws
     * a DOMException if not.
     *
     * @throws DOMException.WRONG_DOCUMENT_ERR if the Node is not
     * instanceof HdxNode
     *
     * @throws DOMException.HIERARCHY_REQUEST_ERR if the Node is an
     * ancestor of the current node, or equal to it.
     */
    private void assertInsertableNode(Node n)
            throws DOMException {
        if (n == null)
            throw new IllegalArgumentException("Node to insert is null");
        if (!(n instanceof HdxNode))
            throw new DOMException (DOMException.WRONG_DOCUMENT_ERR,
                                    "Node is not a HdxNode");
        if (this == n)
            throw new DOMException (DOMException.HIERARCHY_REQUEST_ERR,
                                    "Cannot insert Node into itself");
        if (isAncestor(n))
            throw new DOMException (DOMException.HIERARCHY_REQUEST_ERR,
                                    "Node is an ancestor of current node");
        return;
    }

    /**
     * Manages building up a hashCode from other hashCodes.  Usage is
     * <pre>
     * HdxNode.HashCode hc = new HdxNode.HashCode();
     * hc.add(...);             // argument is, say, some other hashCode()
     * hc.add(...);
     * ...
     * hash = hc.value();
     * </pre>
     * The hashcode is order-dependent, so that
     * <code>hc.add(i1).add(i2)</code> and
     * <code>hc.add(i2).add(i1)</code> are different.  The hash
     * algorithm is somewhat home-made, but it does have the
     * properties of being deterministic, using all of the 2^32 bits,
     * and of order-dependence.
     *
     * <p>Package-only class.
     */
    static final class HashCode {
        private java.util.zip.Checksum crc;
        /** Constructs a new, empty, <code>HashCode</code> object */
        public HashCode() {
            crc = new java.util.zip.Adler32();
            //crc = new java.util.zip.CRC32();
        }
        /**
         * Constructs a new <code>HashCode</code> object, supplying a
         * first hashcode.  Calls
         * <pre>
         * new HashCode().add(x);
         * new HashCode(x);
         * </pre>
         * are equivalent.
         */
        public HashCode(int i) {
            this();
            add(i);
        }
        /**
         * Adds another hashcode to the composite hashcode
         * @return this <code>HashCode</code> object
         */
        public HashCode add(int I) {
            for (int i=0; i<4; i++) {
                crc.update(I&0xFF);
                I >>= 8;
            }
            return this;
        }
        /** Resets the object to its initial state. */
        public void reset() {
            crc.reset();
        }
        /** Obtains the value of the composite hashcode */
        public int value() {
            // As good a way as any of getting an int from a long
            return Long.hashCode(crc.getValue());
        }
        /** Indicates whether some other object is "equal to" this one. */
        public boolean equals(Object o) {
            if (o == null || !(o instanceof HashCode))
                return false;
            return crc.equals(((HashCode)o).crc);
        }
        public int hashCode() {
            return crc.hashCode();
        }
    }
}
