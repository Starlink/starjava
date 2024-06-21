package uk.ac.starlink.datanode.tree.select;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import uk.ac.starlink.connect.Connection;
import uk.ac.starlink.connect.ConnectorAction;
import uk.ac.starlink.datanode.factory.CreationState;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.nodes.BranchDataNode;
import uk.ac.starlink.datanode.nodes.DataNode;

/**
 * Container object for a DataNode and all of its ancestors.
 *
 * @author    Mark Taylor (Starlink)
 * @since     10 Mar 2005
 */
class NodeChain {

    private final DataNodeFactory factory_;
    private final ConnectorAction connAct_;
    private DataNode[] nodes_;

    /**
     * Constructs a new chain from its terminal node.
     *
     * @param  node  terminal node of chain
     */
    public NodeChain( DataNode node ) {
        setNode( node );
        DataNode root = nodes_[ 0 ];
        factory_ = root.getChildMaker();
        connAct_ = root instanceof ConnectorDataNode
                 ? ((ConnectorDataNode) root).getConnectorAction()
                 : null;
    }

    /**
     * Sets this chain's terminal node.  The chain will consist of
     * <code>node</code> as well as all of its ancestors as retrieved
     * recursively using <code>getCreator().getParent()</code>.
     *
     * @param   node   new terminal node
     */
    public void setNode( DataNode node ) {
        List ancestors = new ArrayList();
        for ( DataNode ancestor = node; ancestor != null; ) {
            ancestors.add( ancestor );
            CreationState creator = ancestor.getCreator();
            ancestor = creator == null ? null
                                       : creator.getParent();
        }
        Collections.reverse( ancestors );
        nodes_ = (DataNode[]) ancestors.toArray( new DataNode[ 0 ] );
    }

    /**
     * Returns the terminal node of this chain.
     *
     * @return  last node in chain
     */
    public DataNode getNode() {
        return nodes_[ nodes_.length - 1 ];
    }

    /**
     * Returns the root of this chain.
     *
     * @return   first node in chain
     */
    public DataNode getRoot() {
        return nodes_[ 0 ];
    }

    /**
     * Returns the number of nodes in this chain.
     *
     * @return   chain depth
     */
    public int getDepth() {
        return nodes_.length;
    }

    /**
     * Returns the <code>level</code>'th ancestor.  Level 0 is the root.
     *
     * @param   level  distance from root
     * @return  node
     */
    public DataNode getAncestor( int level ) {
        return nodes_[ level ];
    }

    /**
     * Returns the ConnectorAction associated with the root of this chain,
     * if any.  The ConnectorAction never changes.
     *
     * @return  connector action, or null
     */
    public ConnectorAction getConnectorAction() {
        return connAct_;
    }

}
