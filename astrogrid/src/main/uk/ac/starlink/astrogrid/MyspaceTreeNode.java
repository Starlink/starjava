package uk.ac.starlink.astrogrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import javax.swing.tree.TreeNode;
import org.astrogrid.store.tree.Container;
import org.astrogrid.store.tree.Node;

/**
 * Implements {@link javax.swing.tree.TreeNode} for a node in MySpace.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Nov 2004
 */
public class MyspaceTreeNode implements TreeNode {

    private final Node node_;
    private final TreeNode parent_;
    private MyspaceTreeNode[] children_;
    private final static Comparator nodeComparator_ = new NodeComparator();

    /**
     * Constructs a new tree node from a MySpace node.
     *
     * @param  parent  parent tree node (may be null for the root node)
     * @param  node    node in MySpace
     */
    public MyspaceTreeNode( TreeNode parent, Node node ) {
        parent_ = parent;
        node_ = node;
    }

    /**
     * Returns the MySpace node object associated with this tree node.
     *
     * @return MySpace node
     */
    public Node getMyspaceNode() {
        return node_;
    }

    public boolean getAllowsChildren() {
        return node_.isContainer();
    }

    public TreeNode getChildAt( int index ) {
        return getChildArray()[ index ];
    }

    public int getChildCount() {
        return getChildArray().length;
    }

    public int getIndex( TreeNode node ) {
        return Arrays.asList( getChildArray() ).indexOf( node );
    }

    public Enumeration children() {
        return Collections.enumeration( Arrays.asList( getChildArray() ) );
    }

    public TreeNode getParent() {
        return parent_;
    }

    public boolean isLeaf() {
        return getChildArray().length == 0;
    }

    public String toString() {
        return node_.getName();
    }

    private MyspaceTreeNode[] getChildArray() {

        /* Lazily constructs array of child nodes. */
        if ( children_ == null ) {
            if ( getAllowsChildren() ) {
                List childList = new ArrayList();
                for ( Iterator it = ((Container) node_).getChildNodes()
                                                       .iterator();
                      it.hasNext(); ) {
                    childList.add( new MyspaceTreeNode( this,
                                                        (Node) it.next() ) );
                }
                Collections.sort( childList, nodeComparator_ );
                children_ = (MyspaceTreeNode[])
                            childList.toArray( new MyspaceTreeNode[ 0 ] );
            }
            else {
                children_ = new MyspaceTreeNode[ 0 ];
            }
        }
        return children_;
    }

    /**
     * Comparator for sorting nodes.  Containers are ranked first, then
     * non-Containers (files).  Within each group, they are ranked
     * alphabetically by name.
     */
    private static class NodeComparator implements Comparator {
        public int compare( Object o1, Object o2 ) {
            MyspaceTreeNode t1 = (MyspaceTreeNode) o1;
            MyspaceTreeNode t2 = (MyspaceTreeNode) o2;
            Node n1 = (Node) t1.getMyspaceNode();
            Node n2 = (Node) t2.getMyspaceNode();
            boolean c1 = n1.isContainer();
            boolean c2 = n2.isContainer();
            return c1 == c2 ? n1.getName().compareTo( n2.getName() )
                            : ( c1 ? -1 : +1 );
        }
    }
}
