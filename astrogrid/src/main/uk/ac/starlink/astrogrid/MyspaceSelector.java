package uk.ac.starlink.astrogrid;

import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.astrogrid.community.common.exception.CommunityException;
import org.astrogrid.store.Ivorn;
import org.astrogrid.store.tree.Container;
import org.astrogrid.store.tree.Node;
import org.astrogrid.store.tree.TreeClient;
import org.astrogrid.store.tree.TreeClientException;

/**
 * Graphical component presenting a JTree which contains MySpace nodes.
 * As well as the tree itself, it contains a button for logging into
 * and out of MySpace.
 * It can be used to select an item in the tree.
 * To do something useful with it, subclass it and override the 
 * {@link #ok} and {@link #cancel} methods to do something.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Nov 2004
 */
public class MyspaceSelector extends JPanel {

    private final Action logAction_;
    private final Action okAction_;
    private final Action cancelAction_;
    private final JTree jtree_;
    private final JPanel extraPanel_;
    private final JLabel ivornLabel_;
    private final JLabel locLabel_;
    private final TreeModel emptyTreeModel_;
    private AGConnector connector_;
    private TreeClient treeClient_;
    private boolean allowContainerSelection_;

    /**
     * Constructs a new widget.
     */
    public MyspaceSelector() {
        super( new BorderLayout() );
        Border gapBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
        Border etchedBorder = BorderFactory.createEtchedBorder();
        setBorder( gapBorder );
        Box topBox = Box.createVerticalBox();
        topBox.setBorder( gapBorder );
        add( topBox, BorderLayout.NORTH );

        /* Set up login/out button. */
        logAction_ = new WindowAction( "Log In" );
        Box logPanel = Box.createHorizontalBox();
        logPanel.add( Box.createHorizontalGlue() );
        logPanel.add( new JButton( logAction_ ) );
        logPanel.add( Box.createHorizontalGlue() );
        topBox.add( logPanel, BorderLayout.NORTH );

        /* Label for MySpace location. */
        Box ivornBox = Box.createHorizontalBox();
        locLabel_ = new JLabel( "Location: " );
        locLabel_.setEnabled( false );
        ivornLabel_ = new JLabel( " " );
        ivornBox.add( locLabel_ );
        ivornBox.add( ivornLabel_ );
        ivornBox.add( Box.createHorizontalGlue() );
        topBox.add( Box.createVerticalStrut( 5 ) );
        topBox.add( ivornBox );

        /* Set up main tree component. */
        emptyTreeModel_ = new DefaultTreeModel( null );
        jtree_ = new JTree( emptyTreeModel_ );
        JScrollPane scroller = new JScrollPane( jtree_ );
        scroller.setPreferredSize( new Dimension( 400, 300 ) );
        scroller.setBorder( etchedBorder );
        add( scroller, BorderLayout.CENTER );

        /* Configure to act on selection events. */
        jtree_.getSelectionModel()
              .setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );
        jtree_.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged( TreeSelectionEvent evt ) {
                Node mn = getSelectedNode();
                if ( mn != null && 
                     ! allowContainerSelection_ && mn.isContainer() ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            jtree_.clearSelection();
                        }
                    } );
                }
                okAction_.setEnabled( mn != null );
            }
        } );

        /* Configure panel for controls, including space for expansion. */
        JPanel bottomPanel = new JPanel( new BorderLayout() );
        bottomPanel.setBorder( gapBorder );
        extraPanel_ = new JPanel( new BorderLayout() );
        bottomPanel.add( extraPanel_, BorderLayout.CENTER );
        add( bottomPanel, BorderLayout.SOUTH );

        /* Set up selection actions. */
        cancelAction_ = new WindowAction( "Cancel" );
        okAction_ = new WindowAction( "OK" );
        okAction_.setEnabled( false );

        /* Add standard controls. */
        JComponent controlPanel = Box.createVerticalBox();
        bottomPanel.add( controlPanel, BorderLayout.SOUTH );
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add( Box.createHorizontalGlue() );
        buttonBox.add( new JButton( cancelAction_ ) );
        buttonBox.add( Box.createHorizontalStrut( 5 ) );
        buttonBox.add( new JButton( okAction_ ) );
        controlPanel.add( buttonBox );
    }

    /**
     * Invoked when the user hits the OK button.  This can only happen
     * when a selection is made. 
     * <p>
     * The default implementation does nothing.
     */
    protected void ok() {
    }

    /**
     * Invoked when the user hits the Cancel button.
     * <p>
     * The default implementation does nothing.
     */
    protected void cancel() {
    }

    /**
     * Returns an initially empty panel near the bottom of the window 
     * which can be used to hold custom components.
     *
     * @return   empty panel
     */
    public JPanel getExtraPanel() {
        return extraPanel_;
    }

    /**
     * Returns the currently selected node, if there is one.
     */
    public Node getSelectedNode() {
        TreePath tp = jtree_.getSelectionPath();
        if ( tp != null ) {
            Object tn = tp.getLastPathComponent();
            if ( tn instanceof MyspaceTreeNode ) {
                return ((MyspaceTreeNode) tn).getMyspaceNode();
            }
        }
        return null;
    }

    /**
     * Indicates whether it is permitted to select a container (directory)
     * or only a file-type node.
     *
     * @return  true iff containers can be selected
     */
    public boolean getAllowContainerSelection() {
        return allowContainerSelection_;
    }

    /**
     * Sets whether it is permitted to select a container (directory)
     * or only a file-type node.
     *
     * @param   allowed  true iff you want containers to be selected
     */
    public void setAllowContainerSelection( boolean allowed ) {
        allowContainerSelection_ = allowed;
    }

    /**
     * Invoked by the Log In button.
     */
    private void logIn() {

        /* Try to get a ready-to-use TreeClient. */
        AGConnector conn = getConnector();
        TreeClient tc = null;
        try {
            tc = conn.getConnection();
            if ( tc != null ) {

                /* Get textual information about the session. */
                String community = conn.getCommunity();
                String user = conn.getUser();
                Container rootContainer;
                String ivorn;
                try {
                    ivorn = UserAGConnector.getIvorn( community, user )
                                           .toString();
                }
                catch ( CommunityException e ) {
                    // it's unlikely that this will happen, since it must have 
                    // used the Ivorn to make the connection, but if it does
                    // just carry on without it.
                    ivorn = "???";
                }

                /* Try to get the root node from the tree. */
                rootContainer = tc.getRoot();
                treeClient_ = tc;

                /* Success.  Configure the GUI accordingly. */
                ivornLabel_.setText( ivorn );
                locLabel_.setEnabled( true );
                logAction_.putValue( Action.NAME, "Log Out (" + user + ")" );
                TreeNode rootTreeNode =
                    new MyspaceTreeNode( null, rootContainer );
                jtree_.setModel( new DefaultTreeModel( rootTreeNode ) );
            }
        }

        /* Failed to get a root node.  Clear up and consider ourselves
         * logged out. */
        catch ( TreeClientException e ) {
            JOptionPane.showMessageDialog( this, e.getMessage(),
                                           "AstroGrid Login Error",
                                           JOptionPane.ERROR_MESSAGE );
            if ( tc != null ) {
                try {
                    tc.logout();
                }
                catch ( TreeClientException e2 ) {
                    // no action
                }
            }
        }
    }

    /**
     * Invoked by the Log Out button.
     */
    private void logOut() {
        if ( treeClient_ != null ) {

            /* Try to log out. */
            try {
                treeClient_.logout();
            }
            catch ( TreeClientException e ) {
                // no action
            }

            /* Succeed or fail, we don't have a usable session.
             * Configure the GUI accordingly. */
            logAction_.putValue( Action.NAME, "Log In" );
            ivornLabel_.setText( " " );
            locLabel_.setEnabled( false );
            jtree_.setModel( emptyTreeModel_ );
            treeClient_ = null;
        }
    }

    /**
     * Determines whether there is a usable connection to MySpace in effect.
     *
     * @return  true  iff we are logged in
     */
    public boolean isConnected() {
        return treeClient_ != null;
    }

    /**
     * Returns the dialogue to use.
     *
     * @return  dialogue
     */
    private AGConnector getConnector() {

        /* Lazily construct a dialogue component. */
        if ( connector_ == null ) {
            connector_ = AGConnectorFactory.getInstance().getConnector( this );
        }
        return connector_;
    }

    /**
     * Helper class implementing actions.
     */
    private class WindowAction extends AbstractAction {

        public WindowAction( String name ) {
            super( name );
        }
 
        public void actionPerformed( ActionEvent evt ) {
            if ( this == logAction_ ) {
                if ( isConnected() ) {
                    logOut();
                }
                else {
                    logIn();
                }
            }
            else if ( this == cancelAction_ ) {
                cancel();
            }
            else if ( this == okAction_ ) {
                ok();
            }
            else {
                assert false;
            }
        }
    }
}
