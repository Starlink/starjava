package uk.ac.starlink.connect;

/**
 * Represents a directory-like node in a (possibly remote) filesystem.
 * A branch may contain child items.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
public interface Branch extends Node {

    /**
     * Returns the array of child nodes belonging to this branch.
     * All the returned values represent file objects which actuallly exist
     * in the filesystem.
     *
     * @return   child nodes of this one
     */
    Node[] getChildren();

    /**
     * Attempts to construct a new node in the context of this one.
     * The new item may represent a new or an existing node in the 
     * filesystem.  This call should not in itself perform any write
     * operations on the filesystem (such as creating a node which 
     * doesn't currently exist), though a subsequent
     * {@link Leaf#getOutputStream} call may do so.
     *
     * <p>The returned node will typically be a child of this branch,
     * but need not be, for instance if <code>name</code> is interpreted as an 
     * absolute path.
     *
     * <p>If the named node cannot be created, <code>null</code>
     * may be returned.
     *
     * @param   name   name of a node in the context of this branch
     * @return   node representing the location of an existing or new node
     */
    Node createNode( String name );
}
