// $Id$

package uk.ac.starlink.hdx;

import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * An node in an HDX DOM.
 *
 * <p>This implements the W3C {@link Node} interface, with the
 * addition of the {@link #getNodeObject} method.
 *
 * <p>Package private.
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
        return "HdxNode(" + getNodeValue() + ")";
    }

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
        System.err.println("HdxNode.getNodeObject -- null");
        return null;
    }

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
        unimplementedMethod("setNodeValue");
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
    private boolean isAncestor(HdxNode n) {
        if (this.equals(n))
            return true;
        return (parent == null ? false : parent.isAncestor(n));
    }

    public Node insertBefore(Node newChild, 
                             Node refChild)
            throws DOMException {

        assertModifiableNode();

        // Handle DocumentFragment nodes specially
        if (newChild.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
            System.err.println("HdxNode:" + toString()
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
        System.err.println("...inserted. kids now:");
        for (HdxNode kid=firstChild; kid!=null; kid=kid.nextSibling)
            System.err.println("  " + kid);
        System.err.println("...!");
        return newChild;
    }

    public Node replaceChild(Node newChild, 
                             Node oldChild)
                             throws DOMException {
        notSupportedException("replaceChild");
        assertModifiableNode();
        return null;
    }

    public Node removeChild(Node oldChild)
            throws DOMException {
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

    public Node appendChild(Node newChild)
            throws DOMException {
        return insertBefore(newChild, null);
    }

    public boolean hasChildNodes() {
        return firstChild != null;
    }

    public Node cloneNode(boolean deep) {
        HdxNode n = (HdxNode)clone();
        n.parent = null;
        return (Node)n;
    }

    /**
     * Clones this Node.  The reference to the parent is shared with
     * the original, but the children are full clones.
     */
    public Object clone() {
        try {
            HdxNode n = (HdxNode)super.clone();
            n.parent = parent;
            n.firstChild = (firstChild == null
                            ? null
                            : (HdxNode)firstChild.clone());
            n.nextSibling = (nextSibling == null
                             ? null
                             : (HdxNode)nextSibling.clone());
            n.previousSibling = (previousSibling == null
                                 ? null
                                 : (HdxNode)previousSibling.clone());
            return n;
        } catch (CloneNotSupportedException e) {
            // This shouldn't happen -- this and Object both support clone()
            throw new InternalError(e.toString());
        }
    }

    /**
     * Normalizes the DOM.  There is nothing to do here -- at present
     * the HdxElement DOM has no nodes other than Elements, so there
     * are no Text, PI, or other nodes to be consolidated.
     */
    public void normalize() {
        return;                 // do nothing!
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

    public String getLocalName() {
        return null;
    }

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
            ("Method " + methodName + " not supported on this class");
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
        if (!(n instanceof HdxNode))
            throw new DOMException (DOMException.WRONG_DOCUMENT_ERR,
                                    "Node is not a HdxNode");
        if (isAncestor((HdxNode)n))
            throw new DOMException (DOMException.HIERARCHY_REQUEST_ERR,
                                    "Node is an ancestor of current node");
        return;
    }
}
