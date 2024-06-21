package uk.ac.starlink.datanode.tree;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import uk.ac.starlink.connect.Branch;
import uk.ac.starlink.connect.Connection;
import uk.ac.starlink.connect.Connector;
import uk.ac.starlink.connect.ConnectorManager;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.nodes.BranchDataNode;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;

/**
 * A JTree configured to display a {@link DataNodeTreeModel}.
 * This class doesn't add much to {@link javax.swing.JTree}, but provides
 * by default a suitable cell renderer and guarantees that its model
 * is a <code>DataNodeTreeModel</code>.
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
     * @param  model  a <code>DataNodeTreeModel</code> object
     * @throws  ClassCastException  if <code>model</code> is not a
     *          <code>DataNodeTreeModel</code>
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
     * return value.  The JTree is expanded to show the new nodes.
     *
     * @param  dataNode  the node to expand
     * @return  the Thread in which the expansion is done
     */
    public Thread recursiveExpand( final DataNode dataNode ) {
        final TreeModelNode modelNode = model.getModelNode( dataNode );

        /* Set up a listener which will make sure that the nodes which
         * get given children by the recursive expansion get expanded
         * in the JTree itself, rather than just getting added to the model. */
        final TreePath startPath = 
            new TreePath( model.getPathToRoot( dataNode ) );
        final TreeModelListener expanderListener = new TreeModelListener() {
            public void treeNodesInserted( TreeModelEvent evt ) {
                TreePath path = evt.getTreePath();
                if ( startPath.isDescendant( path ) &&
                     ! hasBeenExpanded( path ) ) {
                    expandPathLater( path );
                }
            }
            public void treeNodesChanged( TreeModelEvent evt ) {}
            public void treeNodesRemoved( TreeModelEvent evt ) {}
            public void treeStructureChanged( TreeModelEvent evt ) {}
        };
        model.addTreeModelListener( expanderListener );

        /* Set up a thread to do the expansion. */
        Thread expander = new Thread( "Recursive expander: " + dataNode ) {
            public void run() {
                recursiveExpand( model.getModelNode( dataNode ) );
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        model.removeTreeModelListener( expanderListener );
                    }
                } );
            }
        };

        expander.start();
        return expander;
    }

    /**
     * Recursively expands a given model node.  The expansion is synchronous
     * and may be time-consuming, so this method should not be invoked in 
     * the event-dispatcher thread.  Only the model is affected, 
     * to reflect the new nodes in the visual appearance of the JTree
     * a suitable TreeModelListener has to be installed to listen for
     * new nodes being added and do suitable {@link #expandPathLater} calls.
     *
     * @param   modelNode  the node to expand
     */
    void recursiveExpand( TreeModelNode modelNode ) {
        NodeExpander newExpander;
        DataNode dataNode;
        synchronized ( modelNode ) {
            dataNode = modelNode.getDataNode();

            /* If this node has somehow been removed from the tree, 
             * give up now. */
            if ( ! model.containsNode( dataNode ) ) {
                return;
            }

            /* If this node is not childbearing, there is no work to do. */
            if ( ! dataNode.allowsChildren() ) {
                return;
            }

            /* See whether this node is already expanded or not. */
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

        /* Finally, recursively expand all the node's children. */
        for ( Iterator it = modelNode.getChildren().iterator();
              it.hasNext(); ) {
             recursiveExpand( (TreeModelNode) it.next() );
        }
    }

    /**
     * Submits a path expansion to the event queue for later invocation.
     * In several cases it seems that invoking 
     * {@link javax.swing.JTree#expandPath(javax.swing.tree.TreePath)} 
     * on the JTree directly, even from the event dispatch thread, has the
     * effect of messing up the tree's visual representation (leaving
     * gaps between nodes).  I'm guessing this is because JTree doesn't
     * expect expandPath to be performed during tree gui processing
     * which is already taking place.  Doing it like this seems to
     * alleviate the problem (I hope).
     *
     * @param   path  the path to expand
     */
    public void expandPathLater( final TreePath path ) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                DataNode node = (DataNode) path.getLastPathComponent();
                if ( model.containsNode( node ) ) {
                    expandPath( path );
                }
            }
        } );
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

    /**
     * Returns a set of actions which can make connections to known virtual
     * filestores, as supplied by 
     * {@link uk.ac.starlink.connect.ConnectorManager}.
     * Each action will result in the user being prompted for authorization
     * information, and if this is satisfactory, a new node representing
     * the root of the resulting virtual filestore being added to this tree.
     * 
     * @param  factory  data node factory which will be used to create
     *         new data nodes for any connections which are opened
     */
    public Action[] getConnectionActions( final DataNodeFactory factory ) {
        Connector[] connectors = ConnectorManager.getConnectors();
        Action[] connectActions = new Action[ connectors.length ];
        for ( int i = 0; i < connectActions.length; i++ ) {
            final Connector connector = connectors[ i ];
            Action connAct = new AbstractAction() { 
                public void actionPerformed( ActionEvent evt ) {
                    connectorLogin( connector, factory );
                }
            };
            String name = connector.getName();
            connAct.putValue( Action.NAME, name + " Connection" );
            connAct.putValue( Action.SMALL_ICON, connector.getIcon() );
            connAct.putValue( Action.SHORT_DESCRIPTION, 
                              "Browse files in a " + name + " filestore" );
            connectActions[ i ] = connAct;
        }
        return connectActions;
    }

    /**
     * Opens a connection for a given connector, prompting the user for
     * authorization information and adding a new node to this tree if
     * one results.
     *
     * @param   connector  the connector to sue
     * @param   factory  data node factory for creating the new node
     */
    private void connectorLogin( Connector connector,
                                 DataNodeFactory factory ) {
        Connection connection = 
            ConnectorManager.showConnectionDialog( this, connector );
        if ( connection != null ) {
            Branch branch = connection.getRoot();
            DataNode node;
            try {
                node = factory.makeDataNode( null, branch );
                if ( node instanceof BranchDataNode ) {
                    ((BranchDataNode) node).setConnection( connection );
                    ((BranchDataNode) node).setPath( branch.toString() );
                }
            }
            catch ( NoSuchDataException e ) {
                node = factory.makeErrorDataNode( null, e );
            }
            model.appendNode( node, (DataNode) model.getRoot() );
            TreePath tpath = new TreePath( model.getPathToRoot( node ) );
            scrollPathToVisible( tpath );
            setSelectionPath( tpath );
        }
    }

}
