package uk.ac.starlink.datanode.tree;

import java.util.Iterator;
import java.util.List;
import javax.swing.SwingUtilities;
import uk.ac.starlink.datanode.nodes.DataNode;

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

    private boolean stopped;
    private boolean complete;
    private DataNodeTreeModel treeModel;
    private TreeModelNode modelNode;
    private DataNode dataNode;

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
        this.dataNode = modelNode.getDataNode();
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

        /* If this node can't have children, it's easy. */
        if ( ! dataNode.allowsChildren() ) {
            stopped = true;
            complete = true;
        }

        /* Otherwise, we have to do the expansion. */
        else {
            repaintNode();

            /* Get each child in turn from the data node. */
            for ( Iterator it = dataNode.getChildIterator();
                  ! stopped && it.hasNext(); ) {

                /* Get the next child. */
                DataNode childDataNode = (DataNode) it.next();

                /* Stop if we are no longer responsible for expanding our
                 * node. */
                synchronized ( modelNode ) {
                    if ( modelNode.getExpander() != this ) {
                        stopped = true;
                    }

                    /* As long as we are still working, update the tree model
                     * with the new child. */
                    if ( ! stopped ) {
                        treeModel.appendNode( childDataNode, dataNode );
                    }
                }
            }

            /* Record that we have finished. */
            if ( ! stopped ) {
                stopped = true;
                complete = true;
            }
            repaintNode();
        }
    }

    /**
     * Interrupts the work of this expander.  It will not add any more
     * children to its node.  It should stop using processing resources
     * to this end, but that may not happen immediately.
     */
    public void stop() {
        stopped = true;
    }

    /**
     * Indicates whether node expansion has stopped happening.
     *
     * @return  true iff this expander is no longer working on expanding
     *          its node
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * Indicates whether node expansion completed successfully. 
     *
     * @return  true iff all child nodes have been added to the parent
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Flags the node for repainting.  This should be invoked when the
     * expansion status changes, since this may affect the node's visual
     * representation in the tree.
     */
    private void repaintNode() {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                if ( treeModel.containsNode( dataNode ) ) {
                    treeModel.repaintNode( dataNode );
                }
            }
        } );
    }
}
