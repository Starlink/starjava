package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import uk.ac.starlink.util.gui.ErrorDialog;


/**
 * General browser for a remote file store of some description which 
 * can be represented as a TreeModel.
 * As well as including a JTree suitably populated, this component 
 * gives you a button to trigger logging in and out; concrete subclasses
 * must implement the abstract {@link #logIn} and {@link #logOut} methods.
 *
 * @author   Mark Taylor (Starlink)
 * @since    31 Jan 2005
 */
public abstract class RemoteTreeBrowser extends JPanel {

    private final Action logAction_;
    private final JLabel locheadLabel_;
    private final JLabel locLabel_;
    private final TreeModel emptyTreeModel_;
    private final JTree jtree_;
    private final JComponent extraPanel_;
    private boolean isConnected_;
    private boolean allowContainerSelection_;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public RemoteTreeBrowser() {
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

        /* Label for remote space location. */
        Box locBox = Box.createHorizontalBox();
        locheadLabel_ = new JLabel( "Location: " );
        locheadLabel_.setEnabled( false );
        locLabel_ = new JLabel( " " );
        locBox.add( locheadLabel_ );
        locBox.add( locLabel_ );
        locBox.add( Box.createHorizontalGlue() );
        topBox.add( Box.createVerticalStrut( 5 ) );
        topBox.add( locBox );

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

        /* Configure panel for controls, including space for expansion. */
        JPanel bottomPanel = new JPanel( new BorderLayout() );
        bottomPanel.setBorder( gapBorder );
        extraPanel_ = new JPanel( new BorderLayout() );
        bottomPanel.add( extraPanel_, BorderLayout.CENTER );
        add( bottomPanel, BorderLayout.SOUTH );
    }

    /**
     * Return a TreeModel representing the data to be displayed.
     *
     * @return   tree model representing remote data
     */
    protected abstract TreeModel logIn() throws IOException;

    /**
     * Called when the tree model obtained by {@link #logIn} is about to
     * be discarded.
     */
    protected abstract void logOut( TreeModel model );

    /**
     * Returns an initially empty panel near the bottom of the window
     * which can be used to hold custom components.
     *
     * @return   empty panel
     */
    public JComponent getExtraPanel() {
        return extraPanel_;
    }

    /**
     * Returns the currently selected node, if there is one.
     *
     * @return  selected tree node
     */
    public Object getSelectedNode() {
        TreePath tp = jtree_.getSelectionPath();
        if ( tp != null ) {
            Object tn = tp.getLastPathComponent();
            return tn;
        }
        return null;
    }

    /**
     * Returns the JTree component used for display.
     *
     * @return  jtree
     */
    public JTree getJTree() {
        return jtree_;
    }

    /**
     * Determines whether there is a usable connection to the remote resource
     * in effect.
     * 
     * @return  true  iff we are logged in
     */
    public boolean isConnected() {
        return isConnected_;
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
     * Helper class implementing actions.
     */
    private class WindowAction extends AbstractAction {

        public WindowAction( String name ) {
            super( name );
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( this == logAction_ ) {

                /* If we're currently logged out, log in now. */
                if ( ! isConnected_ ) {
                    TreeModel tm = null;
                    try {
                        tm = logIn();
                    }
                    catch ( IOException e ) {
                        ErrorDialog.showError( RemoteTreeBrowser.this,
                                               "Login Error", e );
                    }
                    if ( tm != null ) {
                        jtree_.setModel( tm );
                        locheadLabel_.setEnabled( true );
                        locLabel_.setText( tm.getRoot().toString() );
                        logAction_.putValue( Action.NAME, "Log Out" );
                        isConnected_ = true;
                    }
                }

                /* If we're currently logged in, log out now. */
                else {
                    isConnected_ = false;
                    logAction_.putValue( Action.NAME, "Log In" );
                    locLabel_.setText( " " );
                    locheadLabel_.setEnabled( false );
                    TreeModel oldModel = jtree_.getModel();
                    logOut( oldModel );
                    jtree_.setModel( emptyTreeModel_ );
                }
            }
            else {
                assert false;
            }
        }
    }

}
