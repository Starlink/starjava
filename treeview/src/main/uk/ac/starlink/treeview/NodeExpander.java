package uk.ac.starlink.treeview;

import java.util.Iterator;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * Object which can obtain children from a node and add them to a TreeModel
 * as they become available.
 * While working, members of this class will check periodically 
 * whether they are still
 * responsible for expanding nodes by seeing whether the ModelNode on
 * whose behalf they are working still owns them as its NodeExpander.
 * If not, they will assume that they are no longer doing useful work
 * and will stop working. Subclasses ought to observe the same behaviour.
 *
 * @author   Mark Taylor (Starlink)
 */
public class NodeExpander {

    private boolean done;
    private DataNodeTreeModel treeModel;
    private TreeModelNode modelNode;

    /**
     * Constructs a new expander.
     *
     * @param  modelNode  the node to be expanded
     * @param  treeModel  the model into which children are to be added
     */
    public NodeExpander( DataNodeTreeModel treeModel,
                         TreeModelNode modelNode ) {
        this.treeModel = treeModel;
        this.modelNode = modelNode;
    }

    /**
     * Performs the actual node expansion.  All children of the model node's
     * data node are acquired and synchronously inserted into the tree 
     * model.  The tree model's listeners are notified asynchronously.
     * <p>
     * If this expander ceases to be active (is not owned by its model node) 
     * then expansion may be stopped.
     * <p>
     * This may be a slow operation, so should not be done in the
     * event dispatch thread.
     */
    public void expandNode() {
        DataNode dataNode = modelNode.getDataNode();

        /* Only proceed if this node can have children. */
        if ( dataNode.allowsChildren() ) {

            /* Get each child in turn from the data node. */
            for ( Iterator it = dataNode.getChildIterator(); it.hasNext(); ) {

                /* If we are no longer doing useful work, bail out. */
                if ( ! isActive() ) {
                    return;
                }

                /* Get the next child. */
                DataNode childDataNode = (DataNode) it.next();

                /* As long as we are still responsible, update the tree model
                 * with the new child. */
                if ( isActive() ) {
                    treeModel.appendNode( childDataNode, dataNode );
                }
            }
        }

        /* Record that we have finished. */
        setDone();
    }

    /**
     * Indicates that the node expansion has completed successfully.
     */
    protected void setDone() {
        done = true;
    }

    /**
     * Indicates whether node expansion has completed.
     *
     * @return  true iff all child nodes have been added to the parent
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Indicates whether this expander is still responsible for expanding
     * nodes in its parent node.  It will return false if it has been
     * relieved of this responsibility (is no longer the active NodeExpander
     * object of the node).
     *
     * @return  true iff this expander is still responsible for expanding
     */
    public boolean isActive() {
        return modelNode.getExpander() == this;
    }
}
