package uk.ac.starlink.treeview;

import java.util.Iterator;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * A JTree configured to display a {@link DataNodeTreeModel}.
 * This class doesn't add much to {@link javax.swing.JTree}, but provides
 * by default a suitable cell renderer and guarantees that its model
 * is a <tt>DataNodeTreeModel</tt>.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DataNodeJTree extends JTree { 
    private DataNodeTreeModel model;

    /**
     * Constructs a new DataNodeJTree from a suitable model.
     *
     * @param  model  the model representing the tree contents
     */
    public DataNodeJTree( DataNodeTreeModel model ) {
        super( model );

        /* Configure some visual aspects. */
        setRootVisible( false );
        setShowsRootHandles( true );
        putClientProperty( "JTree.lineStyle", "Angled" );

        /* Configure interpretation of button clicks and selections. */
        setToggleClickCount( 2 );
        getSelectionModel().setSelectionMode( TreeSelectionModel
                                             .SINGLE_TREE_SELECTION );

        /* Set up the cell renderer. */
        setCellRenderer( new DataNodeTreeCellRenderer() );
    }

    /**
     * Constructs a new DataNodeJTree from a given root node.
     *
     * @param   root  the root data node
     */
    public DataNodeJTree( DataNode root ) {
        this( new DataNodeTreeModel( root ) );
    }

    /**
     * Sets the model for this JTree to a given {@link DataNodeTreeModel}.
     *
     * @param  model  a <tt>DataNodeTreeModel</tt> object
     * @throws  ClassCastException  if <tt>model</tt> is not a
     *          <tt>DataNodeTreeModel</tt>
     */
    public synchronized void setModel( TreeModel model ) {
        this.model = (DataNodeTreeModel) model;
        super.setModel( model );
    }

    /**
     * Recursively expands a given data node.  As with normal expansion,
     * this happens asynchronously.
     *
     * @param  dataNode  the node to expand
     */
    public void recursiveExpand( DataNode dataNode ) {
        final TreeModelNode modelNode = model.getModelNode( dataNode );
        new Thread( "Recursive expander: " + dataNode ) {
            public void run() {
                recursiveExpand( modelNode );
            }
        }.start();
    }

    /**
     * Recursively expands a given model node.  The expansion is synchronous
     * and may be time-consuming, so this method should not be invoked in 
     * the event-dispatcher thread.
     *
     * @param   modelNode  the node to expand
     */
    private void recursiveExpand( TreeModelNode modelNode ) {

        /* If this node is not childbearing, there is no work to do. */
        DataNode dataNode = modelNode.getDataNode();
        if ( ! dataNode.allowsChildren() ) {
            return;
        }

        /* See whether this node is already expanded or not. */
        NodeExpander newExpander;
        synchronized ( modelNode ) {
            NodeExpander expander = modelNode.getExpander();

            /* If it's never been expanded, prepare to expand it now. */
            if ( expander == null ) {
                newExpander = new NodeExpander( model, modelNode );
            }

            /* If it's been expanded and the expansion is complete,
             * we won't have to expand it now. */
            else if ( expander.isDone() ) {
                newExpander = null;
            }

            /* If it is in the middle of an expansion, discard that
             * expansion and start a new (recursive) one.  This is
             * a bit messy and wasteful, but I think correct, and
             * it shouldn't happen very often. */
            else {
                model.refreshNode( dataNode );
                modelNode = model.getModelNode( dataNode );
                newExpander = new NodeExpander( model, modelNode );
            }

            /* Install the new node expander into the node which must
             * be expanded, if that is going to happen.  This must be
             * done while the lock is held on the node, but the actual
             * expansion should not be done holding this lock, since
             * it may be slow. */
            if ( newExpander != null ) {
                modelNode.setExpander( newExpander );
            }
        }

        /* Activate new node expander if we need to do this.
         * This step, which is executed synchronously, may be time-consuming. */
        if ( newExpander != null ) {
            newExpander.expandNode();
        }

        /* Make sure the tree knows that this node is open not shut. */
        final TreePath tpath = new TreePath( model.getPathToRoot( dataNode ) );
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                expandPath( tpath );
            }
        } );

        /* Finally, recursively expand all the node's children. */
        for ( Iterator it = modelNode.getChildren().iterator();
              it.hasNext(); ) {
             recursiveExpand( (TreeModelNode) it.next() );
        }
    }
}
