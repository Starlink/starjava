package uk.ac.starlink.treeview;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
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
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

/**
 * A component which presents a tree of nodes and allows navigation 
 * around it to locate a node of interest.  One node can be selected.
 * This is intended to be used in a similar way to the JFileChooser
 * component.
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
    private DataNode chosenNode;
    private JDialog currentDialog;

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
        scroller.setPreferredSize( new Dimension( 400, 400 ) );
        add( scroller, BorderLayout.CENTER );

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
    }

    public TreeNodeChooser( DataNode root ) {
        this();
        setRoot( root );
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
            setRoot( nodeMaker.makeDataNode( null, obj ) );
        }
        catch ( NoSuchDataException e ) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * Sets the root node of the tree.  The entire tree is replaced by
     * a new one based at the given node and expanded one level.
     *
     * @param  rootnew root
     */
    public void setRoot( DataNode root ) {
        DataNode oldRoot = (DataNode) jtree.getModel().getRoot();

        /* No action if we have been asked to replace the root with the
         * same thing. */
        if ( oldRoot == root ||
             oldRoot != null && root != null && 
             ("" + oldRoot).equals( "" + root ) &&
             ("" + oldRoot.getPath()).equals( "" + root.getPath() ) ) {
            return;
        }

        /* Replace the whole model with a new one rooted at the given node.
         * This causes fewer threading problems than changing the root
         * node of the existing model. */
        jtree.setModel( new DataNodeTreeModel( root ) );
        upAction.setEnabled( root.hasParentObject() );
        jtree.expandPath( new TreePath( root ) );
        rootSelector.setBottomNode( root );
    }

    public DataNode getRoot() {
        return (DataNode) jtree.getModel().getRoot();
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

    public DataNode makeDataNode( DataNode parent, Object obj )
            throws NoSuchDataException {
        return nodeMaker.makeDataNode( parent, obj );
    }

    protected void showNodeDetail( DataNode node ) {
        infoPanel.setIcon( node == null ? null : node.getIcon() );
        nameLabel.setText( node == null ? null : node.getName() );
        typeLabel.setText( node == null ? null : node.getNodeType() );
        descLabel.setText( node == null ? null : node.getDescription() );
    }

    protected boolean isChoosable( DataNode node ) {
        return true;
    }

    /**
     * Implements the TreeSelectionListener interface.
     */
    public void valueChanged( TreeSelectionEvent evt ) {
        TreePath path = evt.getPath();
        DataNode node = path == null ? null
                                     : (DataNode) path.getLastPathComponent();
        showNodeDetail( node );
        chooseAction.setEnabled( isChoosable( node ) );
        downAction.setEnabled( node != null && node.allowsChildren() );
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
                    DataNode root = getRoot();
                    if ( root.hasParentObject() ) {
                        setRootObject( root.getParentObject() );
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

        /* Return the actions for use in the toolbar thingy. */
        return new Action[] { upAction, downAction, homeAction, };
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
