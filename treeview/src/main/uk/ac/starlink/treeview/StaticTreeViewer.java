package uk.ac.starlink.treeview;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import uk.ac.starlink.datanode.factory.CreationState;
import uk.ac.starlink.datanode.factory.DataNodeBuilder;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.nodes.ComponentMaker;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.ErrorDataNode;
import uk.ac.starlink.datanode.nodes.IconFactory;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;
import uk.ac.starlink.datanode.nodes.NodeUtil;
import uk.ac.starlink.datanode.tree.DataNodeJTree;
import uk.ac.starlink.datanode.tree.DataNodeTransferHandler;
import uk.ac.starlink.datanode.tree.DataNodeTreeModel;
import uk.ac.starlink.datanode.viewers.TextViewer;

/**
 * Main class for the Treeview application.  The GUI provides a two-part
 * panel for viewing, one containing the tree itself and the other which
 * contains additional information on a selected node if required.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StaticTreeViewer extends JFrame {

    private TreeSelectionModel selectModel;
    private DetailPlacer detailPlacer;
    private DataNodeJTree jtree;
    private DataNodeTreeModel treeModel;
    private DataNode root;
    private DemoDataNode demoNode;
    private JFileChooser fileChooser;
    private JComponent helpPanel;
    private JViewport detailHolder;
    private JSplitPane splitter;
    private JLabel treeNodesLabel;
    private JLabel modelNodesLabel;
    private boolean showDetail = true;
    private double splitHloc;
    private double splitVloc;
    private DataNodeFactory nodeMaker = new DataNodeFactory();
    private Map detailMap = new WeakHashMap();
    private Action helpAction;
    private Action collapseAction;
    private Action expandAction;
    private Action recursiveCollapseAction;
    private Action recursiveExpandAction;
    private Action reloadAction;
    private Action deleteAction;
    private Action upAction;
    private Action copyTopAction;
    private Action stopAction;

    /** No details panel is displayed. */
    public static final short DETAIL_NONE = 0;

    /** Details panel is displayed below the main tree panel. */
    public static final short DETAIL_BELOW = 1;

    /** Details panel is displayed to the right of the main tree panel. */
    public static final short DETAIL_BESIDE = 2;

    /**
     * Constructs a viewer with a default window title.
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
        this.root = root;

        /* Construct a JTree from the supplied root. */
        treeModel = new DataNodeTreeModel( root );
        jtree = new DataNodeJTree( treeModel );

        /* Arrange for drag'n'drop drops to be permitted. */
        ((DataNodeTransferHandler) jtree.getTransferHandler())
       .setNodeMaker( nodeMaker );

        /* Configure some custom aspects of the tree. */
        jtree.setScrollsOnExpand( false );

        /* Set up the selection model to update the detail panel on 
         * node selection. */
        selectModel = jtree.getSelectionModel();
        selectModel.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged( TreeSelectionEvent event ) {
                updateDetail( getSelectedDataNode() );
            }
        } );

        /* Set up default sizes for the two panels in the splitter. */
        Dimension treesize = new Dimension( 480, 430 );
        Dimension detailsize = new Dimension( 520, 430 );

        /* Construct the container for the tree. */
        final JScrollPane treePanel = new JScrollPane( jtree );
        treePanel.setPreferredSize( treesize );

        /* Construct a panel to display statistics. */
        JPanel statter = new JPanel();
        treeNodesLabel = new JLabel( " Visible nodes: " );
        modelNodesLabel = new JLabel( " Total nodes: ");
        statter.setLayout( new GridLayout() );
        statter.add( treeNodesLabel );
        statter.add( modelNodesLabel );
        getContentPane().add( statter, BorderLayout.SOUTH );

        /* Construct the panel containing help text. */
        helpPanel = new HelpDetailViewer().getComponent();

        /* Set up the object to deal with asynchronous interrogation of
         * nodes. */
        detailPlacer = new DetailPlacer();
        detailPlacer.start();

        /* Set up a blank detail frame for use when there is no real detail. */
        JPanel dummyDetail = new JPanel();
        dummyDetail.setPreferredSize( detailsize );

        /* Construct the container for the detailed view. */
        detailHolder = new JViewport();
        detailHolder.setView( dummyDetail );
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
                throw new AssertionError( "Invalid initialLayout value "
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
        helpAction.actionPerformed( null );

        /* Set up a listener to keep the number of visible nodes up to date. */
        jtree.addTreeExpansionListener( new TreeExpansionListener() {
            public void treeExpanded( TreeExpansionEvent evt ) {
                showTreeCount();
            }
            public void treeCollapsed( TreeExpansionEvent evt ) {
                showTreeCount();
            }
        } );

        /* Set up a listener to keep the total number of nodes up to date. */
        treeModel.addTreeModelListener( new TreeModelListener() {
            public void treeNodesInserted( TreeModelEvent evt ) {
                showNodeCount();
                showTreeCount();
            }
            public void treeNodesRemoved( TreeModelEvent evt ) {
                showNodeCount();
                showTreeCount();
            }
            public void treeStructureChanged( TreeModelEvent evt ) {
                showNodeCount();
                showTreeCount();
            }
            public void treeNodesChanged( TreeModelEvent evt ) {
                showNodeCount();
            }
        } );

        /* Set up a mouse listener for right-clicks. */
        jtree.addMouseListener( new MouseAdapter() {
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
                    TreePath path = jtree.getPathForLocation( x, y );
                    if ( path != null ) {

                        /* Activate the popup menu. */
                        JPopupMenu popup = alterEgoPopup( path );
                        if ( popup != null ) {
                            popup.show( evt.getComponent(), x, y );
                        }
                        else {
                            Toolkit.getDefaultToolkit().beep();
                        }
                    }
                }
            }
        } );

        /* Do a bit of special setup on the nodes at the top of the tree. */
        if ( root.allowsChildren() ) { // it had better

            /* Acquire all the children from the root node.  This would not
             * be efficient in general (child acquisition should be done
             * asynchronously under control of the DataNodeTreeModel), 
             * but we happen to know how the root node has been put 
             * together by the Driver class, and it will be OK. */
            int nnode = 0;
            for ( Iterator it = root.getChildIterator(); it.hasNext(); 
                  nnode++ ) {
                DataNode child = (DataNode) it.next();

                /* If there is only one child in the root, expand it. */
                if ( nnode == 0 && ! it.hasNext() ) {
                    Object[] path = treeModel.getPathToRoot( child );
                    jtree.expandPathLater( new TreePath( path ) );
                }

                /* If one of the nodes is a demo data node, keep track of
                 * it so that we don't create another one. */
                if ( child instanceof DemoDataNode ) {
                    demoNode = (DemoDataNode) child;
                }
            }
        }
    }

    /**
     * Set up some actions.
     */
    private void configureActions() {

        /* Exit action. */
        Action exitAction = 
            new BasicAction( "Exit",
                             IconFactory.getIcon( IconFactory.EXIT ),
                             "Exit the viewer" ) {
                public void actionPerformed( ActionEvent evt ) {
                    System.exit( 0 );
                }
            };

        /* Collapse selected node action. */
        collapseAction =
            new BasicAction( "Collapse Selected",
                             IconFactory.getIcon( IconFactory.CLOSE ),
                             "Close the selected node" ) {
                public void actionPerformed( ActionEvent evt ) {
                    jtree.collapsePath( jtree.getSelectionPath() );
                }
            };

        /* Expand selected node action. */
        expandAction =
            new BasicAction( "Expand Selected", 
                             IconFactory.getIcon( IconFactory.OPEN ),
                             "Open the selected node" ) {
                public void actionPerformed( ActionEvent evt ) {
                    jtree.expandPathLater( jtree.getSelectionPath() );
                }
            };

        /* Recursively collapse node action. */
        recursiveCollapseAction =
            new BasicAction( "Recursive Collapse Selected",
                             IconFactory.getIcon( IconFactory.EXCISE ),
                             "Recursively collapse the selected node" ) {
                public void actionPerformed( ActionEvent evt ) {
                    recursiveCollapse( getSelectedDataNode() );
                }
            };

        /* Recursively expand node action. */
        recursiveExpandAction =
            new BasicAction( "Recursive Expand Selected",
                             IconFactory.getIcon( IconFactory.CASCADE ),
                             "Recursively expand the selected node" ) {
                public void actionPerformed( ActionEvent evt ) {
                    recursiveExpand( getSelectedDataNode() );
                }
            };

        /* Recursively collapse whole tree action. */
        Action collapseAllAction =
            new BasicAction( "Recursive Collapse All",
                             null,
                             "Recursively collapse the entire tree" ) {
                public void actionPerformed( ActionEvent evt ) {
                    collapseAll();
                }
            };

        /* Recursively expand whole tree action. */
        Action expandAllAction =
            new BasicAction( "Recursive Expand All",
                             null,
                             "Recursively expand the entire tree" ) {
                public void actionPerformed( ActionEvent evt ) {
                    recursiveExpand( root );
                }
            };

        /* Actions for specifying position of detail panel. */
        class SplitPosAction extends BasicAction {
            short pos;
            SplitPosAction( String name, Icon icon, String desc, short pos ) {
                super( name, icon, desc );
                this.pos = pos;
            }
            public void actionPerformed( ActionEvent evt ) {
                configureSplitter( pos );
            }
        }
        Action detailBelowAction = 
            new SplitPosAction( "Details Below",
                                IconFactory.getIcon( IconFactory.SPLIT_BELOW ),
                                "Display node details below the tree",
                                DETAIL_BELOW );
        Action detailBesideAction =
            new SplitPosAction( "Details Beside",
                                IconFactory.getIcon( IconFactory.SPLIT_BESIDE ),
                                "Display node details to right of the tree",
                                DETAIL_BESIDE );
        Action detailNoneAction =
            new SplitPosAction( "No Details",
                                IconFactory.getIcon( IconFactory.SPLIT_NONE ),
                                "Do not display node details",
                                DETAIL_NONE );
 
        /* Action for adding a new top-level node to the tree. */
        Action newFileAction =
            new BasicAction( "Open File", 
                             IconFactory.getIcon( IconFactory.LOAD ),
                             "Add a new node to the tree from filesystem" ) {
                public void actionPerformed( ActionEvent evt ) {
                    chooseNewFile();
                }
            };

        /* Action for adding a new top-level non-file node to the tree. */
        Action newNameAction =
            new BasicAction( "Open Name",
                             null,
                             "Add a new node to the tree by name" ) {
                public void actionPerformed( ActionEvent evt ) {
                    chooseNewName();
                }
            };

        /* Demo data action. */
        Action demoAction = 
            new BasicAction( "Demo Data",
                             IconFactory.getIcon( IconFactory.DEMO ),
                             "Add demo data node at top of tree" ) {
                public void actionPerformed( ActionEvent evt ) {
                    addDemoData();
                }
            };
        try {
            DemoDataNode.getDemoDir();
        }
        catch ( NoSuchDataException e ) {
            demoAction.setEnabled( false );
        }

        /* Help text action. */
        helpAction =
            new BasicAction( "Show Help",
                             IconFactory.getIcon( IconFactory.HELP ),
                             "Show help text in details panel" ) {
                public void actionPerformed( ActionEvent evt ) {
                    displayHelpComponent( helpPanel );
                }
            };

        /* Refresh node action. */
        reloadAction =
            new BasicAction( "Reload Node",
                             IconFactory.getIcon( IconFactory.RELOAD ),
                             "Refresh node data" ) {
                public void actionPerformed( ActionEvent evt ) {
                    DataNode dn = getSelectedDataNode();
                    CreationState creator = dn.getCreator();
                    if ( creator == null || creator.getObject() == null ) {
                        Toolkit.getDefaultToolkit().beep();
                        return;
                    }
                    DataNode newdn;
                    try {
                        newdn = creator.getBuilder()
                               .buildNode( creator.getObject()  );
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
                    replaceNode( dn, newdn );
                }
            };

        /* Delete node from top level of tree action. */
        deleteAction =
            new BasicAction( "Delete Node",
                             IconFactory.getIcon( IconFactory.DELETE ),
                             "Delete top-level node from the tree" ) {
                public void actionPerformed( ActionEvent evt ) {
                    treeModel.removeNode( getSelectedDataNode() );
                }
            };

        /* Action for adding a new level of nodes to the tree. */
        upAction =
            new BasicAction( "Add Parent",
                             IconFactory.getIcon( IconFactory.UP ),
                             "Replace a top-level node by its parent" ) {
                public void actionPerformed( ActionEvent evt ) {
                    replaceWithParent( getSelectedDataNode() );
                }
            };

        /* Action for copying a node into the top level. */
        copyTopAction = 
            new BasicAction( "Copy To Root",
                             IconFactory.getIcon( IconFactory.DOWN ),
                             "Copy node into the top level" ) {
                public void actionPerformed( ActionEvent evt ) {
                    DataNode newNode = 
                        new DuplicateDataNode( getSelectedDataNode() );
                    treeModel.appendNode( newNode, root );
                }
            };

        /* Interrupt action. */
        stopAction =
            new BasicAction( "Stop expansion",
                             IconFactory.getIcon( IconFactory.STOP ),
                             "Interrupt any node expansion happening" ) {
                public void actionPerformed( ActionEvent evt ) {
                    recursiveStopExpansion( root );
                }
            };

        /* Actions for connecting to remote virtual filestores. */
        Action[] connectActions = jtree.getConnectionActions( nodeMaker );

        /* Configure a selection listener to control availability of actions
         * where this is related to what items are selected. */
        selectModel.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged( TreeSelectionEvent evt ) {
                configActs();
            }
        } );

        /* Configure an expansion listener to control availability of actions
         * where this is related to expansion status of nodes. */
        jtree.addTreeExpansionListener( new TreeExpansionListener() {
            public void treeCollapsed( TreeExpansionEvent evt ) {
                configActs();
            }
            public void treeExpanded( TreeExpansionEvent evt ) {
                configActs();
            }
        } );
    
        /* Set up initial availability of actions. */
        configActs();

        /* Set up a toolbar. */
        JToolBar tools = new JToolBar();
        getContentPane().add( tools, BorderLayout.NORTH );

        /* Set up menus. */
        JMenuBar mb = new JMenuBar();
        setJMenuBar( mb );

        /* Add file actions. */
        JMenu fileMenu = new JMenu( "File" );
        mb.add( fileMenu );
        fileMenu.add( newFileAction ).setIcon( null );
        fileMenu.add( newNameAction ).setIcon( null );
        for ( int i = 0; i < connectActions.length; i++ ) {
            fileMenu.add( connectActions[ i ] ).setIcon( null );
        }
        fileMenu.add( exitAction ).setIcon( null );
        tools.add( exitAction );
        tools.add( newFileAction );
        for ( int i = 0; i < connectActions.length; i++ ) {
            Action conact = connectActions[ i ];
            if ( conact.getValue( Action.SMALL_ICON ) != null ) {
                tools.add( conact );
            }
        }
        tools.addSeparator();

        /* Add the detail geometry choices to both menu and toolbar. */
        JMenu viewMenu = new JMenu( "View" );
        mb.add( viewMenu );
        addButtonGroup( tools, viewMenu, new Action[] { detailBesideAction,
                                                        detailBelowAction,
                                                        detailNoneAction } );

        /* Add collapse/expand actions to menu and toolbar. */
        JMenu treeMenu = new JMenu( "Tree" );
        mb.add( treeMenu );
        treeMenu.add( reloadAction ).setIcon( null );
        treeMenu.add( deleteAction ).setIcon( null );
        treeMenu.add( upAction ).setIcon( null );
        treeMenu.add( copyTopAction ).setIcon( null );
        treeMenu.add( collapseAction ).setIcon( null );
        treeMenu.add( expandAction ).setIcon( null );
        treeMenu.add( recursiveCollapseAction ).setIcon( null );
        treeMenu.add( recursiveExpandAction ).setIcon( null );
        treeMenu.add( collapseAllAction ).setIcon( null );
        treeMenu.add( expandAllAction ).setIcon( null );
        // treeMenu.add( stopAction ).setIcon( null ); // doesn't work properly
        tools.add( collapseAction );
        tools.add( expandAction );
        tools.add( recursiveCollapseAction );
        tools.add( recursiveExpandAction );
        // tools.add( stopAction );                    // doesn't work properly
        tools.addSeparator();
        tools.add( upAction );
        tools.add( copyTopAction );
        tools.add( reloadAction );
        tools.add( deleteAction );

        /* Add the help menu actions. */
        JMenu helpMenu = new JMenu( "Help" );
        mb.add( Box.createHorizontalGlue() );
        mb.add( helpMenu );
        helpMenu.add( helpAction ).setIcon( null );
        helpMenu.add( demoAction ).setIcon( null );
        tools.addSeparator();
        tools.add( demoAction );
        tools.add( helpAction );

        /* Add Starlink logo. */
        Image logoImage = 
            ((ImageIcon) IconFactory.getIcon( IconFactory.STAR_LOGO ))
           .getImage()
           .getScaledInstance( -1, 30, Image.SCALE_SMOOTH );
        tools.add( Box.createHorizontalGlue() );
        tools.addSeparator();
        tools.add( new JLabel( new ImageIcon( logoImage ) ) );
        tools.addSeparator();
    }

    /**
     * Sets the availability of certain actions based on the current
     * selection etc.
     */
    private void configActs() {
        DataNode selNode = getSelectedDataNode();
        if ( selNode != null ) {
            TreePath tp = jtree.getSelectionPath();
            boolean isExpansible = ! treeModel.isLeaf( selNode );
            CreationState creator = selNode.getCreator();
            boolean isReloadable = creator != null
                                && creator.getObject() != null;
            recursiveExpandAction.setEnabled( isExpansible );
            recursiveCollapseAction.setEnabled( isExpansible );
            if ( isExpansible ) {
                boolean isExpanded = jtree.isExpanded( tp );
                expandAction.setEnabled( ! isExpanded );
                collapseAction.setEnabled( isExpanded );
            }
            else {
                expandAction.setEnabled( false );
                collapseAction.setEnabled( false );
            }
            boolean inRoot = ( tp.getPathCount() == 2 );
            reloadAction.setEnabled( isReloadable );
            deleteAction.setEnabled( inRoot );
            upAction.setEnabled( inRoot && selNode.getParentObject() != null );
            copyTopAction.setEnabled( ! inRoot );
        }
        else {
            recursiveExpandAction.setEnabled( false );
            recursiveCollapseAction.setEnabled( false );
            expandAction.setEnabled( false );
            collapseAction.setEnabled( false );
            reloadAction.setEnabled( false );
            deleteAction.setEnabled( false );
            upAction.setEnabled( false );
            copyTopAction.setEnabled( false );
        }
    }

    /**
     * Returns the DataNode which is the current selection in the JTree,
     * or <tt>null</tt> if none is selected.
     *
     * @return  the selected node
     */
    private DataNode getSelectedDataNode() {
        return (DataNode) jtree.getLastSelectedPathComponent();
    }

    /**
     * Adds a group of radio buttons to both a given toolbar and a given
     * menu.  The same ButtonModel is used for the corresponding buttons
     * in each set, ensuring that changes in one set of buttons is
     * reflected in the other set.
     *
     * @param  bar  the toolbar
     * @param  menu  the menu
     * @param  acts  the actions to add
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

    /**
     * Display the number of visible nodes in the info panel. 
     */
    private void showTreeCount() {
        treeNodesLabel.setText( " Visible nodes: " + jtree.getRowCount() );
    }

    /**
     * Display the total number of nodes in the info panel.
     */
    private void showNodeCount() {

        /* Don't count the root node. */
        int nnode = treeModel.getNodeCount() - 1;
        modelNodesLabel.setText( " Total nodes: " + nnode );
    }

    /**
     * Displays a JComponent, not associated with a selection, into the 
     * detail panel.  Ensures that the detail panel is visible first.
     */
    private void displayHelpComponent( JComponent comp ) {

        /* If there is a selection, deselect it. */
        if ( jtree.getSelectionPath() != null ) {
            jtree.clearSelection();
        }

        /* Check that the info panel is visible, and put the help text
         * into it. */
        if ( ! showDetail ) {
            configureSplitter( DETAIL_BESIDE );
        }
        setDetailPane( comp );
    }

    private void replaceWithParent( DataNode node ) {
        Object parentObj = node.getParentObject();
        assert parentObj != null; // otherwise action disabled
        String path = NodeUtil.getNodePath( node );

        /* Get the new node. */
        DataNode parentNode;
        boolean error = false;
        try {
            parentNode = nodeMaker.makeDataNode( root, parentObj );
        }
        catch ( NoSuchDataException e ) {
            parentNode = nodeMaker.makeErrorDataNode( null, e );
            error = true;
        }

        /* Try to doctor the label. */
        String pname = node.getName();
        String psep = parentNode.getPathSeparator();
        if ( path != null && pname != null && path.indexOf( pname ) > 0 ) {
            path = path.substring( 0, path.length() - pname.length() );
            if ( psep != null && path.indexOf( psep ) > 0 ) {
                path = path.substring( 0, path.length() - psep.length() );
            }
            parentNode.setLabel( path );
        }

        /* Do the replacement. */
        replaceNode( node, parentNode );
    }

    private void recursiveCollapse( DataNode node ) {
        refreshNode( node );
    }

    private void collapseAll() {
        int nChild = treeModel.getChildCount( root );
        for ( int i = 0; i < nChild; i++ ) {
            DataNode node = (DataNode) treeModel.getChild( root, i );
            refreshNode( node );
        }
    }

    private void recursiveExpand( DataNode dataNode ) {
        jtree.recursiveExpand( dataNode );
    }

    private void recursiveStopExpansion( DataNode node ) {
        treeModel.stopExpansion( node );
        DataNode[] children = treeModel.getCurrentChildren( node );
        for ( int i = 0; i < children.length; i++ ) {
            recursiveStopExpansion( children[ i ] );
        }
    }

    private void chooseNewFile() {
        if ( fileChooser == null ) {
            fileChooser = new JFileChooser( "." ); 
            fileChooser.setApproveButtonText( "Add Node" );
            fileChooser.setFileSelectionMode( JFileChooser
                                             .FILES_AND_DIRECTORIES );
        }
        int retval = fileChooser.showOpenDialog( this );
        if ( retval == JFileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            DataNode node = nodeMaker.makeChildNode( null, file );
            node.setLabel( file.getAbsolutePath() );
            appendNodeToRoot( node );
        }
    }

    private void chooseNewName() {
        String name = JOptionPane
                     .showInputDialog( this, "Name of the new node" );
        if ( name == null || name.trim().length() == 0 ) {
            return;
        }
        DataNode dnode = nodeMaker.makeChildNode( null, name );
        dnode.setLabel( name );
        appendNodeToRoot( dnode );
    }

    private synchronized void addDemoData() {
        DataNode dnode = demoNode;

        /* If we don't already have a demo data node, try to construct one. */
        if ( dnode == null ) {
            try {
                dnode = new DemoDataNode();
                demoNode = (DemoDataNode) dnode;
            }
            catch ( NoSuchDataException e ) {
                dnode = nodeMaker.makeErrorDataNode( null, e );
            }
        }

        /* If the node doesn't exist in the tree, add it. */
        Object[] path = treeModel.getPathToRoot( dnode );
        if ( path == null ) {
            treeModel.insertNode( dnode, root, 0 );
            path = treeModel.getPathToRoot( dnode );
        }

        /* Set the selection to the demo node. */
        TreePath tpath = new TreePath( path );
        jtree.scrollPathToVisible( tpath );
        jtree.setSelectionPath( tpath );
    }

    /**
     * Appends a new DataNode to the root of the tree, and does some 
     * related visual housekeeping.
     *
     * @param  node  the node to append
     */
    private void appendNodeToRoot( DataNode node ) {
        treeModel.appendNode( node, root );
        TreePath tpath = new TreePath( treeModel.getPathToRoot( node ) );
        jtree.scrollPathToVisible( tpath );
        jtree.setSelectionPath( tpath );
    }

    /**
     * Replaces a node in the tree with a different one.  The replacement
     * appears collapsed, since it would be hard to reproduce the same
     * nested expansion status that the old one had.  The user can
     * expand it manually.
     *
     * @param   oldNode  node to replace
     * @param   newNode  node to replace it with
     */
    private void replaceNode( DataNode oldNode, DataNode newNode ) {
        TreePath path = new TreePath( treeModel.getPathToRoot( oldNode ) );
        jtree.collapsePath( path );
        boolean isSelected = path.equals( jtree.getSelectionPath() );
        treeModel.replaceNode( oldNode, newNode );
        if ( isSelected ) {
            TreePath newPath = path.getParentPath()
                                   .pathByAddingChild( newNode );
            jtree.setSelectionPath( newPath );
        }
    }

    /**
     * Refreshes a node in the tree; its children are removed from the
     * tree entirely and will be re-acquired from the DataNode if
     * the tree node is opened up again.
     *
     * @param  node  the node to refresh
     */
    private void refreshNode( DataNode node ) {
        TreePath path = new TreePath( treeModel.getPathToRoot( node ) );
        jtree.collapsePath( path );
        treeModel.refreshNode( node );
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
            throw new IllegalArgumentException( "Unknown option" );
        }

        /* Ensure the detail panel is up to date. */
        updateDetail( getSelectedDataNode() );
    }

    /**
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
        final DataNode dn = (DataNode) tpath.getLastPathComponent();
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
                    replaceNode( dn, newdn );
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
                        CreationState creat = new CreationState( parent, cobj );
                        creat.setFactory( cfact );
                        creat.setBuilder( builder );
                        newdn.setCreator( creat );
                        String text = newdn.getNodeTLA() + ": "
                                    + newdn.toString();
                        Icon icon = newdn.getIcon();
                        Action act = new AbstractAction( text, icon ) {
                            public void actionPerformed( ActionEvent evt ) {
                                replaceNode( dn, newdn );
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
     * Returns an icon for this viewer window.  This is specified by
     * JFrame and may be used by the windowing system during 
     * window minimisation etc.
     *
     * @return  icon
     */
    public Image getIconImage() {
        Icon treeicon = IconFactory.getIcon( IconFactory.TREE_LOGO );
        if ( treeicon instanceof ImageIcon ) {
            return ((ImageIcon) treeicon).getImage();
        }
        else {
            return super.getIconImage();
        }
    }

    /**
     * Puts the appropriate detail view in the detail panel for a given
     * data node.
     *
     * @param   dataNode the node whose details are to be displayed
     */
    private void updateDetail( final DataNode dataNode ) {

        /* If no detail panel is visible, no work needs to be done. */
        if ( ! showDetail ) {
            return;
        }

        /* If there's no selection, show something useful rather than 
         * nothing at all. */
        if ( dataNode == null  ) {
            setDetailPane( helpPanel );
        }

        /* Otherwise, arrange to show the appropriate detail component. */
        else {
            detailPlacer.requestDetails( dataNode );
        }
    }

    private void setDetailPane( JComponent detail ) {
        detail.setMinimumSize( new Dimension( 100, 100 ) );
        detailHolder.setView( detail );
    }

    private JComponent getDetailPane() {
        return (JComponent) detailHolder.getView();
    }


    /**
     * Returns the component which displays the detailed information
     * about a given data node.
     *
     * @param  node  the data node to describe
     * @return  the component which contains the detailed description
     */
    private JComponent getDetail( DataNode node ) {
        if ( ! detailMap.containsKey( node ) ) {
            detailMap.put( node, makeDetail( node ) );
        }
        return (JComponent) detailMap.get( node );
    }

    /**
     * Constructs the component which displays the detailed information
     * about a given data node.  These components are cached in a 
     * WeakHashMap, so that if the nodes to whom they belong are disposed
     * of, they can be discarded by the garbage collector.
     *
     * @param  node  the data node to describe
     * @return  the component which contains the detailed description
     */
    private JComponent makeDetail( DataNode node ) {

        /* Construct a viewer containing basic information about the node. */
        ApplicationDetailViewer dv = makeDetailViewer( node );
        dv.addSeparator();

        /* Allow the node to customise the viewer according to
         * its own knowledge of itself. */
        node.configureDetail( dv );

        /* Add debugging information if it is available. */
        CreationState creator = node.getCreator();
        if ( creator != null ) {
            final String trace = creator.getFactoryTrace();
            if ( trace != null ) {
                dv.addPane( "Construction trace", new ComponentMaker() {
                    public JComponent getComponent() {
                        return new TextViewer( new StringReader( trace ) );
                    }
                } );
            }
            DataNodeFactory factory = creator.getFactory();
            if ( factory != null && factory.getDebug() ) {
                dv.addSubHead( "Debug" );
                dv.addKeyedItem( "Node class", node.getClass().getName() );
                dv.addKeyedItem( "Parent node", creator.getParent() );
                dv.addKeyedItem( "Object", creator.getObject() );
                dv.addKeyedItem( "Object class", 
                                 creator.getObject().getClass().getName() );
                dv.addKeyedItem( "Builder", creator.getBuilder() );
            }
        }

        /* Return the component for display. */
        return dv.getComponent();
    }

    /**
     * Constructs a new Detail Viewer suitable for a given data node.
     *
     * @param   node  data node
     * @return   new viewer
     */
    private ApplicationDetailViewer makeDetailViewer( DataNode node ) {
        return new ApplicationDetailViewer( node );
    }

    /**
     * Helper class which handles getting detail components for selected nodes
     * and placing them in the detail panel.
     */
    private class DetailPlacer extends Thread {

        private DataNode requestNode;
        private DataNode workingNode;
        private DataNode completeNode;
        private JComponent completeDetail;

        /**
         * Initiates a request for the detail panel of a new node.
         * 
         * @param  node  the node we would like to represent in the
         *         detail panel
         */
        public synchronized void requestDetails( DataNode node ) {
            requestNode = node;
            interrupt();
        }

        /**
         * Loops, responding to requests for new detail panels.
         */
        public void run() {
            while ( true ) {
 
                /* Determine whether work needs to be done. */
                boolean done;
                if ( requestNode == null  ) {
                    done = true;
                }
                else if ( requestNode == completeNode ) {
                    done = true;
                }
                else {
                    done = false;
                }

                /* If no work is required, sleep until interrupt. */
                if ( done ) {
                    try {
                        sleep( Integer.MAX_VALUE );
                    }
                    catch ( InterruptedException e ) {
                        // continue
                    }
                }

                /* Otherwise, do the work in this thread, and arrange for
                 * it to be used in the event-dispatch thread. */
                else {
                    workingNode = requestNode;
                    completeDetail = getDetail( requestNode );
                    workingNode = null;
                    completeNode = requestNode;
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            setDetailPane( completeDetail );
                        }
                    } );
                }

                /* Clear interrupted status. */
                interrupted();
            }
        }
    }
}
