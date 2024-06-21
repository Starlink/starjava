package uk.ac.starlink.datanode.tree;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import uk.ac.starlink.connect.ConnectorAction;
import uk.ac.starlink.datanode.factory.CreationState;
import uk.ac.starlink.datanode.factory.DataNodeFactory;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.EmptyDataNode;
import uk.ac.starlink.datanode.nodes.IconFactory;
import uk.ac.starlink.datanode.nodes.NodeUtil;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;
import uk.ac.starlink.datanode.tree.select.NodeRootComboBox;
import uk.ac.starlink.datanode.tree.select.NodeRootModel;

/**
 * A component which presents a tree of nodes and allows navigation 
 * around it to locate a node of interest.  One node can be selected.
 * This is intended to be used in a similar way to the JFileChooser
 * component.
 * <p>
 * The general visual appearance of this component should be mostly 
 * self-explanatory.  Some buttons appear near the top:
 * <ul>
 * <li>Up:   Changes the root of the tree to the parent object of the
 *           current root.  In the typical case in which the current root
 *           is a directory, this means the root of the tree becomes the
 *           parent directory of the current root.
 * <li>Down: This takes the currently selected node (if there is one)
 *           and makes it into the new root node
 * <li>Home: Sets the root as the user's home directory.
 * </ul>
 * <p>
 * The protected {@link #isChoosable} method provides a way for subclasses
 * to restrict which nodes can be chosen from the component or dialog.
 * Subclasses may override this method to indicate which nodes are 
 * eligible, and node eligibility will be reflected visually by the
 * component (node names written in bold for choosable nodes, that sort
 * of thing), as well as controlling the enabled status of the selection
 * button etc.  The <code>getSearch*Action</code> methods provide actions 
 * which may be useful in this context; they will search through the 
 * tree recursively and expand it so that any choosable nodes are visible
 * to the user.  Note this may result in some non-choosable nodes being
 * visible (parents and siblings of choosable ones) too, but branches
 * which contain no choosable nodes will not be displayed expanded in the GUI.
 * <p>
 * The chooser initially contains a default set of root items,
 * and a suitable default (the current directory) is selected.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TreeNodeChooser extends JPanel implements TreeSelectionListener {

    private DataNodeJTree jtree;
    private DataNodeFactory nodeMaker = new DataNodeFactory();
    private InfoPanel infoPanel;
    private NodeRootComboBox rootSelector;
    private JLabel nameLabel;
    private JLabel typeLabel;
    private JLabel descLabel;
    private final JComponent logBox;
    private final JButton logButton;
    private final JLabel logLabel;
    private final JComponent controlBox;
    private final JComponent bottomBox;
    private JTextField gotoField;
    private Action chooseAction;
    private Action cancelAction;
    private Action upAction;
    private Action downAction;
    private Action homeAction;
    private Action searchSelectedAction;
    private Action searchAllAction;
    private DataNode chosenNode;
    private JDialog currentDialog;
    private Box buttonBox;
    private JComponent buttonPanel;

    private static Cursor busyCursor = new Cursor( Cursor.WAIT_CURSOR );
    private static Font yesFont;
    private static Font noFont;

    /**
     * Constructs a new chooser widget with no content.
     */
    public TreeNodeChooser() {
        setLayout( new BorderLayout() );
        Border gapBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
        Border etchedBorder = BorderFactory.createEtchedBorder();
        setBorder( gapBorder );
        Box topBox = Box.createVerticalBox();
        JPanel mainPanel = new JPanel();
        bottomBox = Box.createVerticalBox();
        add( topBox, BorderLayout.NORTH );
        add( bottomBox, BorderLayout.SOUTH );

        /* Set up the selector for choosing the root node. */
        Box rootBox = Box.createHorizontalBox();
        rootSelector = new NodeRootComboBox( nodeMaker );
        rootSelector.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                if ( evt.getStateChange() == ItemEvent.SELECTED ) {
                    DataNode node = (DataNode) evt.getItem();
                    if ( node != null ) {
                        setRootNode( node );
                    }
                    setConnectorAction( rootSelector.getConnectorAction() );
                }
            }
        } );
        rootBox.add( rootSelector );

        /* Add buttons for actions. */
        Action[] actions = configureActions();
        for ( int i = 0; i < actions.length; i++ ) {
            rootBox.add( Box.createHorizontalStrut( 10 ) );
            JButton butt = new JButton( actions[ i ] );
            butt.setMaximumSize( new Dimension( 100, 100 ) );
            butt.setText( null );
            rootBox.add( butt );
        }
        topBox.add( rootBox );
        topBox.add( Box.createVerticalStrut( 5 ) );

        /* Add a button to hold the login/logout button for remote 
         * filestores. */
        logBox = Box.createHorizontalBox();
        logButton = new JButton();
        logLabel = new JLabel();
        logBox.add( logLabel );
        logBox.add( Box.createHorizontalStrut( 5 ) );
        logBox.add( logButton );
        logBox.add( Box.createHorizontalGlue() );
        topBox.add( logBox );
        topBox.add( Box.createVerticalStrut( 5 ) );

        /* Construct and place the tree widget itself. */
        jtree = new DataNodeJTree( new DataNodeTreeModel() );
        jtree.addTreeSelectionListener( this );
        JScrollPane scroller = new JScrollPane( jtree );
        scroller.setBorder( etchedBorder );
        scroller.setPreferredSize( new Dimension( 400, 300 ) );
        add( scroller, BorderLayout.CENTER );

        /* Set a renderer which will indicate whether each node is 
         * choosable or not, as well as the normal function of
         * indicating whether it is expanding. */
        jtree.setCellRenderer( new DataNodeTreeCellRenderer() {
            Font[] fonts;
            protected void configureNode( DataNode node, boolean isExpanding ) {
                if ( fonts == null ) {
                    fonts = new Font[ 4 ];
                    fonts[ 0 ] = getFont();
                    fonts[ 1 ] = fonts[ 0 ].deriveFont( Font.ITALIC );
                    fonts[ 2 ] = fonts[ 0 ].deriveFont( Font.BOLD );
                    fonts[ 3 ] = fonts[ 1 ].deriveFont( Font.BOLD );
                }
                boolean isChoosable = node != null && isChoosable( node );
                setFont( fonts[ ( isExpanding ? 1 : 0 ) +
                                ( isChoosable ? 2 : 0 ) ] );

            //  This is probably visual overkill  //
            //  /* Grey out the icons for non-choosable nodes. */
            //  if ( ! isChoosable ) {
            //      Icon ic = getIcon();
            //      if ( ic instanceof ImageIcon ) {
            //          Image im = ((ImageIcon) ic).getImage();
            //          setIcon( new ImageIcon( GrayFilter
            //                                 .createDisabledImage( im ) ) );
            //      }
            //  }
            }
        } );

        /* Construct and place a button panel. */
        buttonBox = Box.createVerticalBox();
        bottomBox.add( buttonBox );

        /* Construct and place the info panel */
        infoPanel = new InfoPanel();
        nameLabel = new JLabel();
        typeLabel = new JLabel();
        descLabel = new JLabel();
        infoPanel.addItem( new JLabel( "Name:" ), nameLabel );
        infoPanel.addItem( new JLabel( "Type:" ), typeLabel );
        infoPanel.addItem( new JLabel( "Description:" ), descLabel );
        infoPanel.setBorder( BorderFactory
                            .createCompoundBorder( etchedBorder, gapBorder ) );
        bottomBox.add( Box.createVerticalStrut( 10 ) );
        bottomBox.add( infoPanel );

        /* Construct and place a location entry line. */
        Box gotoBox = Box.createHorizontalBox();
        gotoBox.add( new JLabel( "Go to: " ) );
        gotoField = new JTextField();
        gotoBox.add( gotoField );
        gotoField.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                attemptSetRootObject( gotoField.getText() );
            }
        } );
        bottomBox.add( Box.createVerticalStrut( 10 ) );
        bottomBox.add( gotoBox );

        /* Construct and place the control buttons. */
        cancelAction = 
            new DoneAction( false, "Cancel", "Abort chooser dialog" );
        chooseAction =
            new DoneAction( true, "Open", "Open selected item" );
        controlBox = Box.createHorizontalBox();
        controlBox.add( Box.createHorizontalGlue() );
        controlBox.add( new JButton( chooseAction ) );
        controlBox.add( Box.createHorizontalStrut( 5 ) );
        controlBox.add( new JButton( cancelAction ) );
        bottomBox.add( Box.createVerticalStrut( 10 ) );
        bottomBox.add( controlBox );

        /* Arrange for some actions to take place the first time the 
         * component is displayed. */
        addAncestorListener( new AncestorListener() {
            public void ancestorAdded( AncestorEvent evt ) {

                /* Configure action availablity.  Can't do this in the
                 * constructor, since it involves invocation of methods which
                 * may be overridden by subclasses. */
                configureActionAvailability( null );

                /* Ensure that these actions don't happen again. */
                removeAncestorListener( this );
            }
            public void ancestorMoved( AncestorEvent evt ) {}
            public void ancestorRemoved( AncestorEvent evt ) {}
        } );

        /* Initialise the content. */
        rootSelector.addDefaultRoots();
    }

    /**
     * Sets the root of the displayed tree to one made from a given object.
     * This attempts to create a DataNode from <code>obj</code> by feeding it
     * to the DataNodeFactory.
     *
     * @param obj  object from which to form new root node.
     */
    public void setRootObject( Object obj ) throws NoSuchDataException {
        DataNode node = nodeMaker.makeDataNode( null, obj );
        nodeMaker.fillInAncestors( node );
        setRootNode( node );
    }

    /**
     * Attempts to call setRootObject; if succesful a new DataNode made
     * from <code>obj</code> is installed  as the
     * tree's new root, otherwise, there's a beep.
     *
     * @param obj  object from which to form a new root node
     */
    private void attemptSetRootObject( Object obj ) {
        try {
            setRootObject( obj );
        }
        catch ( NoSuchDataException e ) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * Sets the root node of the tree.  The entire tree is replaced by
     * a new one based at the given node and expanded one level.
     *
     * @param  root   new root 
     */
    private void setRootNode( DataNode root ) {
        DataNode oldRoot = (DataNode) jtree.getModel().getRoot();

        /* No action if we have been asked to replace the root with the
         * same thing. */
        if ( oldRoot == root ||
             oldRoot != null && root != null && 
             ("" + oldRoot).equals( "" + root ) &&
             ("" + NodeUtil.getNodePath( oldRoot ))
            .equals( "" + NodeUtil.getNodePath( root ) ) ) {
            return;
        }

        /* Replace the whole model with a new one rooted at the given node.
         * This causes fewer threading problems than changing the root
         * node of the existing model. */
        jtree.setModel( new DataNodeTreeModel( root ) );
        setBusy( false );
        configureActionAvailability( null );
        jtree.expandPathLater( new TreePath( root ) );
        rootSelector.getModel().setSelectedItem( root );
    }

    /**
     * Returns the component which allows the user to select the current
     * root of the tree.
     *
     * @return   root combo box;
     */
    public NodeRootComboBox getRootSelector() {
        return rootSelector;
    }

    /**
     * Removes all roots from the root selector.
     */
    public void clear() {
        ((NodeRootModel) rootSelector.getModel()).removeAllElements();
    }

    /**
     * Sets the root of the tree to a new node, possibly one not already 
     * in the tree.
     *
     * @param   node  new root
     */
    public void setRoot( DataNode node ) {
        getNodeMaker().fillInAncestors( node );
        setRootNode( node );
    }

    /**
     * Returns the root of the tree displayed in this chooser.
     *
     * @return  root node
     */
    public DataNode getRoot() {
        return (DataNode) jtree.getModel().getRoot();
    }

    /**
     * Sets whether the panel containing the OK and Cancel buttons is
     * visible or not.
     *
     * @param  visible   true iff OK and Cancel buttons should be seen
     */
    public void setControlsVisible( boolean visible ) {
        boolean isvis = false;
        for ( int i = 0; i < bottomBox.getComponentCount(); i++ ) {
            if ( bottomBox.getComponent( i ) == controlBox ) {
                isvis = true;
            }
        }
        if ( isvis && ! visible ) {
            bottomBox.remove( controlBox );
        }
        else if ( ! isvis && visible ) {
            bottomBox.add( controlBox );
        }
    }

    /**
     * Returns the action which indicated that the currently selected node
     * is to be used.
     *
     * @return   choose action
     */
    public Action getChooseAction() {
        return chooseAction;
    }

    /**
     * Returns a panel into which extra buttons can be placed.
     *
     * @return  a box for buttons
     */
    public JComponent getButtonPanel() {
        if ( buttonPanel == null ) {
            buttonBox.add( Box.createVerticalStrut( 10 ) );
            buttonPanel = Box.createHorizontalBox();
            buttonBox.add( buttonPanel );
        }
        return buttonPanel;
    }

    /**
     * Called when the user has finished interacting with this chooser.
     * The <code>node</code> argument will be the DataNode which the user
     * has selected if s/he has selected one, or <code>null</code> if the
     * user pushed the cancel button.  This may be overridden to do
     * something useful as an alternative to using the {@link #chooseDataNode}
     * method.  The default implementation does nothing.
     *
     * @param  node  the selected node, or null
     */
    protected void selectNode( DataNode node ) {
    }

    /**
     * Pops up a modal dialog which asks the user for a DataNode.
     * The return value is a node which the user selected, or <code>null</code>
     * if no selection was made.
     *
     * @param  parent  the parent of the dialog
     * @param  buttonText  the text to appear on the 'choose' button
     *         (or <code>null</code> for default)
     * @param  title  the title of the dialog window
     *         (or <code>null</code> for default)
     * @return  the selected DataNode, or <code>null</code> if none was selected
     */
    public DataNode chooseDataNode( Component parent, String buttonText,
                                    String title ) {
        currentDialog = createDialog( parent );
        if ( ! currentDialog.isModal() ) {
            throw new IllegalStateException( 
                          "Return from createDialog is not modal" );
        }
        chooseAction.putValue( Action.NAME, 
                               buttonText == null ? "Choose" : buttonText );
        currentDialog.setTitle( title == null ? "Hierarchical browser"
                                              : title );
        chosenNode = null;
        currentDialog.setVisible( true );
        return chosenNode;
    }

    /**
     * Returns the path of the currently chosen node.
     *
     * @return  chosen node path, or <code>null</code> if none is chosen
     */
    public String getChosenPath() {
        return chosenNode == null ? null
                                  : NodeUtil.getNodePath( chosenNode );
    }

    /**
     * Returns the datanode which is currently selected in the GUI.
     *
     * @return  selected node
     */
    public DataNode getSelectedNode() {
        TreePath tpath = jtree.getSelectionPath();
        return tpath == null ? null
                             : (DataNode) tpath.getLastPathComponent();
    }

    /**
     * Constructs the dialog component used by the {@link #chooseDataNode} 
     * method.
     * This can be overridden by subclasses to customise the dialog's
     * appearance.
     *
     * @param   parent  the owner of the returned dialog
     * @return  modal dialog containing this chooser
     */
    protected JDialog createDialog( Component parent ) {
        Frame frame = parent instanceof Frame 
                    ? (Frame) parent
                    : (Frame) SwingUtilities.getAncestorOfClass( Frame.class,
                                                                 parent );
        String title = "Select node";
        JDialog dialog = new JDialog( frame, title, true );
        dialog.getContentPane().setLayout( new BorderLayout() );
        dialog.getContentPane().add( this, BorderLayout.CENTER );
        dialog.pack();
        dialog.setLocationRelativeTo( parent );
        return dialog;
    }

    /**
     * Invoked when the selection is changed to update the display 
     * according to the current selection.  Subclasses may override
     * this to customise the info panel.
     *
     * @param  node  the node currently selected
     */
    protected void showNodeDetail( DataNode node ) {
        infoPanel.setIcon( node == null ? null : node.getIcon() );
        if ( yesFont == null ) {
            yesFont = nameLabel.getFont();
            noFont = yesFont.deriveFont( Font.PLAIN );
        }
        Font font = ( node != null && isChoosable( node ) ) ? yesFont : noFont;
        nameLabel.setFont( font );
        typeLabel.setFont( font );
        descLabel.setFont( font );
        nameLabel.setText( node == null ? null : node.getName() );
        typeLabel.setText( node == null ? null : node.getNodeType() );
        descLabel.setText( node == null ? null : node.getDescription() );
    }

    /**
     * Indicates whether a given node is eligable to be chosen with
     * the Accept button.  The implementation in <code>TreeNodeChooser</code>
     * always returns <code>true</code>.
     *
     * @param  node  the node to test
     * @return  <code>true</code> iff <code>node</code> can be chosen
     *          by this chooser
     */
    protected boolean isChoosable( DataNode node ) {
        return node != null;
    }

    private void setBusy( boolean busy ) {
        jtree.setCursor( busy ? busyCursor : null );
    }

    private void setConnectorAction( ConnectorAction act ) {
        logButton.setAction( act );
        logLabel.setText( act == null ? null : act.getConnector().getName() );
        logBox.setVisible( act != null );
    }

    /**
     * This method, called from the constructor, constructs all the
     * actions associated with toolbar-type buttons and returns a 
     * list of them.
     */
    private Action[] configureActions() {

        /* Action for moving up a directory. */
        upAction = 
            new BasicAction( "Up", IconFactory.getIcon( IconFactory.UP ), 
                             "Move root up one level" ) {
                public void actionPerformed( ActionEvent evt ) {
                    setRootNode( getParent( getRoot() ) );
                }
            };

        /* Action for moving down a directory. */
        downAction = 
            new BasicAction( "Down", IconFactory.getIcon( IconFactory.DOWN ),
                             "Set root to selected node" ) {
                public void actionPerformed( ActionEvent evt ) {
                    DataNode selNode = getSelectedNode();
                    if ( selNode != null ) {
                        setRootNode( selNode );
                    }
                }
            };

        /* Action for returning to home directory. */
        Action homeAction =
            new BasicAction( "Home", IconFactory.getIcon( IconFactory.HOME ),
                             "User home directory" ) {
                public void actionPerformed( ActionEvent evt ) {
                    String home = System.getProperty( "user.home" );
                    attemptSetRootObject( new File( home ) );
                }
            };

        /* Action for recursively searching the whole tree. */
        searchAllAction =
            new BasicAction( "Search Tree", null, "Search the whole tree" ) {
                public void actionPerformed( ActionEvent evt ) {
                    showAllChoosable( getRoot() );
                }
            };

        /* Action for recursively searching the selected node. */
        searchSelectedAction =
            new BasicAction( "Search Selected", null,
                             "Search the selected node" ) {
                public void actionPerformed( ActionEvent evt ) {
                    DataNode node = getSelectedNode();
                    if ( node != null ) {
                        showAllChoosable( node );
                    }
                }
            };

        /* Return the actions for use in the toolbar thingy. */
        return new Action[] { upAction, downAction, homeAction, };
    }

    /**
     * Implements the TreeSelectionListener interface; public as an 
     * implementation detail.
     */
    public void valueChanged( TreeSelectionEvent evt ) {
        TreePath path = evt.getPath();
        DataNode node = path == null ? null
                                     : (DataNode) path.getLastPathComponent();
        configureActionAvailability( node );
    } 

    /**
     * Sets action availability based on what node is currently selected.
     *
     * @param  selectedNode  the selected node
     */
    private void configureActionAvailability( DataNode selectedNode ) {
        showNodeDetail( selectedNode );
        boolean isSelectedChoosable = selectedNode != null
                                   && isChoosable( selectedNode );
        boolean isSelectedParent = selectedNode != null 
                                && selectedNode.allowsChildren();
        upAction.setEnabled( getParent( (DataNode) jtree.getModel().getRoot() )
                             != null );
        chooseAction.setEnabled( isSelectedChoosable );
        downAction.setEnabled( isSelectedParent );
        searchSelectedAction.setEnabled( isSelectedParent );
    }

    /**
     * Returns the DataNodeFactory which is used for turning objects into
     * data nodes.  This method is used wherever a node factory is 
     * required, so subclasses may override it to change the node creation
     * behaviour.
     *
     * @return  the data node factory
     */
    public DataNodeFactory getNodeMaker() {
        return nodeMaker;
    }

    /**
     * Sets the DataNodeFactory which is used for turning objects into
     * data nodes.
     *
     * @param  nodeMaker  the new data node factory
     */
    public void setNodeMaker( DataNodeFactory nodeMaker ) {
        this.nodeMaker = nodeMaker;
    }

    /**
     * Opens up the tree recursively from a given node to display 
     * all the choosable items at any level.
     *
     * @param  startNode the node from which to start
     * @return  the thread in which the search is done
     */
    public Thread showAllChoosable( final DataNode startNode ) {
        final DataNodeTreeModel model = (DataNodeTreeModel) jtree.getModel();

        /* Set up a listener which will make sure that choosable nodes
         * become visible in the JTree, as well as being added to the model. */
        final TreePath startPath = 
            new TreePath( model.getPathToRoot( startNode ) );
        final TreeModelListener finderListener = new TreeModelListener() {
            public void treeNodesInserted( TreeModelEvent evt ) {
                final TreePath path = evt.getTreePath();
                if ( startPath.isDescendant( path ) &&
                     ! jtree.hasBeenExpanded( path ) ) {
                    Object[] children = evt.getChildren();
                    for ( int i = 0; i < children.length; i++ ) {
                        if ( isChoosable( (DataNode) children[ i ] ) ) {
                            jtree.expandPathLater( path );
                        }
                    }
                }
            }
            public void treeNodesChanged( TreeModelEvent evt ) {}
            public void treeNodesRemoved( TreeModelEvent evt ) {}
            public void treeStructureChanged( TreeModelEvent evt ) {}
        };
        model.addTreeModelListener( finderListener );

        /* Set up a thread to do the expansion. */
        Thread finder = new Thread( "Choosable finder: " + startNode ) {
            public void run() {
                jtree.recursiveExpand( model.getModelNode( startNode ) );
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        model.removeTreeModelListener( finderListener );
                        if ( model == jtree.getModel() ) {
                            setBusy( false );
                        }
                    }
                } );
            }
        };

        /* Start the thread. */
        setBusy( true );
        finder.start();
        return finder;
    }

    /**
     * Returns an action which will search the tree recursively for 
     * choosable nodes, starting from the selected node.
     * The tree is opened up so that all choosable nodes thus discovered
     * are visible.
     *
     * @return  action
     */
    public Action getSearchSelectedAction() {
        return searchSelectedAction;
    }

    /**
     * Returns an action which will search the entire tree for choosable
     * nodes, starting from the root.
     * The tree is opened up so that all choosable nodes thus discovered
     * are visible.
     *
     * @return  action
     */
    public Action getSearchAllAction() {
        return searchAllAction;
    }

    /**
     * Returns the parent node of a datanode.
     *
     * @param   node   node
     * @return   node parent, or null if it's a root
     */
    private static DataNode getParent( DataNode node ) {
        CreationState creator = node.getCreator();
        return creator == null ? null
                               : creator.getParent();
    }

    /**
     * Convenience extension of AbstractAction.
     */
    private abstract class BasicAction extends AbstractAction {
        public BasicAction( String name, Icon icon, String shortdesc ) {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, shortdesc );
        }
    }

    /**
     * Helper class which provides actions for when one of the buttons
     * is pushed indicating the end of the user interaction.
     */
    private class DoneAction extends BasicAction {
        boolean useSelection;

        /**
         * Create a new DoneAction.
         *
         * @param   useSelection  true for select, false for cancel
         * @param   name   action name
         * @param   shortdesc  action description
         */
        DoneAction( boolean useSelection, String name, String shortdesc ) {
            super( name, null, shortdesc );
            this.useSelection = useSelection;
        }

        public void actionPerformed( ActionEvent evt ) {

            /* Work out what node has been selected, if any. */
            DataNode node;
            if ( useSelection ) {
                node = getSelectedNode();
            }
            else {
                node = null;
            }

            /* Record what it was. */
            chosenNode = node;

            /* If we are inside a dialog, dispose it. */
            if ( currentDialog != null ) {
                currentDialog.dispose();
                currentDialog = null;
            }

            /* Invoke a handler. */
            selectNode( node );
        }
    }
}
