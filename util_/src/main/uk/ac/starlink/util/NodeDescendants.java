package uk.ac.starlink.util;

import org.w3c.dom.*;

import java.util.Iterator;

/**
 * Represents the set of Nodes in the tree rooted at a particular DOM
 * Node.  The tree includes the root node.
 *
 * <p>This supports two ways of traversing the tree, either using an
 * iterator, or providing an object which visits each node in the tree
 * in turn.
 *
 * <p>Note that the {@link #iterator} and {@link #visitTree} methods
 * below share state -- namely the state which this object represents
 * -- so you should not use simultaneously the result of two such
 * method calls on the same object.
 */
public class NodeDescendants {
    // Object which allows easy traversal of trees.  If you feel the
    // need to make this more sophisticated, consider whether a full
    // implementation of the <a href="http://www.w3.org/TR/2000/REC-DOM-Level-2-Traversal-Range-20001113/" 
    // >DOM traversal standard interface</a> would be more appropriate.
    // Or whether what you really want is XPath.


    /** Maps node types to the <code>SHOW...</code> masks */
    static private int[] nodeToMaskMap;
    /**
     * Indicates that all nodes should be included in a traversal of,
     * or iteration through, a tree.  To visit only certain nodes,
     * use one of the other <code>SHOW...</code> constants.
     *
     * <p><b>Note</b>: this mechanism is taken from the <a
     * href="http://www.w3.org/TR/DOM-Level-2-Traversal-Range" >DOM2
     * Traversal specification</a>, though it is not an implementation
     * of that set of interfaces.  As noted <a
     * href="http://www.w3.org/TR/2000/REC-DOM-Level-2-Traversal-Range-20001113/traversal.html#Traversal-NodeFilter"
     * >there</a>, not all of the <code>SHOW...</code> values are
     * useful, since not all of the associated <code>Node</code> types
     * can appear as descendants of a Node, othe than in rather
     * special circumstances.  The constants are included here for
     * completeness, however.
     *
     * @see #NodeDescendants(Node,int)
     */
    public static final int SHOW_ALL                  = 0xFFFFFFFF;
    public static final int SHOW_ELEMENT              = 0x00000001;
    public static final int SHOW_ATTRIBUTE            = 0x00000002;
    public static final int SHOW_TEXT                 = 0x00000004;
    public static final int SHOW_CDATA_SECTION        = 0x00000008;
    public static final int SHOW_ENTITY_REFERENCE     = 0x00000010;
    public static final int SHOW_ENTITY               = 0x00000020;
    public static final int SHOW_PROCESSING_INSTRUCTION = 0x00000040;
    public static final int SHOW_COMMENT              = 0x00000080;
    public static final int SHOW_DOCUMENT             = 0x00000100;
    public static final int SHOW_DOCUMENT_TYPE        = 0x00000200;
    public static final int SHOW_DOCUMENT_FRAGMENT    = 0x00000400;
    public static final int SHOW_NOTATION             = 0x00000800;

    static {
        // See comments on nodeTypeMap in DOMUtils.
        nodeToMaskMap = new int[16];
        
        nodeToMaskMap[Node.ATTRIBUTE_NODE] = SHOW_ATTRIBUTE;
        nodeToMaskMap[Node.CDATA_SECTION_NODE] = SHOW_CDATA_SECTION;
        nodeToMaskMap[Node.COMMENT_NODE] = SHOW_COMMENT;
        nodeToMaskMap[Node.DOCUMENT_FRAGMENT_NODE] = SHOW_DOCUMENT_FRAGMENT;
        nodeToMaskMap[Node.DOCUMENT_NODE] = SHOW_DOCUMENT;
        nodeToMaskMap[Node.DOCUMENT_TYPE_NODE] = SHOW_DOCUMENT_TYPE;
        nodeToMaskMap[Node.ELEMENT_NODE] = SHOW_ELEMENT;
        nodeToMaskMap[Node.ENTITY_NODE] = SHOW_ENTITY;
        nodeToMaskMap[Node.ENTITY_REFERENCE_NODE] = SHOW_ENTITY_REFERENCE;
        nodeToMaskMap[Node.NOTATION_NODE] = SHOW_NOTATION;
        nodeToMaskMap[Node.PROCESSING_INSTRUCTION_NODE]
                = SHOW_PROCESSING_INSTRUCTION;
        nodeToMaskMap[Node.TEXT_NODE] = SHOW_TEXT;
    }

    /** The initial node */
    private final Node initialNode;
    /** The current node in the traversal. */
    private Node currentNode;
    /** A subtree. */
    private NodeDescendants subtree;
    /** The mask of Node types to show */
    private int whatToShow;

    /**
     * Creates a new <code>NodeDescendant</code> object.
     * Equivalent to <code>NodeDescendant(node, SHOW_ALL)</code>.
     *
     * @param node the node which is to be the root of the tree
     */
    public NodeDescendants(Node node) {
        this(node, SHOW_ALL);
    }

    /**
     * Creates a new <code>NodeDescendant</code> object.  This
     * represents the set of Nodes in the tree rooted at the given Node.
     *
     * <p>You can configure the set to include only certain
     * nodes.  If the <code>whatToShow</code> parameter is given as
     * {@link #SHOW_ALL}, then all nodes are returned.  If the
     * parameter has one of the other <code>SHOW_...</code> values, or
     * more than one or'ed together (using <code>|</code>), then only
     * the indicated node types are included in the set, and returned
     * by any iterator or examined by any visitor.
     *
     * <p>For example, if you create the <code>NodeDescendant</code>
     * using the constructor:
     * <pre>
     * NodeDescendants tree = new NodeDescendants
     *     (mynode, NodeDescendants.SHOW_ALL);
     * </pre>
     * (which is equivalent to the <code>NodeDescendants(Node)</code>
     * constructor), then the set represents all the nodes in the tree
     * which are reachable from the node <code>mynode</code> by the
     * <code>getFirstChild</code> and similar methods.  If, however,
     * the object was constructed with 
     * <pre>
     * NodeDescendants tree = new NodeDescendants
     *     (mynode, 
     *     NodeDescendants.SHOW_TEXT|NodeDescendants.SHOW_CDATA_SECTION);
     * </pre>
     * then all Text and CDATA nodes would be included in the set, and
     * only these would be returned by the iterator or visited by the visitor.
     *
     * @param node the node which is to be the root of the tree
     * @param whatToShow code indicating which node types should be
     * included in the set
     */
    @SuppressWarnings("this-escape")
    public NodeDescendants(Node node, int whatToShow) {
        initialNode = node;
        this.whatToShow = whatToShow;
        reset();
    }

    /** 
     * Sets the object back to its initial state.  This allows you to
     * extract a second iterator either after an earlier one has
     * finished, or before.
     */
    public void reset() {
        currentNode = initialNode;
        subtree = null;         // marker for first time through
    }

    /**
     * Sets the object back to its initial state, but with a
     * (possibly) different constraint on which nodes are included in the set.
     *
     * @param whatToShow code indicating which node types should be
     * included in the set.  See {@link #NodeDescendants(Node,int)}
     */
    public void reset(int whatToShow) {
        reset();
        this.whatToShow = whatToShow;
    }
    
    /**
     * Returns the next node in the set, irrespective of any
     * constraints represented in <code>whatToShow</code>.
     *
     * @return the next node, or <code>null</code> when all of the
     * elements in the set have been returned
     */
    private Node nextNode() {
        if (currentNode == null)
            return null;
            
        Node ret;
        if (subtree == null) {
            // first time through
            assert currentNode != null;
            ret = currentNode;
            currentNode = currentNode.getFirstChild();
            if (currentNode != null)
                subtree = new NodeDescendants(currentNode, whatToShow);
            return ret;
        }
        
        ret = subtree.nextNode();
        if (ret == null) {
            currentNode = currentNode.getNextSibling();
            if (currentNode != null) {
                subtree = new NodeDescendants(currentNode, whatToShow);
                ret = subtree.nextNode();
            }
        }

        if (ret == null)
            // end of list
            reset();            // for next time?

        return ret;
    }

    /**
     * Returns the next node in the traversal.  This returns only
     * nodes matching those declared in the <code>whatToShow</code>
     * parameter of the initial constructor.
     *
     * @return the next included node, or <code>null</code> when all
     * of the elements in the set have been returned
     */
    private Node nextFilteredNode() {
        if (whatToShow == SHOW_ALL)
            return nextNode();
        
        Node ret;
        do {
            ret = nextNode();
        } while (ret != null
                 && (nodeToMaskMap[ret.getNodeType()] & whatToShow) == 0);
        return ret;
    }

    /**
     * Visits each of the nodes in the tree.  This method
     * iterates through all the descendants of the given node,
     * starting with that node, and presents each of them to the given
     * <code>NodeVisitor</code>.  If that object's
     * <code>visitNode</code> method returns non-null, then the
     * traversal is stopped and the returned object immediately
     * returned as the value of this method.  If the
     * <code>visitNode</code> method always returns null and so the
     * traversal completes, then the method returns null also.
     *
     * @param v a visitor which has each node presented to it in turn
     */
    public Object visitTree(Visitor v) {
        if (v == null)
            throw new IllegalArgumentException
                    ("NodeDescendants.visitTree given null visitor");
        for (Node n = nextFilteredNode(); n != null; n = nextFilteredNode()) {
            Object ret = v.visitNode(n);
            if (ret != null)
                return ret;     // JUMP OUT
        }
        return null;
    }

    /**
     * Obtains an iterator which iterates over the nodes in the set of
     * descendants.  The traversal happens in depth-first order.
     *
     * @return an iterator which returns in turn the given node, then
     * all of its descendants.
     */
    public Iterator<Node> iterator() {
        return new Iterator<Node>() {
                private Node nextNode = nextFilteredNode();
                public boolean hasNext() {
                    return nextNode != null;
                }
                public Node next() {
                    Node ret = nextNode;
                    nextNode = nextFilteredNode();
                    return ret;
                }
                public void remove() {
                    throw new UnsupportedOperationException
                ("NodeDescendants.iterator does not support element removal");
                }
            };
    }

    /**
     * The <code>Visitor</code> processes a single node in a
     * tree.  This allows an object to visit each of the nodes in a tree.
     *
     * @see #visitTree
     */
    public interface Visitor {
        /**
         * Visit a node in a tree.  If the object doing the visiting
         * wishes to stop the traversal early, it should return
         * non-null.  Otherwise it should return null.
         * @param n the node being visited
         * @return null if the traversal is to continue after this
         * node; non-null if it is to stop
         */
        public Object visitNode(Node n);
    }   
}
