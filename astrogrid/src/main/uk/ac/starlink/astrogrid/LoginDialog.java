package uk.ac.starlink.astrogrid;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.astrogrid.community.common.exception.CommunityException;
import org.astrogrid.community.common.ivorn.CommunityAccountIvornFactory;
import org.astrogrid.store.Ivorn;
import org.astrogrid.store.tree.TreeClient;
import org.astrogrid.store.tree.TreeClientException;
import org.astrogrid.store.tree.TreeClientFactory;

/**
 * Dialog for logging in to an Astrogrid community.
 * The most useful method is {@link #openTreeClientDialog}, which 
 * can be used on its own to acquire a logged-in 
 * {@link org.astrogrid.store.tree.TreeClient}.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Nov 2004
 */
public class LoginDialog extends JPanel {

    private final JTextField commField_;
    private final JTextField userField_;
    private final JPasswordField passField_;
    private final JOptionPane opane_;

    /**
     * Constructs a new dialog.
     */
    public LoginDialog() {

        /* Create query components. */
        commField_ = new JTextField();
        userField_ = new JTextField();
        passField_ = new JPasswordField();

        /* Arrange query components. */
        Stack stack = new Stack();
        stack.addItem( "Community", commField_ );
        stack.addItem( "User", userField_ );
        stack.addItem( "Password", passField_ );
        Component strut = Box.createHorizontalStrut( 300 );

        /* Provide defaults for query components. */
        try {
            String comm = System.getProperty( "org.astrogrid.community.ident" );
            if ( comm != null ) {
                setCommunity( comm );
            }
            String user = System.getProperty( "user.name" );
            if ( user != null ) {
                setUser( user );
            }
        }
        catch ( SecurityException e ) {
            // no permissions - never mind
        }

        /* The main panel will be a JOptionPane since it has icons and
         * layout set up for free.  We won't be using its various
         * static show methods though. */
        opane_ = new JOptionPane();
        opane_.setMessageType( JOptionPane.QUESTION_MESSAGE );
        opane_.setOptionType( JOptionPane.OK_CANCEL_OPTION );
        opane_.setMessage( new Component[] { stack, strut } );
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
     * Returns the <code>Ivorn</code> defined by the current entries
     * in this dialogue's fields.
     *
     * @return  ivorn 
     */
    public Ivorn getIvorn() throws CommunityException {
        String community = getCommunity();
        String user = getUser();
        if ( user != null && user.trim().length() > 0 &&
             community != null && community.trim().length() > 0 ) {
            return CommunityAccountIvornFactory
                  .createIvorn( community, user );
        }
        else {
            return null;
        }
    }

    /**
     * Attempts to acquire and login to a TreeClient based on the fields
     * currently filled in in this dialogue.
     * 
     * @return  ready-to-use TreeClient (login has been called)
     */
    public TreeClient openTreeClient() 
            throws CommunityException, TreeClientException {
        Ivorn ivorn = getIvorn();
        if ( ivorn != null ) {
            TreeClient tc = new TreeClientFactory().createClient();
            tc.login( ivorn, new String( passField_.getPassword() ) );
            return tc;
        }
        else {
            throw new TreeClientException( "No username specified" );
        }
    }

    /**
     * Pops up a dialogue which enquires login information from the user
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
    public TreeClient openTreeClientDialog( Component parent ) {
        JDialog dialog = opane_.createDialog( parent, "Astrogrid Login" );
        dialog.getContentPane().add( opane_ );
        while ( true ) {
            dialog.show();
            Object status = opane_.getValue();
            if ( status instanceof Integer &&
                 ((Integer) status).intValue() == JOptionPane.OK_OPTION ) {
                try {
                    TreeClient tc = openTreeClient();
                    if ( tc != null ) {
                        dialog.dispose();
                        return tc;
                    }
                    else {
                        showError( dialog, "Login failed" );
                    }
                }
                catch ( TreeClientException e ) {
                    showError( dialog, e.getMessage() );
                }
                catch ( CommunityException e ) {
                    showError( dialog, e.getMessage() );
                }
            }

            /* Cancel button pressed, or similar - give up. */
            else {
                dialog.dispose();
                return null;
            }
        }
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
        }
        void addItem( String text, JComponent comp ) {
            gbc_.gridx = 0;
            gbc_.anchor = GridBagConstraints.EAST;
            gbc_.weightx = 0.0;
            gbc_.fill = GridBagConstraints.NONE;
            JLabel label = new JLabel( text + ": " );
            layer_.setConstraints( label, gbc_ );
            container_.add( label );

            gbc_.gridx = 1;
            gbc_.anchor = GridBagConstraints.WEST;
            gbc_.weightx = 1.0;
            gbc_.fill = GridBagConstraints.HORIZONTAL;
            layer_.setConstraints( comp, gbc_ );
            container_.add( comp );

            gbc_.gridy++;
        }
    }
}
