package uk.ac.starlink.connect;

/**
 * Represents a node in a (possibly remote) file system.
 *
 * <p><strong>Note</strong> that the {@link java.lang.Object#equals} method
 * must be implemented in such a way that two Nodes referring to the
 * same point in the file system are considered equal.
 * (Don't forget to implement {@link java.lang.Object#hashCode}
 * consistently with <code>equals</code> too).
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
public interface Node {

    /**
     * Returns the name of this node.
     * This should not be an entire pathname, that is, it should not
     * include the name of its parent.
     * 
     * @return  name
     */
    String getName();

    /**
     * Returns the parent branch of this node.
     * If this node is at the root of its tree,
     * it will return <code>null</code>.
     *
     * @return   parent
     */
    Branch getParent();
}
