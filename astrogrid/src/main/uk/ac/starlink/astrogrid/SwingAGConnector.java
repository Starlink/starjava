package uk.ac.starlink.astrogrid;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.astrogrid.store.tree.TreeClient;
import org.astrogrid.community.common.exception.CommunityException;
import org.astrogrid.store.tree.TreeClientException;

/**
 * AGConnector implementation which works using a Swing dialogue box.
 *
 * @author   Mark Taylor (Starlink)
 * @since    6 Dec 2004
 */
public class SwingAGConnector extends UserAGConnector {

    private final JComponent pane_;
    private Component parent_;
    private final JTextField commField_;
    private final JTextField userField_;
    private final JPasswordField passField_;
    private final JLabel statusLabel_;
    private TreeClient treeClient_;
    private Action cancelAction_;
    private Action okAction_;
    private JDialog dialog_;

    /**
     * Constructs a new connector.
     *
     * @param  parent  parent component
     */
    public SwingAGConnector( Component parent ) {
        parent_ = parent;
        pane_ = new JPanel( new BorderLayout() );

        /* Create query components. */
        commField_ = new JTextField();
        userField_ = new JTextField();
        passField_ = new JPasswordField();
        statusLabel_ = new JLabel( " " );

        /* Arrange query components. */
        Stack stack = new Stack();
        stack.addItem( "Community", commField_ );
        stack.addItem( "User", userField_ );
        stack.addItem( "Password", passField_ );
        stack.addItem( null, statusLabel_ );
        Box qBox = Box.createVerticalBox();
        Component strut = Box.createHorizontalStrut( 300 );
        qBox.add( stack );
        qBox.add( strut );
        pane_.add( qBox, BorderLayout.CENTER );

        /* Box with icon. */
        JComponent iconBox = Box.createVerticalBox();
        Icon icon = UIManager.getIcon( "OptionPane.questionIcon" );
        iconBox.add( new JLabel( icon ) );
        iconBox.add( Box.createVerticalGlue() );
        iconBox.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        pane_.add( iconBox, BorderLayout.WEST );

        /* Buttons. */
        okAction_ = new AbstractAction( "OK" ) {
            public void actionPerformed( ActionEvent evt ) {
                ok();
            }
        };
        cancelAction_ = new AbstractAction( "Cancel" ) {
            public void actionPerformed( ActionEvent evt ) {
                cancel();
            }
        };
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add( Box.createHorizontalGlue() );
        buttonBox.add( new JButton( okAction_ ) );
        buttonBox.add( Box.createHorizontalStrut( 5 ) );
        buttonBox.add( new JButton( cancelAction_ ) );
        buttonBox.add( Box.createHorizontalGlue() );
        buttonBox.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        pane_.add( buttonBox, BorderLayout.SOUTH );

        /* Configure return on text fields equivalent to OK. */
        commField_.addActionListener( okAction_ );
        userField_.addActionListener( okAction_ );
        passField_.addActionListener( okAction_ );
    }

    /**
     * Sets the parent component.
     *
     * @param   parent   parent component
     */
    public void setParent( Component parent ) {
        parent_ = parent;
    }

    /**
     * Returns the parent component.
     *
     * @return   parent 
     */
    public Component getParent() {
        return parent_;
    }

    /**
     * Sets the content of the community text entry field.
     *
     * @param  comm  community identifier
     */
    public void setCommunity( String comm ) {
        commField_.setText( comm );
    }

    /**
     * Returns the content of the community text entry field.
     *
     * @return  community identifier
     */
    public String getCommunity() {
        return commField_.getText();
    }

    /**
     * Sets the content of the user text entry field.
     *
     * @param   user  user identifier
     */
    public void setUser( String user ) {
        userField_.setText( user );
    }

    /**
     * Returns the content of the user text entry field.
     *
     * @return  user identifier
     */
    public String getUser() {
        return userField_.getText();
    }

    /**
     * Attempts to open a new connection to the currently selected AstroGrid 
     * community and return the TreeClient for accessing it.
     * If the user indicates that they do not want to (e.g. hits the Cancel
     * button) null will be returned.
     *
     * @return   open TreeClient, or null
     */
    public TreeClient openNewConnection() {
        return showLoginDialog( getParent() );
    }

    /**
     * Pops up a modal dialogue which enquires login information from the user
     * and tries to use it to log in to an Astrogrid community.
     * If the attempt fails, the user will get another chance to fill
     * in the fields and try again.  This will go on until one of two
     * things happens:
     * <ol>
     * <li>the user hits <strong>Cancel</strong> (null return)
     * <li>a successful login occurs (logged-in TreeClient return)
     * </ol>
     *
     * @param  parent  parent window
     * @return  ready-to-use TreeClient or <tt>null</tt> if the user got bored
     */
    public TreeClient showLoginDialog( Component parent ) {
        if ( dialog_ != null ) {
            throw new IllegalStateException( "Already showing" );
        }
        dialog_ = createDialog( parent );
        dialog_.show();
        return treeClient_;
    }

    /**
     * Invoked when the user hits the OK button.
     * This initiates an asynchronous login attempt, and if successful
     * disposes the current dialog and sets this connector's TreeClient
     * object.
     */
    private void ok() {
        final JDialog dialog = dialog_;
        if ( dialog == null ) {
            return;
        }
        final String community = getCommunity();
        final String user = getUser();
        final char[] password = passField_.getPassword();
        if ( community == null || community.trim().length() == 0 ) {
            showError( dialog, "No community ID supplied" );
            return;
        }
        if ( user == null || user.trim().length() == 0 ) {
            showError( dialog, "No user ID supplied" );
            return;
        }
        setReady( false );
        new Thread( "AstroGrid Connector" ) {
            TreeClient tc_;
            Throwable error_;
            public void run() {
                try {
                    tc_ = openConnection( community, user, password );
                }
                catch ( Throwable th ) {
                    error_ = th;
                }
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( dialog_ == dialog ) {
                            if ( tc_ != null ) {
                                treeClient_ = tc_;
                                dialog_.dispose();
                                dialog_ = null;
                            }
                            else {
                                showError( dialog_, error_.getMessage() );
                            }
                            setReady( true );
                        }
                    }
                } );
            }
        }.start();
    }

    /**
     * Invoked when the user hits the Cancel button.
     * This cancels any current attempt to log in and disposes the dialogue.
     */
    private void cancel() {
        if ( dialog_ != null ) {
            dialog_.dispose();
            dialog_ = null;
            setReady( true );
        }
    }

    /**
     * Sets the ready status.
     *
     * @param  ready  true iff this component is ready to accept user
     *         interaction (apart from Cancel, which is always available)
     */
    private void setReady( boolean ready ) {
        commField_.setEnabled( ready );
        userField_.setEnabled( ready );
        passField_.setEnabled( ready );
        okAction_.setEnabled( ready );
        statusLabel_.setText( ready ? " " : "Connecting..." );
    }

    /**
     * Informs the user of an error.
     *
     * @param  parent  parent component
     * @param  message  message text
     */
    private void showError( Component parent, String message ) {
        JOptionPane.showMessageDialog( parent, message, "Login Error",
                                       JOptionPane.ERROR_MESSAGE );
    }

    /**
     * Constructs a dialogue window based on the graphical component 
     * owned by this connector.
     *
     * @param  parent  parent component
     */
    private JDialog createDialog( Component parent ) {

        /* Locate parent's frame. */
        Frame frame = null;
        if ( parent != null ) {
            frame = parent instanceof Frame
                  ? (Frame) parent
                  : (Frame) SwingUtilities.getAncestorOfClass( Frame.class,
                                                               parent );
        }

        /* Create a new dialogue. */
        dialog_ = new JDialog( frame, "AstroGrid Login", true );
        dialog_.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
        dialog_.getContentPane().setLayout( new BorderLayout() );
        dialog_.getContentPane().add( pane_, BorderLayout.CENTER );

        /* Position. */
        dialog_.pack();
        dialog_.setLocationRelativeTo( parent );
        setReady( true );
        return dialog_;
    }

    /**
     * Helper class for positioning pairs of components in a vertical stack.
     * Insulates the rest of the class from having to deal with the
     * apallingly unfriendly GridBagLayout.
     */
    private static class Stack extends JPanel {
        private final GridBagLayout layer_ = new GridBagLayout();
        private final GridBagConstraints gbc_ = new GridBagConstraints();
        private final JComponent container_ = new JPanel( layer_ );
        Stack() {
            super( new BorderLayout() );
            add( container_ );
            gbc_.gridy = 0;
            gbc_.insets = new Insets( 5, 5, 5, 5 );
        }
        void addItem( String text, JComponent comp ) {
            gbc_.gridx = 0;
            gbc_.anchor = GridBagConstraints.EAST;
            gbc_.weightx = 0.0;
            gbc_.fill = GridBagConstraints.NONE;
            if ( text != null ) {
                JLabel label = new JLabel( text + ": " );
                layer_.setConstraints( label, gbc_ );
                container_.add( label );
            }

            gbc_.gridx = 1;
            gbc_.anchor = GridBagConstraints.WEST;
            gbc_.weightx = 1.0;
            gbc_.fill = GridBagConstraints.HORIZONTAL;
            layer_.setConstraints( comp, gbc_ );
            container_.add( comp );

            gbc_.insets = new Insets( 0, 5, 5, 5 );
            gbc_.gridy++;
        }
    }
}
