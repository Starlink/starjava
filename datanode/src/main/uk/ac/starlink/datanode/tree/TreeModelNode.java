package uk.ac.starlink.datanode.tree;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.datanode.nodes.DataNode;

/**
 * Object used to store information about items in the DataNodeTreeModel
 * hierarchy.  This plays a role somewhat similar to that which
 * {@link javax.swing.tree.TreeNode} plays for 
 * {@link javax.swing.tree.DefaultTreeModel}, but also takes care of
 * some of the duties related to asynhronous expansion of the node.
 * <p>
 * You should generally synchronize on a <code>TreeModelNode</code> when 
 * accessing it in a way which might modify it or be sensitive to 
 * modification of it.
 * <p>
 * To create a <code>TreeModelNode</code>, use the 
 * {@link DataNodeTreeModel#makeModelNode} method
 * of <code>DataNodeTreeModel</code>.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TreeModelNode {

    private final DataNode dataNode;
    private final TreeModelNode parent;
    private final List children;
    private NodeExpander expander;

    /**
     * Constructs a new TreeModelMode from a DataNode. 
     * Initially, no children are present.
     *
     * @param  dataNode  the DataNode managed by this TreeModelNode
     * @param  parent    the parent of this node (null for the root)
     */
    TreeModelNode( DataNode dataNode, TreeModelNode parent ) {
        this.dataNode = dataNode;
        this.parent = parent;
        this.children = new ArrayList();
    }

    /**
     * Returns the list which contains the children.  This list may be
     * modified, but only TreeModelNodes should be elements of the list.
     *
     * @return  mutable list of child nodes
     */
    public List getChildren() {
        return children;
    }

    /**
     * Returns the parent of this node.  Will be <code>null</code> for the
     * root.
     *
     * @return  parent node
     */
    public TreeModelNode getParent() {
        return parent;
    }

    /**
     * Returns the DataNode managed by this TreeModelNode.
     *
     * @return  data node
     */
    public DataNode getDataNode() {
        return dataNode;
    }

    /**
     * Installs a NodeExpander object to take charge of 
     * locating this node's children.  Any existing NodeExpander owned
     * by this node will be uninstalled.  <code>NodeExpander</code>s behave in
     * such a way that such uninstallation will (at least may) cause 
     * them to stop expanding, 
     * so an expander should not be deinstalled and installed again later.
     * <p>
     * It is the responsibility of the calling code to ensure that the
     * new expander starts doing its expansion work.
     *
     * @param   expander  new node expander
     */
    public synchronized void setExpander( NodeExpander expander ) {
        this.expander = expander;
    }

    /**
     * Returns the object which is currently in charge of locating this
     * nodes children.  If <code>null</code>, no node expansion has been
     * attempted on this node.
     *
     * @return  the expander which has started (and may have finished)
     *          locating this node's children
     */
    public synchronized NodeExpander getExpander() {
        return expander;
    }

}
