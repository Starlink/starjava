package uk.ac.starlink.datanode.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.EmptyDataNode;

/**
 * A TreeModel for storing {@link DataNode}s.  
 * Each Object in the TreeModel is a DataNode.
 * Node expansion is handled carefully: children are added one child at a time 
 * with suitable TreeModelEvents fired after each one rather than 
 * providing the whole list of children at once, since this could
 * be a time-consuming operation and might cause the user interface
 * to lock up for longer than was acceptable.
 * <p>
 * Associated with each <code>DataNode</code> in the tree is a 
 * {@link TreeModelNode}, which handles some of the structure and is
 * in fact used internally by this tree model to store the tree structure.
 * This can be obtained using the {@link #getModelNode} method 
 * and manipulated directly for more direct control over the tree structure
 * than is possibly by manipulating <code>DataNode</code>s.
 * 
 * @author   Mark Taylor
 */
public class DataNodeTreeModel implements TreeModel {

    /*
     * Internally the tree structure is stored as a hierarchy of 
     * TreeModelNode objects.  Each of these contains a DataNode (the
     * node payload), a TreeModelNode parent, and a list of children; 
     * each child is a TreeModelNode.
     * The hierarchical structure of the tree model is provided by 
     * navigating these lists of children.
     * Additionally, a Map is maintained which maps all DataNode objects
     * to their corresponding TreeModelNode. 
     * These two structures together enable the TreeModel to be 
     * implemented in such a way that the user sees a DataNode at each
     * node of the tree.
     */

    /** The root node of the tree.  */
    private TreeModelNode root;

    /** Maps each DataNode to its corresponding TreeModelNode. */
    private Map nodeMap = new HashMap();

    /** DefaultTreeModel kept on the staff purely to handle events. */
    TreeModelListenerHandler listenerHandler = 
        new TreeModelListenerHandler();

    /**
     * Constructs a new DataNodeTreeModel with a default root node.
     */
    public DataNodeTreeModel() {
        this( new EmptyDataNode() );
    }
 
    /**
     * Constructs a new DataNodeTreeModel with a given root node.
     *
     * @param   rootDataNode  the root node
     */
    public DataNodeTreeModel( DataNode rootDataNode ) {
        root = makeModelNode( rootDataNode, null );
    }

    /**
     * Returns the root of the tree.
     *
     * @return  tree root {@link DataNode}
     */
    public Object getRoot() {
        return root.getDataNode();
    }

    /**
     * Returns a given child of a node in the tree.
     *
     * @param  parentDataNode  parent {@link DataNode}
     * @param  index  index of the child of <code>parentDataNode</code> required
     * @return  child {@link DataNode}, the <code>index</code>'th child of
     *          <code>parentDataNode</code>
     */
    public Object getChild( Object parentDataNode, int index ) {
        TreeModelNode childModelNode = 
            (TreeModelNode) getModelNode( (DataNode) parentDataNode )
                           .getChildren().get( index );
        return childModelNode.getDataNode();
    }

    /**
     * Returns the number of children a node currently has.
     * <p>
     * The first time that this method is called on a node it is 
     * used as a trigger to begin node expansion.  This may be a
     * a time-consuming business, so this method will return a 
     * number of children which can be determined without delay
     * (this may be zero), and will initiate the process of retrieving
     * all the other children.  This is done in a separate thread,
     * and a suitable <code>TreeModelEvent</code> is fired each time a
     * child arrives.  The expansion process will continue until 
     * all the children have been found.  No notification is currently
     * made when the set of children is complete.
     * <p>
     * Subsequent calls return the number of children which are currently
     * present, but again do not offer a guarantee that more might not
     * be about to arrive.
     *
     * @param  parentDataNode  {@link DataNode} object in the tree to be
     *         queried
     * @return number of children
     */
    public int getChildCount( Object parentDataNode ) {
        DataNode dataNode = (DataNode) parentDataNode;
        TreeModelNode modelNode = getModelNode( dataNode );
        synchronized ( modelNode ) {
            if ( modelNode.getExpander() == null ) {
                final NodeExpander expander = 
                    new NodeExpander( this, modelNode );
                modelNode.setExpander( expander );
                new Thread( "Node expander: " + dataNode ) {
                    public void run() {
                        expander.expandNode();
                    }
                }.start();
            }
            return modelNode.getChildren().size();
        }
    }

    /**
     * Returns an array of the children currently owned by a given node.
     * Unlike {@link #getChildCount}, this does not trigger expansion
     * of a not-currently-expanded node, it just gives a snapshot of
     * the current state of the model.
     *
     * @param  dataNode  the node whose children are being enquired about
     * @return  an array of <code>dataNode</code>'s children
     */
    public DataNode[] getCurrentChildren( DataNode dataNode ) {
        TreeModelNode modelNode = getModelNode( dataNode );
        synchronized ( modelNode ) {
            List childList = modelNode.getChildren();
            int nChild = childList.size();
            DataNode[] children = new DataNode[ nChild ];
            for ( int i = 0; i < nChild; i++ ) {
                children[ i ] = 
                    ((TreeModelNode) childList.get( i )).getDataNode();
            }
            return children;
        }
    }

    /**
     * Indicates whether the node is a leaf, that is cannot have any 
     * children.  Non-leaf nodes may still be childless though.
     *
     * @param   dataNode  {@link DataNode} node to query
     * @return  true iff <code>dataNode</code> cannot support children
     */
    public boolean isLeaf( Object dataNode ) {
        return ! ((DataNode) dataNode).allowsChildren();
    }

    /**
     * Returns the index of a given child if it is a direct child of another.
     * <p>
     * Note this returns -1 (without error) if <code>childDataNode</code> 
     * does not appear in the tree at all; this appears to be required 
     * on occasion by Sun's JTree implementation, though that's not 
     * documented in the {@link javax.swing.tree.TreeModel} interface.
     *
     * @param  parentDataNode  {@link DataNode} which is the parent object
     * @param  childDataNode {@link DataNode} which is a child of 
     *         <code>parentDataNode</code>
     * @return the child number of <code>childDataNode</code> within
     *         <code>parentDataNode</code> or -1 if it's not a child
     */
    public int getIndexOfChild( Object parentDataNode, Object childDataNode ) {
        if ( parentDataNode != null && childDataNode != null &&
             nodeMap.containsKey( childDataNode ) ) {
            TreeModelNode parentModelNode = 
                getModelNode( (DataNode) parentDataNode );
            TreeModelNode childModelNode =
                getModelNode( (DataNode) childDataNode );
            return parentModelNode.getChildren().indexOf( childModelNode );
        }
        else {
            return -1;
        }
    }

    /**
     * Messaged when the user has altered the value for an item.
     * 
     * @param  path  path to the altered node (all the objects in the
     *         path must be <code>DataNode</code>s)
     * @param  newDataNode  the {@link DataNode} which is now found at
     *         <code>path</code>
     */
    public void valueForPathChanged( TreePath path, Object newDataNode ) {
        replaceNode( (DataNode) path.getLastPathComponent(), 
                     (DataNode) newDataNode );
    }

    public void addTreeModelListener( TreeModelListener listener ) {
        listenerHandler.addTreeModelListener( listener );
    }

    public void removeTreeModelListener( TreeModelListener listener ) {
        listenerHandler.removeTreeModelListener( listener );
    }

    /**
     * Returns the number of nodes known in this model.
     *
     * @return  node count
     */
    public int getNodeCount() {
        return nodeMap.size();
    }

    /**
     * Indicates whether this model contains a given data node.
     *
     * @return  <code>true</code> iff this model contains <code>node</code>
     */
    public boolean containsNode( DataNode node ) {
        return nodeMap.containsKey( node );
    }

    /**
     * Returns an array representing the position of the given 
     * <code>DataNode</code> in the tree.  
     * The root is the first element in the returned
     * array, and <code>dataNode</code> is the last.  The length of
     * the returned array gives the node's depth in the tree.
     * If <code>dataNode</code> does not exist in this model, <code>null</code>
     * is returned.
     *
     * @param  dataNode  the node to find the path of
     * @return  the path from the root to <code>dataNode</code>
     */
    public DataNode[] getPathToRoot( DataNode dataNode ) {
        return nodeMap.containsKey( dataNode ) 
            ? getPathToRoot( getModelNode( dataNode ) )
            : null;
    }

    /**
     * Returns an array representing the position of the given 
     * <code>ModelNode</code> in the tree.
     *
     * @param  modelNode  the node to find the path of
     * @return  the path from the root to <code>modelNode</code>
     */
    private DataNode[] getPathToRoot( TreeModelNode modelNode ) { 
        List pathList = new ArrayList();
        for ( ; modelNode != null; modelNode = modelNode.getParent() ) {
            pathList.add( modelNode.getDataNode() );
        }
        Collections.reverse( pathList );
        return (DataNode[]) pathList.toArray( new DataNode[ 0 ] );
    }

    /**
     * Inserts a new node into the child list of a node which exists 
     * in this tree model.  The model's listeners are notified.
     * This method may be called from any thread,
     * not just the event dispatch thread.
     *
     * @param  newChild  the new data node to insert into
     *         <code>parent</code>'s list of children
     * @param  parent  the parent node in whose children <code>newChild</code>
     *         should be inserted
     * @param  ipos the position at which the insertion should take place
     */
    public void insertNode( DataNode newChild, DataNode parent, int ipos ) {
        TreeModelNode parentModelNode = getModelNode( parent );
        synchronized ( parentModelNode ) {

            /* Create the new node to add. */
            TreeModelNode childModelNode =
                makeModelNode( newChild, parentModelNode );

            /* Insert the new node into the list of its parent's children. */
            List childList = parentModelNode.getChildren();
            childList.add( ipos, childModelNode );

            /* Notify listeners of the new arrival. */
            final Object[] path = getPathToRoot( parentModelNode );
            final int[] indices = new int[] { ipos };
            final Object[] children = new DataNode[] { newChild };
            invokeLater( new Runnable() {
                public void run() {
                    listenerHandler.fireTreeNodesInserted( this, path, indices,
                                                           children );
                }
            } );
        }
    }

    /**
     * Appends a new child node to the children of a node which 
     * exists in this tree model.  The model's listeners are notified.
     * This method may be called from any thread,
     * not just the event dispatch thread.
     *
     * @param  newChild  the new data node to add at the end of 
     *         <code>parent</code>'s list of children
     * @param  parent  the parent node to whose children <code>newChild</code>
     *         should be appended
     */
    public void appendNode( DataNode newChild, DataNode parent ) {
        TreeModelNode parentModelNode = getModelNode( parent );
        synchronized ( parentModelNode ) {
            int ipos = parentModelNode.getChildren().size();
            insertNode( newChild, parent, ipos );
        }
    }

    /**
     * Removes a node from the tree.  The model's listeners are notified.
     * This method may be called from any thread,
     * not just the event dispatch thread.
     * 
     * @param  dataNode  the node to remove
     */
    public void removeNode( DataNode dataNode ) {
        TreeModelNode modelNode = getModelNode( dataNode );
   
        /* Clear out resources associated with the node being removed. */
        discardModelNode( modelNode );

        /* Remove the node from the appropriate data structures. */
        TreeModelNode parentModelNode = modelNode.getParent();
        synchronized ( parentModelNode ) {
            List childList = parentModelNode.getChildren();
            int index = childList.indexOf( modelNode );

            /* Remove the child from its parent node. */
            childList.remove( index );

            /* Notify listeners of the change. */
            final Object[] path = getPathToRoot( parentModelNode );
            final int[] indices = new int[] { index };
            final Object[] children = new DataNode[] { dataNode };
            invokeLater( new Runnable() {
                public void run() {
                    listenerHandler.fireTreeNodesRemoved( this, path, indices, 
                                                          children );
                }
            } );
        }
    }

    /**
     * Replaces a given data node with a new one.  This only works for
     * non-root nodes.  To replace the root node, use {@link #setRoot}.
     *
     * @param  oldDataNode  the node to be replaced
     * @param  newDataNode  the node to replace it with
     */
    public void replaceNode( DataNode oldDataNode, DataNode newDataNode ) {
        TreeModelNode oldModelNode = getModelNode( oldDataNode );
        TreeModelNode parentModelNode = oldModelNode.getParent();
 
        /* Create a new model node representing the new data node. */
        TreeModelNode newModelNode = 
            makeModelNode( newDataNode, parentModelNode );

        synchronized ( parentModelNode ) {

            /* Replace the old model node by the new one in its parent. */
            List childList = parentModelNode.getChildren();
            int index = childList.indexOf( oldModelNode );
            childList.set( index, newModelNode );

            /* Clear out obsolete entries from the node map. */
            discardModelNode( oldModelNode );

            /* Notify listeners of the change. */
            Object[] path = getPathToRoot( newDataNode );
            listenerHandler.fireTreeStructureChanged( this, path, null, null );
        }
    }

    /**
     * Sets the root node of the tree.
     *
     * @param  rootDataNode  the new root node 
     */
    public synchronized void setRoot( DataNode rootDataNode ) {

        /* Clear out the existing data structures. */
        discardModelNode( root );

        /* Construct a new root model node. */
        TreeModelNode newRoot = makeModelNode( rootDataNode, null );

        /* Change the root. */
        root = newRoot;

        /* Notify listeners. */
        Object[] path = new Object[] { newRoot };
        listenerHandler.fireTreeStructureChanged( this, path, null, null );
    }

    /**
     * Effectively re-initialises a node.  Its children are removed from
     * the tree, and any subsequent attempts to read the children
     * (triggered by getChildCount) will cause them to be re-acquired
     * from the DataNode.
     *
     * @param   dataNode  the node to refresh
     */
    public void refreshNode( DataNode dataNode ) {
        TreeModelNode oldModelNode = getModelNode( dataNode );

        /* Discard its children. */
        synchronized( oldModelNode ) {

            oldModelNode.setExpander( null );
            for ( Iterator it = oldModelNode.getChildren().iterator();
                  it.hasNext(); ) {
                TreeModelNode child = (TreeModelNode) it.next();
                discardModelNode( child );
            }
        }

        /* Rehouse the DataNode in a new TreeModelNode. */
        TreeModelNode parentModelNode = oldModelNode.getParent();
        TreeModelNode newModelNode = repackage( dataNode, parentModelNode );

        /* Replace the modelnode in its parent with the repackaged one. */
        if ( parentModelNode != null ) {
            synchronized ( parentModelNode ) {
                List childList = parentModelNode.getChildren();
                int index = childList.indexOf( oldModelNode );
                childList.set( index, newModelNode );
            }
        }

        /* Notify the listeners that a change has taken place. */
        Object[] path = getPathToRoot( newModelNode );
        listenerHandler.fireTreeStructureChanged( this, path, null, null );
    }

    /**
     * Refreshes the representation of the node iteself.  It is redrawn,
     * but nothing is done about its children.
     *
     * @param  dataNode  the node to repaint
     */
    public void repaintNode( DataNode dataNode ) {
        TreeModelNode modelNode = getModelNode( dataNode );
        if ( modelNode != root ) {
            TreeModelNode parentModelNode = modelNode.getParent();
            if ( parentModelNode != null ) {
                synchronized ( parentModelNode ) {
                    Object[] path = getPathToRoot( parentModelNode );
                    int index = parentModelNode.getChildren()
                                               .indexOf( modelNode );
                    listenerHandler
                   .fireTreeNodesChanged( this, path, new int[] { index }, 
                                          new Object[] { dataNode } );
                }
            }
        }
    }

    /**
     * Stops a node from expanding.  If the given node is currently 
     * undergoing expansion, calling this will stop it, leaving in 
     * place any children which have already been added to it.
     * Processing work associated with the expansion should be stopped,
     * though this may not happen immediately.
     * If the given node is not undergoing expansion this method will 
     * have no effect.
     *
     * @param  dataNode  the node whose expansion is to be stopped
     */
    public void stopExpansion( DataNode dataNode ) {
        TreeModelNode modelNode = getModelNode( dataNode );
        synchronized ( modelNode ) {
            NodeExpander expander = modelNode.getExpander();
            if ( expander != null && ! expander.isStopped() ) {
                expander.stop();
            }
        }
    }

    /**
     * Returns the <code>TreeModelNode</code> which acts as the container
     * for a given data node.
     *
     * @param  dataNode  the data node whose model node is required
     * @return   <code>dataNode</code>'s container model node
     */
    public TreeModelNode getModelNode( DataNode dataNode ) {
        TreeModelNode modelNode = (TreeModelNode) nodeMap.get( dataNode );
        if ( modelNode == null ) {
            throw new IllegalStateException( "Node " + dataNode 
                                           + " does not exist in model" );
        }
        return modelNode;
    }

    /**
     * Creates a new TreeModelNode for use in this TreeModel.
     * Note that this method should be used rather than the 
     * TreeModelNode constructor, since this model needs to be able
     * to keep track of the nodes that have been created. 
     *
     * @param  dataNode  the DataNode managed by this TreeModelNode
     * @param  parent    the parent of this node (null for the root)
     */
    public TreeModelNode makeModelNode( DataNode dataNode,
                                        TreeModelNode parent ) {
        if ( nodeMap.containsKey( dataNode ) ) {
            throw new IllegalStateException( "DataNode " + dataNode + 
                                             " already exists in model" );
        }
        TreeModelNode modelNode = new TreeModelNode( dataNode, parent );
        nodeMap.put( dataNode, modelNode );
        return modelNode;
    }

    /**
     * Used to free resources from a model node which no longer appears
     * in the tree.
     * Removes entries in the node map for a given model node and all
     * its descendents.  This is important both to prevent memory leaks
     * and to keep track of the total number of nodes in this model.
     *
     * @param  node the node which will no longer be required
     */
    private void discardModelNode( TreeModelNode node ) {
        synchronized ( node ) {

            /* Stop any expansion in progress. */
            NodeExpander expander = node.getExpander();
            if ( expander != null && ! expander.isStopped() ) {
                expander.stop();
            }

            /* Recursively discard each child of this node. */
            for ( Iterator it = node.getChildren().iterator(); it.hasNext(); ) {
                TreeModelNode child = (TreeModelNode) it.next();
                discardModelNode( child );
            }

            /* Remove the entry for this node from dataNode-modelNode map. */
            nodeMap.remove( node.getDataNode() );
        }
    }

    /**
     * Discards the TreeModelNode which contains a given DataNode,
     * and inserts it into a new TreeModelNode instead.
     * The necessary housekeeping tasks are performed.
     * The new TreeModelNode is returned.  
     *
     * @param  dataNode  the node to repackage
     * @param  parent  the parent of the new modelnode
     * @return  the new TreeModelNode containing dataNode
     */
    private TreeModelNode repackage( DataNode dataNode, TreeModelNode parent ) {
        if ( ! nodeMap.containsKey( dataNode ) ) {
            throw new IllegalStateException( "DataNode " + dataNode + 
                                             " does not exist in model" ); 
        }
        TreeModelNode newModelNode = new TreeModelNode( dataNode, parent );
        nodeMap.put( dataNode, newModelNode );
        return newModelNode;
    }

    /**
     * Invokes a runnable, perhaps asynchronously, on the event dispatch
     * thread.
     *
     * @param   runnable  the Runnable to invoke
     */
    private void invokeLater( Runnable runnable ) {
        if ( SwingUtilities.isEventDispatchThread() ) {
            runnable.run();
        }
        else {
            SwingUtilities.invokeLater( runnable );
        }
    }
   
    /**
     * This helper class exists solely to promote the event firing methods
     * of DefaultTreeModel to public visibility.  An object of this class
     * is used by each DataNodeTreeModel instance to take care of 
     * tree model events.  The event handling code could be replicated
     * within DataNodeTreeModel, but it's very ugly.  DataNodeTreeModel
     * could use these by inheriting from DefaultTreeModel, but it's
     * more hygienic this way (it's clear that none of the other 
     * functionality is coming from DefaultTreeModel).
     */
    private static class TreeModelListenerHandler extends DefaultTreeModel {
        TreeModelListenerHandler() {
            super( new DefaultMutableTreeNode( null, false ) );
        }
        public void fireTreeNodesChanged( Object source, Object[] path,
                                          int[] indices, Object[] children ) {
            super.fireTreeNodesChanged( source, path, indices, children );
        }
        public void fireTreeNodesInserted( Object source, Object[] path,
                                           int[] indices, Object[] children ) {
            super.fireTreeNodesInserted( source, path, indices, children );
        }
        public void fireTreeNodesRemoved( Object source, Object[] path,
                                          int[] indices, Object[] children ) {
            super.fireTreeNodesRemoved( source, path, indices, children );
        }
        public void fireTreeStructureChanged( Object source, Object[] path,
                                              int[] indices, 
                                              Object[] children ) {
            super.fireTreeStructureChanged( source, path, indices, children );
        }
    }

}
