package uk.ac.starlink.treeview;

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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
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
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

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
 * button etc.  The <tt>getSearch*Action</tt> methods provide actions 
 * which may be useful in this context; they will search through the 
 * tree recursively and expand it so that any choosable nodes are visible
 * to the user.  Note this may result in some non-choosable nodes being
 * visible (parents and siblings of choosable ones) too, but branches
 * which contain no choosable nodes will not be displayed expanded in the GUI.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TreeNodeChooser extends JPanel implements TreeSelectionListener {

    private DataNodeJTree jtree;
    private DataNodeFactory nodeMaker = new DataNodeFactory();
    private InfoPanel infoPanel;
    private NodeAncestorComboBox rootSelector;
    private JLabel nameLabel;
    private JLabel typeLabel;
    private JLabel descLabel;
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
        Box bottomBox = Box.createVerticalBox();
        add( topBox, BorderLayout.NORTH );
        add( bottomBox, BorderLayout.SOUTH );

        /* Set up the selector for choosing the root node. */
        Box rootBox = Box.createHorizontalBox();
        rootSelector = new NodeAncestorComboBox();
        rootSelector.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                if ( evt.getStateChange() == ItemEvent.SELECTED ) {
                    DataNode node = (DataNode) evt.getItem();
                    if ( node != null ) {
                        setRoot( node );
                    }
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
        topBox.add( Box.createVerticalStrut( 10 ) );

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
                boolean isChoosable = isChoosable( node );
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
                setRootObject( gotoField.getText() );
            }
        } );
        bottomBox.add( Box.createVerticalStrut( 10 ) );
        bottomBox.add( gotoBox );

        /* Construct and place the control buttons. */
        cancelAction = 
            new DoneAction( false, "Cancel", "Abort chooser dialog" );
        chooseAction =
            new DoneAction( true, "Open", "Open selected item" );
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add( Box.createHorizontalGlue() );
        buttonBox.add( new JButton( chooseAction ) );
        buttonBox.add( Box.createHorizontalStrut( 5 ) );
        buttonBox.add( new JButton( cancelAction ) );
        bottomBox.add( Box.createVerticalStrut( 10 ) );
        bottomBox.add( buttonBox );

        /* Ensure the actions are in an appropriate state. */
        configureActionAvailability( null );
    }

    /**
     * Constructs a new chooser widget with a given initial root.
     *
     * @param   root  the root of the hierarchy
     */
    public TreeNodeChooser( DataNode root ) {
        this();
        if ( root != null ) {
            setRoot( root );
        }
    }

    /**
     * Sets the root of the displayed tree to one made from a given object.
     * This attempts to create a DataNode from <tt>obj</tt> by feeding it
     * to the DataNodeFactory; if succesful it is installed  as the
     * tree's new root, otherwise, there's a beep.
     *
     * @param  object from which to form a new root node
     */
    public void setRootObject( Object obj ) {
        try {
            setRoot( getNodeMaker().makeDataNode( null, obj ) );
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
    public void setRoot( DataNode root ) {
        DataNode oldRoot = (DataNode) jtree.getModel().getRoot();

        /* No action if we have been asked to replace the root with the
         * same thing. */
        if ( oldRoot == root ||
             oldRoot != null && root != null && 
             ("" + oldRoot).equals( "" + root ) &&
             ("" + TreeviewUtil.getNodePath( oldRoot ))
            .equals( "" + TreeviewUtil.getNodePath( root ) ) ) {
            return;
        }

        /* Replace the whole model with a new one rooted at the given node.
         * This causes fewer threading problems than changing the root
         * node of the existing model. */
        jtree.setModel( new DataNodeTreeModel( root ) );
        setBusy( false );
        configureActionAvailability( null );
        jtree.expandPath( new TreePath( root ) );
        rootSelector.setBottomNode( root );
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
     * Returns a panel into which extra buttons can be placed.
     *
     * @param  a box for buttons
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
     * The <tt>node</tt> argument will be the DataNode which the user
     * has selected if s/he has selected one, or <tt>null</tt> if the
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
     * The return value is a node which the user selected, or <tt>null</tt>
     * if no selection was made.
     *
     * @param  the parent of the dialog
     * @param  buttonText  the text to appear on the 'choose' button
     *         (or <tt>null</tt> for default)
     * @param  title  the title of the dialog window
     *         (or <tt>null</tt> for default)
     * @return  the selected DataNode, or <tt>null</tt> if none was selected
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
        currentDialog.show();
        return chosenNode;
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
     * This method is used whenever a new DataNode needs to be created
     * from an object.
     *
     * @param  parent  new data node's parent (may be null for root)
     * @param  obj     the object from which to construct the new data node
     * @return  the new data node based on <tt>obj</tt>
     */
    public DataNode makeDataNode( DataNode parent, Object obj )
            throws NoSuchDataException {
        return getNodeMaker().makeDataNode( parent, obj );
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
        Font font = isChoosable( node ) ? yesFont : noFont;
        nameLabel.setFont( font );
        typeLabel.setFont( font );
        descLabel.setFont( font );
        nameLabel.setText( node == null ? null : node.getName() );
        typeLabel.setText( node == null ? null : node.getNodeType() );
        descLabel.setText( node == null ? null : node.getDescription() );
    }

    /**
     * Indicates whether a given node is eligable to be chosen with
     * the Accept button.  The implementation in <tt>TreeNodeChooser</tt>
     * always returns <tt>true</tt>.
     *
     * @param  node  the node to test
     * @return  <tt>true</tt> iff <tt>node</tt> can be chosen by this chooser
     */
    protected boolean isChoosable( DataNode node ) {
        return true;
    }

    private void setBusy( boolean busy ) {
        jtree.setCursor( busy ? busyCursor : null );
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
                    Object parentObj = getRoot().getParentObject();
                    if ( parentObj != null ) {
                        setRootObject( parentObj );
                    }
                }
            };

        /* Action for moving down a directory. */
        downAction = 
            new BasicAction( "Down", IconFactory.getIcon( IconFactory.DOWN ),
                             "Set root to selected node" ) {
                public void actionPerformed( ActionEvent evt ) {
                    TreePath tpath = jtree.getSelectionPath();
                    if ( tpath != null ) {
                        setRoot( (DataNode) tpath.getLastPathComponent() );
                    }
                }
            };

        /* Action for returning to home directory. */
        Action homeAction =
            new BasicAction( "Home", IconFactory.getIcon( IconFactory.HOME ),
                             "User home directory" ) {
                public void actionPerformed( ActionEvent evt ) {
                    String home = System.getProperty( "user.home" );
                    setRootObject( new File( home ) );
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
                    TreePath tpath = jtree.getSelectionPath();
                    if ( tpath != null ) {
                        showAllChoosable( (DataNode) 
                                          tpath.getLastPathComponent() );
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
        upAction.setEnabled( ((DataNode) jtree.getModel().getRoot())
                            .getParentObject() != null );
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
     * @param  topNode the node from which to start
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
                            SwingUtilities.invokeLater( new Runnable() {
                                public void run() {
                                    if ( model == jtree.getModel() ) {
                                        jtree.expandPath( path );
                                    }
                                }
                            } );
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
            super( name, shortdesc );
            this.useSelection = useSelection;
        }
        public void actionPerformed( ActionEvent evt ) {

            /* If we are inside a dialog, dispose it. */
            if ( currentDialog != null ) {
                currentDialog.dispose();
                currentDialog = null;
            }

            /* Work out what node has been selected, if any. */
            DataNode node;
            if ( useSelection ) {
                node = (DataNode) jtree.getSelectionPath()
                                       .getLastPathComponent();
            }
            else {
                node = null;
            }

            /* Record what it was. */
            chosenNode = node;

            /* Invoke a handler. */
            selectNode( node );
        }
    }

}
