package uk.ac.starlink.treeview;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;

/**
 * Views a tree of DataNode objects.  The GUI provides a two-part 
 * panel for viewing, one containing the tree itself and the other which
 * contains additional information on a selected node if required.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class StaticTreeViewer extends JFrame {

/*
 * This class is implemented such that every node of the JTree is a
 * DefaultMutableObject, and the userObject of each of these
 * DefaultMutableObjects is a DataNode. 
 */

    private JTree tree;
    private DefaultTreeModel treeModel;
    private JSplitPane splitter;
    private JComponent blankDetail;
    private JViewport detailHolder;
    private boolean showDetail;
    private double splitHloc;
    private double splitVloc;
    private DetailProducer detailProducer;
    private TreeWillExpandListener expansionListener;
    private IconFactory iconMaker = IconFactory.getInstance();
    private JFileChooser fileChooser;
    private DataNodeFactory nodeMaker = new DataNodeFactory();
    private short initialLayout = DETAIL_BESIDE;
    private JComponent helpPanel;
    private DefaultMutableTreeNode demoNode;

    private Action rExpandSelAct;
    private Action rCollapseSelAct;
    private Action expandSelAct;
    private Action collapseSelAct;
    private Action helpAct;
    private Action deleteAct;
    private Action upAct;

    /** No details panel is displayed. */
    public static final short DETAIL_NONE = 0;

    /** Details panel is displayed below the main tree panel. */
    public static final short DETAIL_BELOW = 1;

    /** Details panel is displayed to the right of the main tree panel. */
    public static final short DETAIL_BESIDE = 2;

    /**
     * Constructs a viewer with a default window title.
     *
     * @param  root   root node of the tree to view.  All the children of
     *                this node will be visible on startup.
     */
    public StaticTreeViewer( DataNode root ) {
        this( root, "Tree Display", DETAIL_BESIDE );
    }

    /**
     * Constructs a viewer given a root node and a window title.
     *
     * @param  root   root node of the tree to view.  All the children of
     *                this node will be visible on startup.
     * @param  title  a string giving the title of the viewer window
     * @param  initialLayout  one of the values DETAIL_BESIDE, DETAIL_BELOW
     *                        or DETAIL_NONE indicating the initial positioning
     *                        of the detail window
     */
    public StaticTreeViewer( DataNode root, String title,
                             short initialLayout ) {

        /* Invoke the parent window constructor. */
        super( title );

        /* Construct a JTree from the supplied root. */
        final DefaultMutableTreeNode rootNode = 
            new DefaultMutableTreeNode( root );
        treeModel = new DefaultTreeModel( rootNode, true );
        tree = new JTree( treeModel );
        rootNode.setAllowsChildren( true );
        ensureHasChildren( rootNode );
    //  tree.setLargeModel( true );

        /* Configure visual aspects of the JTree. */
        tree.setShowsRootHandles( true );
        tree.setScrollsOnExpand( false );
        tree.setRootVisible( false );
        tree.putClientProperty( "JTree.lineStyle", "Angled" );

        /* Configure interpretation of button clicks and selections. */
        tree.setToggleClickCount( 2 );
        TreeSelectionModel selectModel = tree.getSelectionModel();
        selectModel.setSelectionMode( TreeSelectionModel
                                     .SINGLE_TREE_SELECTION );
        selectModel.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged( TreeSelectionEvent event ) {
                updateDetail( event.getNewLeadSelectionPath() );
            }
        } );

        /*
         * Configure the expansion listener.
         */
        expansionListener = new TreeWillExpandListener() {

            /* When a node is about to expand, determine and add its 
             * children. */
            public synchronized void treeWillExpand( TreeExpansionEvent evt ) 
                    throws ExpandVetoException {
                TreePath expPath = evt.getPath();
                final DefaultMutableTreeNode expNode = 
                    (DefaultMutableTreeNode) expPath.getLastPathComponent();
                new SwingWorker() {
                    public Object construct() {
                        ensureHasChildren( expNode );
                        return null;
                    }
                }.start();
            }

            /* When a node is about to collapse, make sure none of its children
             * is selected.  If this isn't done then the JTree transfers the
             * selection to the lowest still-visible parent, which is not 
             * what we want. */
       //       - actually, it is.
            public synchronized void treeWillCollapse( TreeExpansionEvent ev ) {
       //       TreePath collapsor = ev.getPath();
       //       TreePath selection = tree.getSelectionPath();
       //       if ( collapsor.isDescendant( selection ) &&
       //            collapsor != selection ) {
       //           tree.removeSelectionPath( selection );
       //       }
            }
        };
        tree.addTreeWillExpandListener( expansionListener );

        /*
         * Configure the tree cell renderer.  Note that we subclass the
         * DefaultTreeCellRenderer class here rather than just implementing 
         * the TreeCellRenderer interface to take advantage of some of
         * the JComponent methods DefaultTreeCellRenderer overrides for
         * performance reasons.
         */
        tree.setCellRenderer( new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent( JTree tree,
                    Object value, boolean selected, boolean expanded,
                    boolean leaf, int row, boolean hasFocus ) {
                DataNode node = (DataNode) ((DefaultMutableTreeNode) value)
                                          .getUserObject();
                TreeCellRenderer tcr = node.getTreeCellRenderer();
                return tcr.getTreeCellRendererComponent( tree, value, selected,
                                                         expanded, leaf, row,
                                                         hasFocus );
            }
        } );

        /* Set up default sizes for the two panels in the splitter. */
        Dimension treesize = new Dimension( 480, 430 );
        Dimension detailsize = new Dimension( 520, 430 );

        /* Construct the container for the tree. */
        final JScrollPane treePanel = new JScrollPane( tree );
        treePanel.setPreferredSize( treesize );

        /* Construct a panel to display statistics. */
        JPanel statter = new JPanel();
        final JLabel treeNodesLabel = new JLabel( " Visible nodes: " );
        final JLabel modelNodesLabel = new JLabel( " Total nodes: ");
        statter.setLayout( new GridLayout() );
        statter.add( treeNodesLabel );
        statter.add( modelNodesLabel );
        getContentPane().add( statter, BorderLayout.SOUTH );

        /* Construct the panel containing help text. */
        helpPanel = new HelpDetailViewer().getComponent();

        /* Set up a blank detail frame for use when there is no real detail. */
        blankDetail = new JPanel();
        blankDetail.setPreferredSize( detailsize );

        /* Set up the object to deal with asynchronous interrogation of 
         * nodes. */
        detailProducer = new DetailProducer();

        /* Construct the container for the detailed view. */
        detailHolder = new JViewport();
        detailHolder.setView( blankDetail );
        detailHolder.setPreferredSize( detailsize );

        /* Construct the split pane. */
        splitter = new JSplitPane();
        splitter.setTopComponent( treePanel );
        Dimension psize;
        switch ( initialLayout ) {
            case DETAIL_BESIDE:
                psize = new Dimension( treesize.width + detailsize.width,
                                       ( treesize.height 
                                       + detailsize.height ) / 2 );
                break;
            case DETAIL_BELOW:
                psize = new Dimension( ( treesize.width 
                                       + detailsize.width ) / 2,
                                         treesize.height + detailsize.height );
                break;
            case DETAIL_NONE:
                psize = new Dimension( treesize.width, treesize.height );
                break;
            default:
                throw new Error( "Invalid initialLayout value " 
                               + initialLayout );
        }
        splitter.setPreferredSize( psize );

        /* Add the menus and toolbars with associated actions. */
        configureActions();

        /* Add the content to the top-level.*/
        getContentPane().add( splitter, BorderLayout.CENTER );
        pack();

        /* Put things in the splitter. */
        configureSplitter( initialLayout );
        helpAct.actionPerformed( null );

        /* Set up listeners to keep the statistics displays up to date. */
        class StatWatcher implements TreeExpansionListener, TreeModelListener {
            private int nnode = countDescendants( rootNode );

            /* TreeExpansionListener methods. */
            public void treeExpanded( TreeExpansionEvent ev ) {
                showTreeCount();
            }
            public void treeCollapsed( TreeExpansionEvent ev ) {
                showTreeCount();
            }

            /* TreeModelListener methods. */
            public void treeNodesChanged( TreeModelEvent ev ) {
            }
            public void treeNodesInserted( TreeModelEvent ev ) {
                nnode += ev.getChildIndices().length;
                showNodeCount();
            }
            public void treeNodesRemoved( TreeModelEvent ev ) {
                Object[] children = ev.getChildren();
                for ( int i = 0; i < children.length; i++ ) {
                    nnode -= 1 + countDescendants( children[ i ] );
                }
                showNodeCount();
            }
            public void treeStructureChanged( TreeModelEvent ev ) {
                nnode = countDescendants( rootNode );
                showNodeCount();
            }

            /* Private methods. */
            private void showTreeCount() {
                treeNodesLabel.setText( " Visible nodes: " 
                                      + tree.getRowCount() );
            }
            private void showNodeCount() {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        modelNodesLabel.setText( " Total nodes: " + nnode );
                        showTreeCount();
                    }
                } );
            }
        }
        StatWatcher statWatcher = new StatWatcher();
        tree.addTreeExpansionListener( statWatcher );
        treeModel.addTreeModelListener( statWatcher );

        /* Set up a mouse listener for right-clicks. */
        tree.addMouseListener( new MouseAdapter() {
            public void mousePressed( MouseEvent evt ) {
                maybeShowPopup( evt );
            }
            public void mouseReleased( MouseEvent evt ) {
                maybeShowPopup( evt );
            }
            private void maybeShowPopup( MouseEvent evt ) {
                if ( evt.isPopupTrigger() ) {

                    /* Work out what node, if any, has been clicked on. */
                    int x = evt.getX();
                    int y = evt.getY();
                    TreePath path = tree.getPathForLocation( x, y );
                    if ( path != null ) {

                        /* Activate the popup menu. */
                        JPopupMenu popup = alterEgoPopup( path );
                        if ( popup != null ) {
                            popup.show( evt.getComponent(), x, y );
                        }
                        else {
                            beep();
                        }
                    }
                }
            }
        } ); 
      
        /* If we have just the one node, set it to expand, but do it in a
         * separate thread in case it is slow. */
        if ( treeModel.getChildCount( rootNode ) == 1 ) {
            final TreePath onlyChild = new TreePath( new Object[] {
                rootNode,
                treeModel.getChild( rootNode, 0 ),
            } );
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    tree.expandPath( onlyChild );
                }
            } );
        }
        else {
            statWatcher.showNodeCount();
        }
        splitter.revalidate();
    }

    private void setDetailPane( JComponent detail ) {
        detail.setMinimumSize( new Dimension( 100, 100 ) );
        detailHolder.setView( detail );
    }

    private static DataNode getDataNodeFromTreePath( TreePath tPath ) {
        DefaultMutableTreeNode tNode = 
            (DefaultMutableTreeNode) tPath.getLastPathComponent();
        return (DataNode) tNode.getUserObject();
    }

    /*
     * Counts the total number of nodes in the TreeModel descended from the
     * given node. 
     */
    private int countDescendants( Object node ) {
        int count = 0;
        for ( int i = treeModel.getChildCount( node ) - 1; i >= 0; i-- ) {
            count += 1 + countDescendants( treeModel.getChild( node, i ) );
        }
        return count;
    }

    /*
     * Ensures that a given node has all its children, adding them if 
     * necessary.
     * This method may be time-consuming, and should not be called from
     * the event-dispatching thread.  It will only add children if no
     * children have been added previously to the node.
     * Whether it actually adds children or not, it will call a notify
     * in another thread on the treeNode it is passed.
     */
    private void ensureHasChildren( DefaultMutableTreeNode treeNode ) {
        final DefaultMutableTreeNode tnode = treeNode;
        synchronized ( tnode ) {
            boolean willNotify = false;
            DataNode dataNode = (DataNode) tnode.getUserObject();
            if ( dataNode.allowsChildren() ) {

                /* Only proceed if children have not already been added to this
                 * node. */
                if ( treeModel.getChildCount( tnode ) == 0 ) { 
                    int nchild = 0;
                    Iterator cIt = dataNode.getChildIterator();
                    if ( cIt.hasNext() ) {
                        try {

                            /* Handle each child node in turn. */
                            while ( cIt.hasNext() ) {

                                /* Get the next child (this may be 
                                 * time-consuming). */
                                DataNode cdn = (DataNode) cIt.next();
                                final DefaultMutableTreeNode child = 
                                    new DefaultMutableTreeNode( cdn );
                                child.setAllowsChildren( cdn.allowsChildren() );
 
                                /* Queue a request to insert the child in the
                                 * tree.  First time round only, wait for 
                                 * it to complete - this ensures that this 
                                 * method cannot complete and thus relinquish
                                 * the node lock unless at least one child
                                 * has been added.  In this way we guarantee
                                 * that children cannot be added to the
                                 * same node twice. */
                                final int nc = nchild++;
                                Runnable inserter = new Runnable() {
                                    public void run() {
                                        treeModel.insertNodeInto( child, 
                                                                  tnode, nc );
                                    }
                                };
                                if ( nc == 0 ) {
                                    SwingUtilities.invokeAndWait( inserter );
                                }
                                else {
                                    SwingUtilities.invokeLater( inserter );
                                }
                            }

                            /* Put a thread on the event dispatcher queue to 
                             * call a notify on the tree node after all the
                             *  children have been added. */
                            SwingUtilities.invokeLater( new Runnable() {
                                public void run() {
                                    synchronized ( tnode ) {
                                        tnode.notify();
                                    }
                                }
                            } );
                            willNotify = true;
                        }
                        catch ( InterruptedException e ) {}
                        catch ( InvocationTargetException e ) {}
                    }
                }
            }

            /* If we have not already arranged to do so, make sure that a 
             * notify will be called on the treenode in a different thread. */
            if ( ! willNotify ) {
                new Thread() {
                    public void run() {
                        synchronized ( tnode ) {
                            tnode.notify();
                        }
                    }
                }.start();
            }
        }
    }


    private void configureActions() {
        /* 
         * Set up some actions.
         */

        /* Exit action. */
        Action exitAct = 
            new AbstractAction( "Exit", 
                                iconMaker.getIcon( IconFactory.EXIT ) ) {
            public void actionPerformed( ActionEvent event ) {
                doExit();
            }
        };
        exitAct.putValue( Action.SHORT_DESCRIPTION, 
                          "Exit the viewer" );

        /* Selected node simple collapse and expand actions. */
        collapseSelAct = 
            new AbstractAction( "Collapse selected",
                                iconMaker.getIcon( IconFactory.CLOSE ) ) {
            public void actionPerformed( ActionEvent event ) {
                tree.collapsePath( tree.getSelectionPath() );
            }
        };
        collapseSelAct.putValue( Action.SHORT_DESCRIPTION,
                                 "Close the selected node" );
        expandSelAct =
            new AbstractAction( "Expand selected",
                                iconMaker.getIcon( IconFactory.OPEN ) ) {
            public void actionPerformed( ActionEvent event ) {
                tree.expandPath( tree.getSelectionPath() );
            }
        };
        expandSelAct.putValue( Action.SHORT_DESCRIPTION,
                               "Open the selected node" );

        /* Selected node recursive collapse and expand actions. */
        rCollapseSelAct = 
            new AbstractAction( "Recursive collapse selected",
                                iconMaker.getIcon( IconFactory.EXCISE ) ) {
            public void actionPerformed( ActionEvent event ) {
                treeCollapse( tree.getSelectionPath() );
            }
        };
        rCollapseSelAct.putValue( Action.SHORT_DESCRIPTION, 
                                  "Recursively collapse the selected node" );
        rExpandSelAct = 
            new AbstractAction( "Recursive expand selected", 
                                iconMaker.getIcon( IconFactory.CASCADE ) ) {
            public void actionPerformed( ActionEvent event ) {
                treeExpand( tree.getSelectionPath() );
            }
        };
        rExpandSelAct.putValue( Action.SHORT_DESCRIPTION, 
                                "Recursively expand the selected node" );

        /* All nodes recursive collapse and expand actions. */
        Action rCollapseAllAct = 
            new AbstractAction( "Recursive collapse all" ) {
            public void actionPerformed( ActionEvent event ) {
                Enumeration cEn = 
                    ( (DefaultMutableTreeNode) treeModel.getRoot() )
                   .children();
                while ( cEn.hasMoreElements() ) {
                    TreePath topChild = 
                        new TreePath( new Object[] { treeModel.getRoot(),
                                                     cEn.nextElement() } );
                    treeCollapse( topChild );
                }
            }
        };
        rCollapseAllAct.putValue( Action.SHORT_DESCRIPTION, 
                                  "Recursively collapse the entire tree" );
        Action rExpandAllAct = new AbstractAction( "Recursive expand all" ) {
            public void actionPerformed( ActionEvent event ) {
                treeExpand( new TreePath( treeModel.getRoot() ) );
            }
        };
        rExpandAllAct.putValue( Action.SHORT_DESCRIPTION, 
                                "Recursively expand the entire tree" );

        /* Set viewing geometry actions. */
        Action detailBelowAct = 
            new AbstractAction( "Details below", 
                                iconMaker.getIcon( IconFactory.SPLIT_BELOW ) ) {
            public void actionPerformed( ActionEvent event ) {
                configureSplitter( DETAIL_BELOW );
            }
        };
        detailBelowAct.putValue( Action.SHORT_DESCRIPTION,
                                 "Display the node details below the tree" );
        Action detailBesideAct = 
            new AbstractAction( "Details beside",
                                iconMaker.getIcon( IconFactory
                                                  .SPLIT_BESIDE ) ) {
            public void actionPerformed( ActionEvent event ) {
                configureSplitter( DETAIL_BESIDE );
            }
        };
        detailBesideAct.putValue( Action.SHORT_DESCRIPTION,
                        "Display the node details to the right of the tree" );
        Action detailNoneAct = 
            new AbstractAction( "No details",
                                iconMaker.getIcon( IconFactory.SPLIT_NONE ) ) {
            public void actionPerformed( ActionEvent event ) {
                configureSplitter( DETAIL_NONE );
            }
        };
        detailNoneAct.putValue( Action.SHORT_DESCRIPTION,
                                "Do not display any node details" );

        /* Action for adding a new top-level file node to the tree. */
        Action chooseNewFileAct = 
            new AbstractAction( "Open file",
                                iconMaker.getIcon( IconFactory.LOAD ) ) {
            public void actionPerformed( ActionEvent event ) {
                if ( fileChooser == null ) {
                    fileChooser = new JFileChooser( "." );
                    fileChooser.setApproveButtonText( "Add node" );
                    fileChooser.setFileSelectionMode( JFileChooser
                                                     .FILES_AND_DIRECTORIES );
                }
                int retval = fileChooser
                            .showOpenDialog( StaticTreeViewer.this );
                if ( retval == JFileChooser.APPROVE_OPTION ) {
                    DefaultMutableTreeNode root = 
                        (DefaultMutableTreeNode) treeModel.getRoot();
                    File file = fileChooser.getSelectedFile();
                    DataNode dnode;
                    try {
                        dnode = nodeMaker.makeDataNode( DataNode.ROOT, file );
                    }
                    catch ( NoSuchDataException e ) {
                        dnode = nodeMaker.makeErrorDataNode( DataNode.ROOT, e );
                    }
                    dnode.setLabel( file.getAbsolutePath() );
                    DefaultMutableTreeNode tnode = 
                        new DefaultMutableTreeNode( dnode );
                    tnode.setAllowsChildren( dnode.allowsChildren() );
                    treeModel.insertNodeInto( tnode, root, 
                                              root.getChildCount() );
                    tree.scrollPathToVisible( new TreePath( tnode.getPath() ) );
                }
            }
        };
        chooseNewFileAct.putValue( Action.SHORT_DESCRIPTION,
            "Add a new node to the tree from the filesystem" );

        /* Action for adding a new top-level non-file node to the tree. */
        Action chooseNewNameAct = new AbstractAction( "Open name" ) {
            public void actionPerformed( ActionEvent event ) {
                String name = JOptionPane
                             .showInputDialog( "Name of the new node" );
                DataNode dnode;
                try {
                    dnode = nodeMaker.makeDataNode( DataNode.ROOT, name );
                }
                catch ( NoSuchDataException e ) {
                    dnode = nodeMaker.makeErrorDataNode( DataNode.ROOT, e );
                }
                dnode.setLabel( name );
                DefaultMutableTreeNode tnode =
                    new DefaultMutableTreeNode( dnode );
                tnode.setAllowsChildren( dnode.allowsChildren() );
                DefaultMutableTreeNode root = 
                    (DefaultMutableTreeNode) treeModel.getRoot();
                treeModel.insertNodeInto( tnode, root, root.getChildCount() );
                tree.scrollPathToVisible( new TreePath( tnode.getPath() ) );
            }
        };
        chooseNewNameAct.putValue( Action.SHORT_DESCRIPTION,
            "Add a new node to the tree by name" );

        /* Action for displaying demo data. */
        Action demoAct =
            new AbstractAction( "Display demo data",
                                iconMaker.getIcon( IconFactory.DEMO ) ) {
                public void actionPerformed( ActionEvent event ) {
                    if ( demoNode == null ) {
                        DataNode dnode;
                        try { 
                            dnode = new DemoDataNode();
                        }
                        catch ( NoSuchDataException e ) {
                            dnode = nodeMaker
                                   .makeErrorDataNode( DataNode.ROOT, e );
                        }
                        demoNode = new DefaultMutableTreeNode( dnode );
                        demoNode.setAllowsChildren( dnode.allowsChildren() );
                        DefaultMutableTreeNode root = 
                            (DefaultMutableTreeNode) treeModel.getRoot();
                        treeModel.insertNodeInto( demoNode, root, 0 );
                    }
                    TreePath tpath = new TreePath( demoNode.getPath() );
                    tree.scrollPathToVisible( tpath );
                    tree.setSelectionPath( tpath );
                }
            };
        demoAct.putValue( Action.SHORT_DESCRIPTION,
                          "Add demo data node at top of tree" );

        /* Action for showing help text. */
        helpAct = 
            new AbstractAction( "Show help text",
                                iconMaker.getIcon( IconFactory.HELP ) ) {
            public void actionPerformed( ActionEvent event ) {
               displayHelpComponent( helpPanel );
            }
        };
        helpAct.putValue( Action.SHORT_DESCRIPTION,
                          "Show help text in info panel" );

        /* Action for removing a top-level node from the tree. */
        deleteAct = 
            new AbstractAction( "Delete node from root",
                                iconMaker.getIcon( IconFactory.DELETE ) ) {
            public void actionPerformed( ActionEvent event ) {
                MutableTreeNode tnode = 
                    (MutableTreeNode) tree.getLastSelectedPathComponent();
                if ( tnode == demoNode ) {
                    demoNode = null;
                }
                treeModel.removeNodeFromParent( tnode );

                /* Post the help window so we don't have a blank detail pane. */
                setDetailPane( helpPanel );
            }
        };
        deleteAct.putValue( Action.SHORT_DESCRIPTION,
                            "Delete top level node from display" );

        /* Action for adding a new level of nodes to the tree. */
        upAct = 
            new AbstractAction( "Add node parent",
                                iconMaker.getIcon( IconFactory.UP ) ) {
            public void actionPerformed( ActionEvent event ) {
                TreePath tp = tree.getSelectionPath();
                DataNode dn = getDataNodeFromTreePath( tp );
                assert dn.hasParentObject(); // action only enabled if so
                Object parent = dn.getParentObject();
                DataNode pdn;
                boolean error = false;
                try { 
                    pdn = nodeMaker.makeDataNode( DataNode.ROOT, parent );
                }
                catch ( NoSuchDataException e ) {
                    pdn = nodeMaker.makeErrorDataNode( DataNode.ROOT, e );
                    error = true;
                }
                MutableTreeNode tnode =
                    (MutableTreeNode) tree.getLastSelectedPathComponent();
                MutableTreeNode root = (MutableTreeNode) treeModel.getRoot();
                MutableTreeNode ptnode = new DefaultMutableTreeNode( pdn );
                int pos = treeModel.getIndexOfChild( root, tnode );
                if ( ! error ) {
                    treeModel.removeNodeFromParent( tnode );
                }
                treeModel.insertNodeInto( ptnode, root, pos );
                tree.setSelectionRow( pos );
            }
        };
        upAct.putValue( Action.SHORT_DESCRIPTION,
                        "Replace a top-level node by its parent" );

        /* Configure a selection listener to control availability of actions. */
        tree.getSelectionModel()
            .addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged( TreeSelectionEvent evt ) {
                configActs();
            }
        } );

        /* Configure an expansion listener to control action availability. */
        tree.addTreeExpansionListener( new TreeExpansionListener() {
            public void treeCollapsed( TreeExpansionEvent evt ) {
                configActs();
            }
            public void treeExpanded( TreeExpansionEvent evt ) {
                configActs();
            }
        } );

        /* Set up initial availability of actions. */
        configActs();

        /*
         * Set up a toolbar.
         */
        JToolBar tools = new JToolBar();
        getContentPane().add( tools, BorderLayout.NORTH );

        /*
         * Set up menus.
         */
        JMenuBar mb = new JMenuBar();
        setJMenuBar( mb );

        /* Add file actions. */
        JMenu fileMenu = new JMenu( "File" );
        mb.add( fileMenu );
        fileMenu.add( chooseNewFileAct ).setIcon( null );
        fileMenu.add( chooseNewNameAct ).setIcon( null );
        fileMenu.add( exitAct ).setIcon( null );
        tools.add( exitAct );
        tools.add( chooseNewFileAct );
        tools.addSeparator();

        /* Add the detail geometry choices to both menu and toolbar. */
        JMenu viewMenu = new JMenu( "View" );
        mb.add( viewMenu );
        addButtonGroup( tools, viewMenu, new Action[] { detailBesideAct, 
                                                        detailBelowAct, 
                                                        detailNoneAct } );

        /* Add collapse/expand actions to menu and toolbar. */
        JMenu treeMenu = new JMenu( "Tree" );
        mb.add( treeMenu );
        treeMenu.add( deleteAct ).setIcon( null );
        treeMenu.add( upAct ).setIcon( null );
        treeMenu.add( collapseSelAct ).setIcon( null );
        treeMenu.add( expandSelAct ).setIcon( null );
        treeMenu.add( rCollapseSelAct ).setIcon( null );
        treeMenu.add( rExpandSelAct ).setIcon( null );
        treeMenu.add( rCollapseAllAct ).setIcon( null );
        treeMenu.add( rExpandAllAct ).setIcon( null );
        tools.add( collapseSelAct );
        tools.add( expandSelAct );
        tools.addSeparator();
        tools.add( rCollapseSelAct );
        tools.add( rExpandSelAct );
        tools.addSeparator();
        tools.add( upAct );
        tools.add( deleteAct );

        /* Add the help menu action. */
        JMenu helpMenu = new JMenu( "Help" );
        mb.add( Box.createHorizontalGlue() );
        mb.add( helpMenu );
        helpMenu.add( helpAct ).setIcon( null );
        helpMenu.add( demoAct ).setIcon( null );
        tools.addSeparator();
        tools.add( demoAct );
        tools.add( helpAct );
    }


    /*
     * Displays a JComponent, not associated with a selection, into the 
     * detail panel.  Ensures that the detail panel is visible first.
     */
    private void displayHelpComponent( JComponent comp ) {

        /* If there is a selection, deselect it. */
        if ( tree.getSelectionPath() != null ) {
            tree.clearSelection();
        }
        
        /* Check that the info panel is visible, and put the help text
         * into it. */
        if ( ! showDetail ) {
            configureSplitter( DETAIL_BESIDE );
        }
        setDetailPane( comp );
    }


    /*
     * Sets the availability of certain actions based on the current 
     * selection etc.
     */
    private void configActs() {
        boolean hasSelection = tree.getSelectionCount() > 0;
        if ( hasSelection ) {
            TreePath tp = tree.getSelectionPath();
            boolean isExpansible = ! tree.getModel()
                                    .isLeaf( tp.getLastPathComponent() );
            rExpandSelAct.setEnabled( isExpansible );
            rCollapseSelAct.setEnabled( isExpansible );
            if ( isExpansible ) {
                boolean isExpanded = tree.isExpanded( tp );
                expandSelAct.setEnabled( ! isExpanded );
                collapseSelAct.setEnabled( isExpanded );
            }
            else {
                expandSelAct.setEnabled( false );
                collapseSelAct.setEnabled( false );
            }
            boolean inRoot = ( tp.getPathCount() == 2 );
            deleteAct.setEnabled( inRoot );
            DataNode dn = getDataNodeFromTreePath( tp );
            upAct.setEnabled( inRoot && dn.hasParentObject() );
        }
        else {
            rExpandSelAct.setEnabled( false );
            rCollapseSelAct.setEnabled( false );
            expandSelAct.setEnabled( false );
            collapseSelAct.setEnabled( false );
            deleteAct.setEnabled( false );
            upAct.setEnabled( false );
        }
    }

  
    /*
     * Adds a group of radio buttons to both a given toolbar and a given
     * menu.  The same ButtonModel is used for the corresponding buttons
     * in each set, ensuring that changes in one set of buttons is 
     * reflected in the other set.
     */
    private void addButtonGroup( JToolBar bar, JMenu menu, Action[] acts ) {
        ButtonGroup grp = new ButtonGroup();
        Insets margin = new Insets( 0, 0, 0, 0 );
        for ( int i = 0; i < acts.length; i++ ) {
            Action act = acts[ i ];

            JToggleButton tbutt = new JToggleButton( act );
            tbutt.setText( null );
            tbutt.setMargin( margin );
            bar.add( tbutt );

            JRadioButtonMenuItem mbutt = new JRadioButtonMenuItem( act );
            mbutt.setIcon( null );
            mbutt.setModel( tbutt.getModel() );
            menu.add( mbutt );

            grp.add( tbutt );
            tbutt.setSelected( i == 0 );
        }
        bar.addSeparator();
    }


    /* 
     * Collapse all nodes in a tree recursively starting from a given path.
     * We actually remove these nodes from the tree.
     */
    private void treeCollapse( TreePath tpath ) {
        MutableTreeNode tnode = (MutableTreeNode) tpath.getLastPathComponent();
        for ( int i = treeModel.getChildCount( tnode ) - 1; i >= 0; i-- ) {
            MutableTreeNode tchild = (MutableTreeNode) treeModel
                                                      .getChild( tnode, i );
            treeModel.removeNodeFromParent( tchild );
        }

        /* Now remove the node and add it again.  In this way it will be 
         * examined again when it is next expanded.  We also check if it
         * was the current selection and reinstate this if necessary. */
        boolean selected = ( tree.getSelectionPath() == tpath );
        MutableTreeNode parent = (MutableTreeNode) tpath.getParentPath()
                                                        .getLastPathComponent();
        int pos = treeModel.getIndexOfChild( parent, tnode );
        treeModel.removeNodeFromParent( tnode );
        treeModel.insertNodeInto( tnode, parent, pos );
        if ( selected ) {
            tree.setSelectionPath( tpath );
        }
    }

    /*
     * Replace a node at the given path with a new one.  This is done
     * in a similar way to treeCollapse.  This also causes the new node
     * to be selected.  This is not essential, but avoids some display
     * anomalies, and it is not an unreasonable thing to do in any case.
     */
    private void replaceNode( TreePath tpath, MutableTreeNode tnode ) {
        TreePath parentPath = tpath.getParentPath();
        MutableTreeNode parent = (MutableTreeNode) parentPath
                                                  .getLastPathComponent();
        MutableTreeNode oldtnode = (MutableTreeNode) tpath
                                                    .getLastPathComponent();
        int pos = treeModel.getIndexOfChild( parent, oldtnode );
        treeModel.removeNodeFromParent( oldtnode );
        treeModel.insertNodeInto( tnode, parent, pos );

        /* Select the new node. */
        TreePath newpath = parentPath.pathByAddingChild( tnode );
        tree.setSelectionPath( newpath );
    }

    /*
     * Expand all nodes in a tree recursively starting from a given path.
     */
    private void treeExpand( TreePath treepath ) {
        final DefaultMutableTreeNode tnode = 
            (DefaultMutableTreeNode) treepath.getLastPathComponent();
        if ( tnode.getAllowsChildren() ) {
            final TreePath tpath = treepath;
            new SwingWorker() {
                public Object construct() {
                    try {

                        /* Expand the node without triggering the normal
                         * automatic expansion of children. */
                        synchronized ( expansionListener ) {
                            tree.removeTreeWillExpandListener(
                                    expansionListener );
                            SwingUtilities.invokeAndWait( new Runnable() {
                                public void run() {
                                    tree.expandPath( tpath );
                                }
                            } );
                            tree.addTreeWillExpandListener( expansionListener );
                        }

                        /* Make sure that all the children are in place 
                         * (if necessary wait until they have finished 
                         * being added) */
                        synchronized ( tnode ) {
                            ensureHasChildren( tnode );
                            tnode.wait();
                        }

                        /* Get the children, and invoke the expansion 
                         * recursively. */
                        Enumeration cEn = tnode.children();
                        while ( cEn.hasMoreElements() ) {
                            DefaultMutableTreeNode cnode = 
                                (DefaultMutableTreeNode) cEn.nextElement();
                            TreePath cpath = new TreePath( cnode.getPath() );
                            treeExpand( cpath );
                        }
                    }
                    catch ( InterruptedException e ) {}
                    catch ( InvocationTargetException e ) {}
                    return null;
                }
            }.start();
        }
    }

    /*
     * Creates the popup menu used for the node at tpath.
     * This contains a reload item and an item for each type of node
     * this node could be viewed as.  These two types of entry are
     * actually of exactly the same sort, except that the reload one
     * happens to be of the same DataNode class as that of the 
     * existing node.  The reload one is done on demand, the alteregos
     * are created preemptively (since otherwise we don't know which ones
     * to offer).  
     *
     * In both cases they work by retrieving the original creation
     * conditions from the datanode, and running them again to produce
     * one of each of the nodes it could possibly look like. 
     * If an item is selected from the menu, the old node is excised 
     * from the tree and replaced by one of the newly created ones.
     */
    private JPopupMenu alterEgoPopup( final TreePath tpath ) {
        final DataNode dn = getDataNodeFromTreePath( tpath );
        final CreationState creator = dn.getCreator();
        JPopupMenu popper = new JPopupMenu(); 
        if ( creator != null && creator.getObject() != null ) {
            final DataNodeBuilder origbuilder = creator.getBuilder();
            final Object cobj = creator.getObject();

            /* Add the reload menu item. */
            final Class origclass = dn.getClass();
            Action reload = new AbstractAction( "Reload", dn.getIcon() ) {
                public void actionPerformed( ActionEvent evt ) {
                    DataNode newdn;
                    try {
                        newdn = origbuilder.buildNode( cobj );
                        if ( newdn == null ) {
                            throw new NoSuchDataException( 
                                "Data no longer available" );
                        }
                        newdn.setLabel( dn.getLabel() );
                        newdn.setCreator( creator );
                    }
                    catch ( NoSuchDataException e ) {
                        newdn = new ErrorDataNode( e );
                        newdn.setCreator( creator );
                    }
                    DefaultMutableTreeNode newtn = 
                        new DefaultMutableTreeNode( newdn );
                    newtn.setAllowsChildren( newdn.allowsChildren() );
                    replaceNode( tpath, newtn );
                }
            };
            popper.add( reload );

            /* Add the alterego menu items, if any. */
            DataNodeFactory cfact = new DataNodeFactory();
            List builders = cfact.getBuilders();
            List alteregos = new ArrayList();
            Set nodetypes = new HashSet();
            nodetypes.add( dn.getClass() );
 
            /* Go through the builders available when the DataNode was first
             * created, trying to build a new DataNode with each one.
             * Assemble a list of possible alteregos in this way. 
             * If any of the datanodes created has the same class as one
             * we've already got, don't bother putting it on the menu.
             * Note this creates a lot of datanodes which probably will
             * not be required, so you wouldn't really want to do it
             * for every node ever made - OK for a popup menu but expensive
             * to have it done preemptively. */
            /* Should really do some node disposal here. */
            DataNode parent = creator.getParent();
            for ( Iterator bit = builders.iterator(); bit.hasNext(); ) {
                DataNodeBuilder builder = (DataNodeBuilder) bit.next();
                if ( builder.suitable( cobj.getClass() ) ) {
                    DataNode newdn1;
                    try {
                        newdn1 = builder.buildNode( cobj );
                    }
                    catch ( NoSuchDataException e ) {
                        newdn1 = null;
                    }
                    final DataNode newdn = newdn1;
                    if ( newdn == null || 
                         nodetypes.contains( newdn.getClass() ) ||
                         newdn instanceof ErrorDataNode ) {
                        // no new node, or broken one, or we've already got one
                    }
                    else {
                        newdn.setLabel( dn.getLabel() );
                        CreationState creat = 
                            new CreationState( cfact, builder, parent, cobj );
                        newdn.setCreator( creat );
                        String text = newdn.getNodeTLA() + ": "
                                    + newdn.toString();
                        Icon icon = newdn.getIcon();
                        Action act = new AbstractAction( text, icon ) {
                            public void actionPerformed( ActionEvent evt ) {
                                DefaultMutableTreeNode newtn =
                                    new DefaultMutableTreeNode( newdn );
                                newtn.setAllowsChildren( 
                                    newdn.allowsChildren() );
                                replaceNode( tpath, newtn );
                            }
                        };
                        alteregos.add( act );
                        nodetypes.add( newdn.getClass() );
                    }
                }
            }

            if ( alteregos.size() > 0 ) {
                popper.addSeparator();
                for ( Iterator it = alteregos.iterator(); it.hasNext(); ) {
                    popper.add( (Action) it.next() );
                }
            }
            return popper;
        }
        else {
            return null;
        }
    }


    /**
     * Configures the geometric layout of the display.
     *
     * @param config  a constant indicating the configuration of the split
     *                pane in which the tree and details panels are displayed.
     *                May be DETAIL_NONE for no details panel, 
     *                DETAIL_BELOW for a vertical split or 
     *                DETAIL_BESIDE for a horizontal split.
     */
    public void configureSplitter( short config ) {
        Component bottom = splitter.getBottomComponent();

        /* If we have not been here before, do some initialisation. */
        if ( bottom != null && bottom != detailHolder ) {
            double initloc = 0.5;
            splitHloc = initloc;
            splitVloc = initloc;
            splitter.setDividerLocation( initloc );
            splitter.setLastDividerLocation( splitter.getDividerLocation() );
        }

        /* In the event of no change, proceed no further. */
        else if ( bottom == null && config == DETAIL_NONE ||
                  bottom == detailHolder && 
                  ( config == DETAIL_BESIDE && 
                    splitter.getOrientation() == JSplitPane.HORIZONTAL_SPLIT ||
                    config == DETAIL_BELOW &&
                    splitter.getOrientation() == JSplitPane.VERTICAL_SPLIT ) ) {
            return;
        }

        /* Save the current position of the divider if it is valid. */
        else if ( bottom == detailHolder && 
                  splitter.getDividerLocation() > 0 ) {
            if ( splitter.getOrientation() == JSplitPane.HORIZONTAL_SPLIT ) {
                splitHloc = (double) splitter.getDividerLocation() 
                                   / splitter.getSize().getWidth();
            }
            else if ( splitter.getOrientation() == JSplitPane.VERTICAL_SPLIT ) {
                splitVloc = (double) splitter.getDividerLocation()
                                   / splitter.getSize().getHeight();
            }
        }

        /* Will we show data at all? */
        if ( config == DETAIL_BESIDE || config == DETAIL_BELOW ) {
            showDetail = true;
            splitter.setDividerSize( 4 );
            if ( bottom != detailHolder ) {
                splitter.setBottomComponent( detailHolder );
            }
            if ( config == DETAIL_BESIDE ) {
                splitter.setLastDividerLocation( 
                    (int) ( splitHloc * splitter.getSize().getWidth() ) );
                splitter.setOrientation( JSplitPane.HORIZONTAL_SPLIT );
                splitter.setDividerLocation( splitHloc );
            }
            else {
                splitter.setLastDividerLocation( 
                    (int) ( splitVloc * splitter.getSize().getHeight() ) );
                splitter.setOrientation( JSplitPane.VERTICAL_SPLIT );
                splitter.setDividerLocation( splitVloc );
            }
        }

        /* No details will be shown. */
        else if ( config == DETAIL_NONE ) {
            showDetail = false;
            if ( bottom != null ) {
                splitter.setLastDividerLocation( splitter
                                                .getDividerLocation() );
                splitter.setBottomComponent( null );
                splitter.setDividerSize( 0 );
            }
        }
        else {
            throw new Error( "Unknown option" );
        }

        /* Ensure the detail panel is up to date. */
        updateDetail( tree.getSelectionPath() );
    }

    public Image getIconImage() {
        Icon treeicon = IconFactory.getInstance()
                                   .getIcon( IconFactory.TREE_LOGO );
        if ( treeicon instanceof ImageIcon ) {
            return ((ImageIcon) treeicon).getImage();
        }
        else {
            return super.getIconImage();
        }
    }

    public String toString() {
        String result = "";
        for ( int i = 0; i < tree.getRowCount(); i++ ) {
            result += i + "   " 
                    + ( (DefaultMutableTreeNode) tree.getPathForRow( i )
                                                     .getLastPathComponent() )
                     .getUserObject()
                     .toString() + "\n";
        }
        return result;
    }

    /* This may be invoked by the event dispatcher, so should execute fast. */
    private void updateDetail( TreePath selection ) {
        DataNode snode = null;
        boolean blank = true;

        /* See if there is a detailed information panel to view. */
        if ( showDetail ) {
            if ( selection != null ) {
                snode = getDataNodeFromTreePath( selection );
                blank = ! snode.hasFullView();
            }
        }

        /* Prepare and show the details.  This may be time-consuming, so 
         * do it outside this thread.  Do it at a lower priority, so it does
         * not slow direct user actions (such as moving the selection). */
        if ( blank ) {
            Component detail = detailHolder.getView();
            if ( detail == helpPanel ) {
                setDetailPane( (JComponent) detail );
            }
            else {
                setDetailPane( blankDetail );
            }
        }
        else {
            final DataNode snode1 = snode;
            detailProducer.requestDetailsForNode( snode1 );
            new SwingWorker() {
                public Object construct() {
                    Thread here = Thread.currentThread();
                    here.setPriority( Math.max( here.getPriority() - 2,
                                      Thread.MIN_PRIORITY ) );
                    final JComponent details = detailProducer.getDetailPane();
                    if ( detailProducer.getDataNode() == snode1 ) {
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                setDetailPane( details );
                            }
                        } );
                    }
                    return null;
                }
            }.start();
        }
    }

    private void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    private class DetailProducer {
        private DataNode requestedNode;
        private DataNode completedNode;
        private JComponent completedDetail = blankDetail;

        void requestDetailsForNode( DataNode node ) {
            requestedNode = node;
        }

        synchronized JComponent getDetailPane() {
         
            /* Get the most recently requested node (note that the value of
             * requestedNode itself may change under us during this method). */
            DataNode node = requestedNode;

            /* Calculate the details for it - potentially time-consuming. */
            if ( node != null && completedNode != node ) {
                completedNode = node;
                completedDetail = node.hasFullView() ? node.getFullView()
                                                     : blankDetail;
            }
            return completedDetail;
        }

        synchronized DataNode getDataNode() {
            return completedNode;
        }
    }
      

    private void doExit() {
        dispose();
        System.exit( 0 );
    }

}
