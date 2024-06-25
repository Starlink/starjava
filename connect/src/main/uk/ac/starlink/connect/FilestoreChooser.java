package uk.ac.starlink.connect;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Generalised file browser which can browse virtual remote filesystems
 * as well as the local filesystem.  The objects it holds are instances
 * of the {@link Node} interface.
 *
 * <p>Though written from scratch, this class is effectively a generalisation 
 * of {@link javax.swing.JFileChooser}. JFileChooser looks like it ought
 * to be generalisable by providing alternative 
 * <code>FileSystemView</code> implementations, but
 * I've tried it, and that way lies misery.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
public class FilestoreChooser extends JPanel {

    private final BranchComboBox branchSelector_;
    private final JList<Node> nodeList_;
    private final JScrollPane scroller_;
    private final JTextField nameField_;
    private final JButton logButton_;
    private final JComponent logBox_;
    private final JLabel logLabel_;
    private final PropertyChangeListener connectorWatcher_;
    private final Action upAction_;
    private final Action prevAction_;
    private final Action okAction_;
    private final Action[] navActions_;
    private final Component[] activeComponents_;
    private Branch currentBranch_;
    private Branch prevBranch_;
    private ConnectorAction connectorAction_;
    private List<ConnectorAction> connectorActions_;
    private static Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.connect" );

    /**
     * Constructs a FilestoreChooser with navigation buttons included.
     */
    public FilestoreChooser() {
        this( true );
    }

    /**
     * Constructs a FilestoreChooser with navigation buttons optionally
     * included.
     *
     * @param  includeButtons  whether to include navigation buttons in
     *         the component
     */
    public FilestoreChooser( boolean includeButtons ) {
        super( new BorderLayout() );

        /* Basic setup. */
        JPanel main = new JPanel( new BorderLayout() );
        add( main, BorderLayout.CENTER );
        Border gapBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
        Border etchBorder = BorderFactory.createEtchedBorder();
        List<Component> activeList = new ArrayList<Component>();

        /* Construct and place a chooser for the current directory. */
        branchSelector_ = new BranchComboBox();
        activeList.add( branchSelector_ );
        Box branchBox = Box.createHorizontalBox();
        branchBox.add( new JLabel( "Location: " ) );
        branchBox.add( branchSelector_ );
        branchBox.setBorder( gapBorder );
        add( branchBox, BorderLayout.NORTH );

        /* Action for moving up a directory. */
        Icon upIcon =
            new ImageIcon( FilestoreChooser.class.getResource( "Up24.gif" ) );
        upAction_ = new AbstractAction( "Up", upIcon ) {
            public void actionPerformed( ActionEvent evt ) {
                Branch parent = getBranch().getParent();
                if ( parent != null ) {
                    setBranch( parent );
                }
            }
        };
        upAction_.putValue( Action.SHORT_DESCRIPTION,
                            "Move to the parent directory" );

        /* Action for moving to home directory. */
        Icon homeIcon =
            new ImageIcon( FilestoreChooser.class.getResource( "Home24.gif" ) );
        final Branch homedir = getHomeBranch();
        Action homeAction = new AbstractAction( "Home", homeIcon ) {
            public void actionPerformed( ActionEvent evt ) {
                if ( homedir != null ) {
                    setBranch( homedir );
                }
            }
        };
        homeAction.putValue( Action.SHORT_DESCRIPTION,
                              "Return to home directory" );
        homeAction.setEnabled( homedir != null );

        /* Action for moving to previous directory. */
        Icon prevIcon =
            new ImageIcon( FilestoreChooser.class.getResource( "Back24.gif" ) );
        prevAction_ = new AbstractAction( "Back", prevIcon ) {
            public void actionPerformed( ActionEvent evt ) {
                if ( prevBranch_ != null ) {
                    setBranch( prevBranch_ );
                }
            }
        };
        prevAction_.putValue( Action.SHORT_DESCRIPTION,
                              "Back to previously selected directory" );
        prevAction_.setEnabled( prevBranch_ != null );

        /* Action for refreshing the file list. */
        Icon refreshIcon = 
            new ImageIcon( FilestoreChooser.class
                                           .getResource( "Refresh24.gif" ) );
        Action refreshAction = new AbstractAction( "Refresh", refreshIcon ) {
            public void actionPerformed( ActionEvent evt ) {
                refreshList();
            }
        };
        refreshAction.putValue( Action.SHORT_DESCRIPTION,
                                "Refresh list of files in current directory" );

        /* Add navigation action buttons if required. */
        navActions_ =
            new Action[] { homeAction, upAction_, prevAction_, refreshAction, };
        if ( includeButtons ) {
            for ( int ia = 0; ia < navActions_.length; ia++ ) {
                JButton button = new JButton( navActions_[ ia ] );
                button.setText( null );
                activeList.add( button );
                if ( ia > 0 ) {
                    branchBox.add( Box.createHorizontalStrut( 5 ) );
                    branchBox.add( button );
                }
            }
        }

        /* Button for login/logout.  This will only be visible if the current
         * branch represents a remote filesystem. */
        logBox_ = Box.createHorizontalBox();
        logButton_ = new JButton();
        logLabel_ = new JLabel();
        logBox_.add( logLabel_ );
        logBox_.add( Box.createHorizontalStrut( 5 ) );
        logBox_.add( logButton_ );
        logBox_.add( Box.createHorizontalGlue() );
        logBox_.setBorder( gapBorder );
        setConnectorAction( null );
        main.add( logBox_, BorderLayout.NORTH );

        /* Main JList containing nodes in the current branch. */
        nodeList_ = new JList<Node>();
        nodeList_.setCellRenderer( new NodeRenderer() );
        scroller_ = new JScrollPane( nodeList_ );
        scroller_.setBorder( BorderFactory.createCompoundBorder( gapBorder,
                                                                 etchBorder ) );
        scroller_.setPreferredSize( new Dimension( 450, 300 ) );
        main.add( scroller_, BorderLayout.CENTER );

        /* Text entry field for typing in the name of a file or directory. */
        nameField_ = new JTextField();
        activeList.add( nameField_ );
        Box nameBox = Box.createHorizontalBox();
        nameBox.add( new JLabel( "File Name: " ) );
        nameBox.add( nameField_ );
        nameBox.setBorder( gapBorder );
        add( nameBox, BorderLayout.SOUTH );

        /* Make sure we update state when a new branch selection is made
         * from the branch selector combo box. */
        branchSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                if ( evt.getStateChange() == ItemEvent.SELECTED ) {
                    setBranch( (Branch) evt.getItem() );
                }
            }
        } );

        /* Ensure that double-clicking or hitting return on one of the items
         * in the list, as well as hitting return in the text entry field,
         * count as indicating a selection. */
        okAction_ = new AbstractAction( "OK" ) {
            public void actionPerformed( ActionEvent evt ) {
                ok();
            }
        };
        nameField_.addActionListener( okAction_ );
        nodeList_.addMouseListener( new MouseAdapter() {
            public void mouseClicked( MouseEvent evt ) {
                if ( evt.getClickCount() == 2 ) {
                    ok();
                }
            }
        } );
        nodeList_.getInputMap()
                 .put( KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0 ),
                       "select" );
        nodeList_.getActionMap().put( "select", okAction_ );

        /* Keep the text in the selection box up to date with the current
         * selected node, and vice versa. */
        nodeList_.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                List<Node> selected = nodeList_.getSelectedValuesList();
                String text = null;
                if ( selected.size() == 1 ) {
                    text = selected.get( 0 ).getName();
                    nameField_.setText( text );
                }
            }
        } );
        nameField_.addFocusListener( new FocusListener() {
            public void focusGained( FocusEvent evt ) {
                nodeList_.clearSelection();
            }
            public void focusLost( FocusEvent evt ) {
            }
        } );

        /* Set up an object which can make sure the list is refreshed
         * when a connector logs in or out. */
        connectorWatcher_ = new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                if ( evt.getSource() instanceof ConnectorAction ) {
                    ConnectorAction connAct = (ConnectorAction) evt.getSource();
                    if ( branchSelector_.getConnectorAction() == connAct &&
                         evt.getPropertyName()
                            .equals( ConnectorAction.CONNECTION_PROPERTY ) ) {
                        refreshList();
                    }
                }
            }
        };
        activeComponents_ = activeList.toArray( new Component[ 0 ] );
    }

    /**
     * Constructs a new chooser pointing to a given branch.
     *
     * @param  branch  initial branch
     */
    public FilestoreChooser( Branch branch ) {
        this();
        setBranch( branch );
    }

    /**
     * Returns a mutable list of the connector instances used by this chooser.
     * You can manipulate this list either by adding/removing items from
     * it or overriding this method.
     *
     * @return  mutable list of {@link ConnectorAction} objects
     */
    public synchronized List<ConnectorAction> getConnectorActions() {
        if ( connectorActions_ == null ) {
            connectorActions_ = 
                new ArrayList<ConnectorAction>
                             ( Arrays.asList( ConnectorManager
                                             .getConnectorActions() ) );
        }
        return connectorActions_;
    }

    /**
     * Returns the action which is equivalent to hitting an OK button,
     * that is performing a selection.
     *
     * @return  OK action
     */
    public Action getOkAction() {
        return okAction_;
    }

    /**
     * Returns the actions which allow the user to do additional navigation.
     *
     * @return   navigation actions
     */
    public Action[] getNavigationActions() {
        return navActions_;
    }

    public void setEnabled( boolean enabled ) {
        if ( enabled != isEnabled() ) {
            okAction_.setEnabled( enabled );
            for ( int i = 0; i < activeComponents_.length; i++ ) {
                activeComponents_[ i ].setEnabled( enabled );
            }
            upAction_.setEnabled( enabled &&
                                  currentBranch_.getParent() != null );
        }
        super.setEnabled( enabled );
    }

    /**
     * Populate this browser with a default set of branches.
     * This includes the current directory and possibly some 
     * connectors for remote filestores.
     * The selection is also set to a sensible initial value 
     * (probably the current directory).
     */
    public void addDefaultBranches() {

        /* Note: there is a problem with listRoots on Windows 2000 - it
         * pops up a dialogue about empty removable drives (floppy, cd-rom).
         * See Java bug id #4711632.  There may be workarounds but I tried
         * for a bit and didn't manage (hard without a local win2000 machine),
         * so I've given up for now since there's probably not that many
         * win2000 users. */
        File[] fileRoots = File.listRoots();

        /* Add branches for local filesystems. */
        for ( int i = 0; i < fileRoots.length; i++ ) {
            File fileRoot = fileRoots[ i ];
            if ( fileRoot.isDirectory() && fileRoot.canRead() ) {
                branchSelector_.addBranch( new FileBranch( fileRoot ) );
            }

            /* Unreadable roots are quite common on MS Windows for, e.g.,
             * empty removable media drives. */
            else {
                logger_.info( "Local filesystem root " + fileRoot + 
                              " is not a readable directory" );
            }
        }

        /* Add branches for remote virtual filesystems. */
        ConnectorAction[] actions = ConnectorManager.getConnectorActions();
        for ( int i = 0; i < actions.length; i++ ) {
            branchSelector_.addConnection( actions[ i ] );
        }

        /* Try to set the current selection to something sensible. */
        File dir = new File( "." );
        try {
            dir = new File( System.getProperty( "user.dir" ) );
        }
        catch ( SecurityException e ) {
            logger_.warning( "Can't get current directory" );
        }
        if ( dir.isDirectory() ) {
            setBranch( new FileBranch( dir ) );
        }
        else {
            logger_.warning( "Can't read current directory" );
        }
    }

    /**
     * Sets the current selected branch.  This may or may not add a new
     * branch to the selector.
     * 
     * @param  branch  branch
     */
    public void setBranch( Branch branch ) {
        if ( branch != branchSelector_.getSelectedBranch() ) {
            prevBranch_ = branchSelector_.getSelectedBranch();
            prevAction_.setEnabled( prevBranch_ != null );
            branchSelector_.setSelectedBranch( branch );
        }
        if ( branch != currentBranch_ ) {
            currentBranch_ = branch;
            BoundedRangeModel scrollModel = 
                scroller_.getVerticalScrollBar().getModel();
            scrollModel.setValue( scrollModel.getMinimum() );
            refreshList();

            /* Add a login/logout button if it represents remote filespace. */
            setConnectorAction( branchSelector_.getConnectorAction() );
        }
        upAction_.setEnabled( branch.getParent() != null );
    }

    /**
     * Ensures that the list contains the correct children for the
     * currently selected branch.
     */
    public void refreshList() {
        final Branch branch = getBranch();
        if ( branch != null ) {
            new Thread( "Refresh Directory" ) {
                public void run() {
                    final Node[] children = branch.getChildren();
                    Arrays.sort( children, NodeComparator.getInstance() );
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            if ( getBranch() == branch ) {
                                nodeList_.setListData( children );
                            }
                        }
                    } );
               }
            }.start();
        }
    }

    /**
     * Returns the currently selected branch.
     *
     * @return   current branch
     */
    public Branch getBranch() {
        return branchSelector_.getSelectedBranch();
    }

    /**
     * Adds a new branch representing a connection to a remote service to
     * this chooser.
     *
     * @param  connAct connector action
     */
    public void addConnection( ConnectorAction connAct ) {
        branchSelector_.addConnection( connAct );
    }

    /**
     * Returns the array of all nodes currently selected.
     *
     * @return  array of selected nodes
     */
    public Node[] getSelectedNodes() {
        return nodeList_.getSelectedValuesList().toArray( new Node[ 0 ] );
    }

    /**
     * Returns the single selected node.  If more than one is selected,
     * null is returned.  A node is considered selected if its name is
     * currently entered in the text field (as well as if it's been 
     * selected in the list in the usual way).
     *
     * @return  unique selected node, or null
     */
    public Node getSelectedNode() {
        Node[] nodes = getSelectedNodes();
        if ( nodes.length == 1 ) {
            return nodes[ 0 ];
        }
        else if ( nodes.length == 0 ) {
            String name = nameField_.getText();
            if ( name != null && name.trim().length() > 0 ) {
                return getBranch().createNode( name );
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Invoked if the user hits the OK button or equivalent.
     * If the selected node is a branch, this is interpreted as a 
     * request to move to that branch.  If it's a leaf, then it 
     * counts as a final selection.
     */
    private void ok() {
        if ( okAction_.isEnabled() ) {
            Node node = getSelectedNode();
            if ( node instanceof Leaf ) {
                leafSelected( (Leaf) node );
            }
            else if ( node instanceof Branch ) {
                setBranch( (Branch) node );
            }
        }
    }

    /**
     * Configures the ConnectorAction for the currently selected branch
     * (it will only be non-null if the currently selected branch 
     * represents a connector).
     *
     * @param  connectorAction   new connector action (possibly null)
     */
    private void setConnectorAction( ConnectorAction connectorAction ) {
        if ( connectorAction != connectorAction_ ) {
            if ( connectorAction_ != null ) {
                connectorAction_
               .removePropertyChangeListener( connectorWatcher_ );
            }
            connectorAction_ = connectorAction;
            if ( connectorAction != null ) {
                logLabel_.setText( connectorAction.getConnector().getName() );
                logButton_.setAction( connectorAction );
                connectorAction
               .addPropertyChangeListener( connectorWatcher_ );
            }
        }

        /* Only display the logIn button if the action is non-null. */
        logBox_.setVisible( connectorAction != null );
    }

    /**
     * Returns an object which contains the state of this chooser.
     * The object is the ComboBoxModel which defines the state of the
     * selector at the top of the window which selects the current
     * branch.
     *
     * @return   data model for this chooser
     */
    public ComboBoxModel<Branch> getModel() {
        return branchSelector_.getModel();
    }

    /**
     * Sets the model which contains the state of this chooser.
     * The object is the ComboBoxModel which defines the state of the
     * selector at the top of the window which selects the current branch.
     * Note you can't just bung any old ComboBoxModel in here; it must
     * be one obtained from a {@link #getModel} call on another
     * <code>FilestoreChooser</code>.
     *
     * @param  model   data model to use
     */
    public void setModel( ComboBoxModel<Branch> model ) {
        branchSelector_.setModel( model );
    }

    /**
     * This method is called if a successful selection of a single leaf
     * has taken place.  The default implementation does nothing.
     *
     * @param   leaf  selected leaf (not null)
     */
    protected void leafSelected( Leaf leaf ) {
    }

    /**
     * Returns the user's home directory.
     *
     * @return  readable user.home directory, or null
     */
    private static Branch getHomeBranch() {
        try {
            String home = System.getProperty( "user.home" );
            if ( home != null ) {
                File homedir = new File( home );
                if ( homedir.isDirectory() && homedir.canRead() ) {
                    return new FileBranch( homedir );
                }
            }
            return null;
        }
        catch ( SecurityException e ) {
            return null;
        }
    }

    /**
     * Renderer for list items.
     */
    private static class NodeRenderer extends DefaultListCellRenderer {
        private Icon branchIcon = UIManager.getIcon( "Tree.closedIcon" );
        private Icon leafIcon = UIManager.getIcon( "Tree.leafIcon" );
        public Component getListCellRendererComponent( JList<?> list,
                                                       Object value,
                                                       int index, 
                                                       boolean isSelected,
                                                       boolean hasFocus ) {
            Icon icon = null;
            if ( value instanceof Branch ) {
                value = ((Branch) value).getName();
                icon = branchIcon;
            }
            else if ( value instanceof Leaf ) {
                value = ((Leaf) value).getName();
                icon = leafIcon;
            }
            Component comp = 
                super.getListCellRendererComponent( list, value, index, 
                                                    isSelected, hasFocus );
            if ( comp instanceof JLabel ) {
                ((JLabel) comp).setIcon( icon );
            }
            return comp;
        }
    }

    public static void main( String[] args ) {
        FilestoreChooser chooser =
            new FilestoreChooser() {
                public void leafSelected( Leaf leaf ) {
                    try {
                        java.io.InputStream istrm =
                            leaf.getDataSource().getInputStream();
                        for ( int c; ( c = istrm.read() ) >= 0; ) {
                            System.out.write( c );
                        }
                        System.exit( 0 );
                    }
                    catch ( java.io.IOException e  ) {
                        e.printStackTrace();
                    }
                }
            };
        chooser.addDefaultBranches();
        javax.swing.JFrame frame = new javax.swing.JFrame();
        frame.getContentPane().add( chooser );
        frame.setLocationRelativeTo( null );
        frame.pack();
        frame.setVisible( true );
    }
}
