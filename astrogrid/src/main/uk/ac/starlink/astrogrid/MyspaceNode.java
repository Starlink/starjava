package uk.ac.starlink.astrogrid;

import uk.ac.starlink.connect.Branch;

/**
 * Connection node implemenatation in terms of a Myspace node.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Feb 2005
 */
class MyspaceNode implements uk.ac.starlink.connect.Node {

    final org.astrogrid.store.tree.Node myNode_;
    final MyspaceBranch parent_;
    final String path_;

    /**
     * Constructor.
     *
     * @param  myNode   Myspace node object
     * @param  parent   parent Myspace branch
     */
    public MyspaceNode( org.astrogrid.store.tree.Node myNode,
                        MyspaceBranch parent ) {
        myNode_ = myNode;
        parent_ = parent;
        path_ = parent == null ? "/" 
                               : parent.toString() + "/" + getName();
    }

    public String getName() {
        return myNode_.getName();
    }

    public Branch getParent() {
        return parent_;
    }

    public org.astrogrid.store.tree.Node getMyspaceNode() {
        return myNode_;
    }

    public String toString() {
        return path_;
    }
}
