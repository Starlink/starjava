package uk.ac.starlink.treeview;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
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
    private TreePath dragStarted;
    private TreePath dragNow;

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

        /* Set up drag'n'drop. */
        setDropTarget( new BasicDropHandler( this ) {
            protected boolean isDropLocation( Point loc ) {
                return getPathForLocation( loc.x, loc.y ) == null;
            }
        } );
        setTransferHandler( new DataNodeTransferHandler() );
        setDragEnabled( true );
        addMouseListener( new MouseAdapter() {
            public void mousePressed( MouseEvent evt ) {
                if ( ! SwingUtilities.isMiddleMouseButton( evt ) ) {
                    dragStarted = getPathForLocation( evt.getX(), evt.getY() );
                }
            }

            /* Note this paste is not the same as the
             * TransferHandler.getPasteAction paste - this one goes from
             * the system selection, not the platform clipboard.  
             * I got this code from grubbing through the 
             * javax.swing.text.DefaultCaret code. */
            public void mouseClicked( MouseEvent evt ) {
                if ( SwingUtilities.isMiddleMouseButton( evt ) &&
                     evt.getClickCount() == 1 ) {
                    TransferHandler th = getTransferHandler();
                    if ( th instanceof DataNodeTransferHandler ) {
                        ((DataNodeTransferHandler) th)
                       .pasteSystemSelection( DataNodeJTree.this );
                    }
                }
            }
        } );
        addMouseMotionListener( new MouseMotionListener() {
            public void mouseDragged( MouseEvent evt ) {
                if ( ! SwingUtilities.isMiddleMouseButton( evt ) ) {
                    dragNow = getPathForLocation( evt.getX(), evt.getY() );
                }
            }
            public void mouseMoved( MouseEvent evt ) {
            }
        } );

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
     * this happens asynchronously.  A new thread is created and started 
     * in which the model expansion (though not the JTree notification) 
     * is done synchronously.  This thread is available as the method's
     * return value.
     *
     * @param  dataNode  the node to expand
     * @return  the Thread in which the expansion is done
     */
    public Thread recursiveExpand( DataNode dataNode ) {
        final TreeModelNode modelNode = model.getModelNode( dataNode );
        Thread expander = new Thread( "Recursive expander: " + dataNode ) {
            public void run() {
                recursiveExpand( modelNode );
            }
        };
        expander.start();
        return expander;
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
            else if ( expander.isComplete() ) {
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
        synchronized ( modelNode ) {
            Object[] path = model.getPathToRoot( dataNode );
            if ( path != null ) {
                final TreePath tpath = new TreePath( path );
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        expandPath( tpath );
                    }
                } );
            }

            /* Finally, recursively expand all the node's children. */
            for ( Iterator it = modelNode.getChildren().iterator();
                  it.hasNext(); ) {
                 recursiveExpand( (TreeModelNode) it.next() );
            }
        }
    }

    /**
     * Returns the last place the mouse button was pressed on this component.
     * This is required by the DataNodeTransferHandler, since otherwise
     * there seems to be no way of knowing which node the drag gesture
     * referred to.  The way this is got hold of is not entirely reputable.
     *
     * @return  the node corresponding to the last node dragged
     */
    public DataNode getDraggedNode() {
        return dragStarted == null 
                   ?  null 
                   : (DataNode) dragStarted.getLastPathComponent();
    }

    /**
     * Returns the current position of the mouse if it's being dragged
     * on this component.  This is required by the DataNodeTransferHandler.
     *
     * @return  the node corresponding to the current drop position
     */
    public DataNode getDropNode() {
        return dragNow == null
                   ? null
                   : (DataNode) dragNow.getLastPathComponent();
    }

}
