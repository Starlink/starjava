package uk.ac.starlink.datanode.tree.select;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import uk.ac.starlink.connect.Branch;
import uk.ac.starlink.connect.Connection;
import uk.ac.starlink.connect.ConnectorAction;
import uk.ac.starlink.datanode.factory.CreationState;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.nodes.BranchDataNode;
import uk.ac.starlink.datanode.nodes.DataNode;

/**
 * Model for a NodeRootComboBox.
 *
 * @author   Mark Taylor (Starlink)
 * @since    10 Mar 2005
 */
public class NodeRootModel extends AbstractListModel implements ComboBoxModel {

    /* The basic data structure in which the model's data are held is an
     * array of N NodeChain objects.  However this corresponds to >= N items,
     * since the model is considered to hold all the ancestors of each
     * branch (every link in each chain).
     * The selected_ member points to the currently selected NodeChain;
     * the currently selected node is the terminal node of the selected
     * chain. */

    private NodeChain[] chains_ = new NodeChain[ 0 ];
    private int selected_ = -1;
    private final DataNodeFactory factory_;

    public NodeRootModel( DataNodeFactory factory ) {
        factory_ = factory;
    }

    public int getSize() {
        int size = 0; 
        for ( int i = 0; i < chains_.length; i++ ) {
            size += chains_[ i ].getDepth();
        }
        return size;
    }

    public Object getElementAt( int index ) {
        for ( int i = 0; i < chains_.length; i++ ) {
            NodeChain chain = chains_[ i ];
            int depth = chain.getDepth();
            if ( index < depth ) {
                return chain.getAncestor( index );
            }
            else {
                index -= depth;
            }
        }
        return null;
    }

    public Object getSelectedItem() {
        return getSelectedNode();
    }

    public void setSelectedItem( Object item ) {
        setSelectedNode( (DataNode) item );
    }

    public DataNode getSelectedNode() {
        return selected_ >= 0 ? chains_[ selected_ ].getNode()
                              : null;
    }

    /** 
     * Sets the given node as the current selection.  If the given
     * node has the same root as one of the existing chains, 
     * that chain's current node is changed and it is marked selected.
     * If the given node is not rooted at the same place as any of
     * the existing ones, a new chain is added containing it.
     * 
     * @param  node  new selection
     */
    private void setSelectedNode( DataNode node ) {
        DataNode root = new NodeChain( node ).getRoot();
        for ( int i = 0; i < chains_.length; i++ ) {
            if ( sameData( root, chains_[ i ].getRoot() ) ) {
                chains_[ i ].setNode( node );
                selected_ = i;
                fireContentsChanged( this, -1, -1 );
                return;
            }
        }
        addChain( new NodeChain( node ) );
        selected_ = chains_.length - 1;
        fireContentsChanged( this, -1, -1 );
    }

    /**
     * Adds a new node chain to this model.
     *
     * @param   chain  new chain to add
     */
    public void addChain( final NodeChain chain ) {

        /* Add a new chain to the list of chains. */
        int oldSize = getSize();
        List clist = new ArrayList( Arrays.asList( chains_ ) );
        clist.add( chain );
        chains_ = (NodeChain[]) clist.toArray( new NodeChain[ 0 ] );
        fireIntervalAdded( this, Math.max( 0, oldSize - 1 ), getSize() - 1 );

        /* If there was previously no selection, make this chain the
         * selected one. */
        if ( selected_ < 0 ) {
            setSelectedNode( chain.getNode() );
        }
        
        /* If it's a connector node, make sure that this model will react
         * properly to the connection going up or down. */
        DataNode node = chain.getNode();
        if ( node instanceof ConnectorDataNode ) {
            final ConnectorDataNode cnode = (ConnectorDataNode) node;
            cnode.getConnectorAction()
                 .addPropertyChangeListener( new PropertyChangeListener() {
                public void propertyChange( PropertyChangeEvent evt ) {
                    if ( evt.getPropertyName()
                            .equals( ConnectorAction.CONNECTION_PROPERTY ) ) {
                        Connection conn = (Connection) evt.getNewValue();
                        int oldSize = getSize();
                        DataNode snode;
                        if ( conn == null ) {
                            snode = cnode;
                        }
                        else {
                            Branch root = conn.getRoot();
                            snode = factory_.makeChildNode( null, root );
                            if ( snode instanceof BranchDataNode ) {
                                ((BranchDataNode) snode).setConnection( conn );
                            }
                        }
                        chain.setNode( snode );
                        fireContentsChanged( this, 0,
                                             Math.max( oldSize, getSize() ) );
                    }
                }
            } );
        }
    }

    /**
     * Tests whether two data nodes reference the same data object.
     *
     * @param  n1  one node
     * @param  n2  other node
     * @return  true  iff n1 and n2 are representations of the same thing
     */
    private boolean sameData( DataNode n1, DataNode n2 ) {
        if ( n1.equals( n2 ) ) {
            return true;
        }
        else {
            CreationState creator1 = n1.getCreator();
            CreationState creator2 = n2.getCreator();
            if ( creator1 != null && creator2 != null ) {
                Object dataObj1 = creator1.getObject();
                Object dataObj2 = creator2.getObject();
                if ( dataObj1 != null && dataObj2 != null &&
                     dataObj1.equals( dataObj2 ) ) {
                    return true;
                }
            }
        }
      
        return false;
    }

    public void removeAllElements() {
        int oldSize = getSize();
        chains_ = new NodeChain[ 0 ];
        selected_ = -1;
        fireContentsChanged( this, 0, oldSize );
    }

    public ConnectorAction getConnectorAction() {
        return selected_ >= 0 ? chains_[ selected_ ].getConnectorAction()
                              : null;
    }

}
