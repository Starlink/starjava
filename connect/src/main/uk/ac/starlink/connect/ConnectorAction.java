package uk.ac.starlink.connect;

import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
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
import javax.swing.border.Border;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Action which controls logging in to and out of a remote service using
 * a {@link Connector} object.
 * This action can be put into a button whose text will read "Log In"
 * and "Log Out" as appropriate.  It has a property with the
 * key {@link #CONNECTION_PROPERTY} which contains the active 
 * {@link Connection} object, so that PropertyChangeListeners may be
 * configured to watch when a connection is established or broken.
 * A log in attempt will pop up a modal dialogue asking for the
 * various authorization information required to attempt the connection.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
public class ConnectorAction extends AbstractAction {

    private final Connector connector_;
    private final JPanel entryPanel_;
    private final Map<AuthKey,JTextField> fieldMap_;
    private final Action okAction_;
    private final boolean noAuth_;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo.tree" );

    private static final String LOGIN_TEXT = "Log In";
    private static final String LOGOUT_TEXT = "Log Out";

    /** Key for the property which stores a {@link Connection} object. */
    public static final String CONNECTION_PROPERTY = "connection";

    /**
     * Constructor.
     *
     * @param  connector   connector describing the service this action 
     *         can connect to
     */
    public ConnectorAction( Connector connector ) {
        super( LOGIN_TEXT, connector.getIcon() );
        connector_ = connector;

        /* Set up action triggered by the user finishing entry of 
         * authorization information in the dialogue. */
        okAction_ = new AbstractAction( "OK" ) {
            public void actionPerformed( ActionEvent evt ) {
                ok();
            }
        };

        /* Set up the main panel which will be used for the dialogue.
         * This contains fields for each of the authorization keys
         * specified by the connector. */
        GridBagLayout layer = new GridBagLayout();
        JPanel stack = new JPanel( layer );
        AuthKey[] keys = connector.getKeys();
        noAuth_ = keys.length == 0;
        fieldMap_ = new HashMap<AuthKey,JTextField>();
        JTextField firstField = null;
        JTextField firstEmpty = null;
        for ( int i = 0; i < keys.length; i++ ) {
            AuthKey key = keys[ i ];
            GridBagConstraints c = new GridBagConstraints();

            /* Place a label for this key. */
            c.insets = new Insets( 2, 2, 0, 0 );
            c.gridy = i;
            c.gridx = 0;
            c.anchor = GridBagConstraints.WEST;
            JLabel label = new JLabel( key.getName() + ": " );

            /* Place a text entry field for this key. */
            layer.setConstraints( label, c );
            stack.add( label );
            c.gridx = 1;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;
            JTextField field = key.isHidden() 
                             ? (JTextField) new JPasswordField( 20 )
                             : new JTextField( 20 );
            layer.setConstraints( field, c );
            stack.add( field );
            if ( firstField == null ) {
                firstField = field;
            }

            /* Store the field. */
            fieldMap_.put( key, field );

            /* Arrange that hitting return on the field completes entry. */
            field.addActionListener( okAction_ );

            /* Fill in an initial default value if available. */
            Object dfault = key.getDefault();
            if ( dfault instanceof String ) {
                field.setText( (String) dfault );
            }
            else if ( dfault instanceof char[] ) {
                field.setText( new String( (char[]) dfault ) );
            }
            else if ( firstEmpty == null ) {
                firstEmpty = field;
            }

            /* Add description information as a tooltip if available. */
            String desc = key.getDescription();
            if ( desc != null ) {
                label.setToolTipText( desc );
                field.setToolTipText( desc );
            }
        }

        /* Place the stack. */
        entryPanel_ = new JPanel( new BorderLayout() );
        entryPanel_.add( stack, BorderLayout.CENTER );

        /* Arrange for the first empty field to have focus when the 
         * window is initially popped up. */
        if ( firstEmpty != null ) {
            final Component initFocus = firstEmpty;
            firstField.addFocusListener( new FocusAdapter() {
                boolean done_;
                public void focusGained( FocusEvent evt ) {
                    if ( ! done_ ) {
                        done_ = initFocus.requestFocusInWindow();
                    }
                }
            } );
        }
    }

    public void actionPerformed( ActionEvent evt ) {

        /* If there's no active connection, try to log in. */
        if ( getConnection() == null ) {
            Object src = evt.getSource();
            Component parent = src instanceof Component 
                             ? (Component) src
                             : null;
            final JDialog dialog = createDialog( parent );

            if ( ! noAuth_ ) {
                dialog.setVisible( true );
            }
            else {   

                /* This rather tortuous way of doing things is to ensure that
                 * the ok() action is invoked as soon as, but not before,
                 * the dialogue has been posted.  Can't just make the two
                 * calls one after the other, since show() blocks. 
                 * Is there a less weird way of doing this?? */
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() { 
                        dialog.setVisible( true );
                    }
                } );
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() { 
                        if ( noAuth_ ) {
                            ok();
                        }
                    }
                } );
            }
        }

        /* Otherwise, try to log out. */
        else {
            try {
                getConnection().logOut();
            }
            catch ( IOException e ) {
                logger_.warning( "Logout failed: " + e.getMessage() );
            }
            setConnection( null );
        }
    }

    /**
     * Constructs the dialogue which is used to ask the user for
     * authorization information.
     *
     * @param  parent   parent component
     * @return  dialogue
     */
    protected JDialog createDialog( Component parent ) {

        /* Construct a basic dialogue. */
        Frame fparent = parent == null 
                      ? null
                      : (Frame) SwingUtilities
                               .getAncestorOfClass( Frame.class, parent );

        final JDialog dialog =
            new JDialog( fparent, connector_.getName() + " Log In", true );

        /* Prepare an action which will cancel the login attempt. */
        Action cancelAction = new AbstractAction( "Cancel" ) {
            public void actionPerformed( ActionEvent evt ) {
                dialog.dispose();
            }
        };

        /* Box containing OK and Cancel buttons. */
        Border gapBorder = BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
        JComponent controlBox = Box.createHorizontalBox();
        controlBox.add( Box.createHorizontalGlue() );
        controlBox.add( new JButton( cancelAction ) );
        if ( ! noAuth_ ) {
            controlBox.add( Box.createHorizontalStrut( 5 ) );
            controlBox.add( new JButton( okAction_ ) );
        }
        controlBox.setBorder( gapBorder );

        /* Box containing a question mark image. */
        String iconID = noAuth_ ? "OptionPane.informationIcon"
                                : "OptionPane.questionIcon";
        JComponent imageBox = new JLabel( UIManager.getIcon( iconID ) );
        imageBox.setBorder( gapBorder );

        /* Box containing the data entry fields themselves. */
        JPanel main = new JPanel( new BorderLayout() );
        JComponent entryHolder = new JPanel( new BorderLayout() );
        entryHolder.setBorder( gapBorder );
        if ( noAuth_ ) {
            entryHolder.add( new JLabel( "Attempting " + connector_.getName() 
                                                       + " connection ..." ) );
        }
        else {
            entryHolder.add( entryPanel_ );
        }

        /* Put them all together in the dialogue. */
        main.add( entryHolder, BorderLayout.CENTER );
        main.add( controlBox, BorderLayout.SOUTH );
        main.add( imageBox, BorderLayout.WEST );
        dialog.getContentPane().add( main );

        /* Watch for a connection being established, and dispose of the 
         * dialogue when it is.  This is what causes execution to continue
         * if no cancel happens. */
        addPropertyChangeListener( new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                if ( evt.getPropertyName().equals( CONNECTION_PROPERTY ) &&
                     evt.getNewValue() != null ) {
                    ConnectorAction.this.removePropertyChangeListener( this );
                    dialog.dispose();
                }
            }
        } );

        /* Return ready-to-use dialogue. */
        dialog.pack();
        dialog.setLocationRelativeTo( parent );
        return dialog;
    }

    /**
     * Invoked when the user indicates that the authorization fields
     * have been filled in.
     */
    private void ok() {

        /* Prepare a map of authorization key -> value pairs to describe
         * the login attempt. */
        Map<AuthKey,Object> valueMap = new HashMap<AuthKey,Object>();
        for ( AuthKey key : fieldMap_.keySet() ) {
            Object value;
            if ( key.isHidden() ) {
                JPasswordField field = (JPasswordField) fieldMap_.get( key );
                char[] pass = field.getPassword();
                value = pass == null || pass.length == 0 ? null : pass;
            }
            else {
                JTextField field = fieldMap_.get( key );
                String text = field.getText();
                value = text == null || text.length() == 0 ? null : text;
            }
            if ( key.isRequired() && value == null ) {
                String msg = "Must supply value for field " + key.getName();
                JOptionPane.showMessageDialog( entryPanel_, msg, "Login Error",
                                               JOptionPane.ERROR_MESSAGE );
                return;
            }
            valueMap.put( key, value );
        }
        attemptLogin( valueMap );
    }

    /**
     * Makes an asynchronous login attempt with a given set of authorization
     * keys.
     *
     * @param  authorization key-value pairs
     */
    private void attemptLogin( final Map<AuthKey,?> authMap ) {

        /* Asynchronously attempt to make a connection using these values. */
        setEnabled( false );
        new Thread( "Login" ) {
            Connection conn;
            IOException error;
            public void run() {
                try {
                    conn = connector_.logIn( authMap );
                }
                catch ( IOException e ) {
                    error = e;
                }
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( conn != null ) {

                            /* This causes the connection property to be
                             * changed, which results in the disposal of
                             * the dialogue. */
                            setConnection( conn );
                        }
                        else {
                            ErrorDialog.showError( entryPanel_, "Login Error",
                                                   error );
                        }
                        setEnabled( true );
                    }
                } );
            }
        }.start();
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        okAction_.setEnabled( enabled );
        for ( JTextField field : fieldMap_.values() ) {
            field.setEnabled( enabled );
        }
    }

    /**
     * Sets the value of the connection property and performs associated
     * housekeeping.
     *
     * @param  connection  new value for connection
     */
    private void setConnection( Connection connection ) {
        putValue( NAME, connection == null ? LOGIN_TEXT : LOGOUT_TEXT );
        putValue( CONNECTION_PROPERTY, connection );
    }

    /**
     * Returns the connector used by this action.
     *
     * @return  connector
     */
    public Connector getConnector() {
        return connector_;
    }

    /**
     * Returns the currently active connection.  May be null if no
     * connection is active.  If the connection has expired, this may
     * result in the connection property being reset to <code>null</code>.
     * Thus it's very likely that the connection returned from this
     * method will be active, but it can't be guaranteed that it won't
     * have expired between this method returning it and the caller
     * receiving it.
     *
     * @return   connection, hopefully an active one
     */
    public Connection getConnection() {
        Connection conn = (Connection) getValue( CONNECTION_PROPERTY );
        if ( conn == null ) {
            return null;
        }
        else if ( conn.isConnected() ) {
            return conn;
        }
        else {
            putValue( CONNECTION_PROPERTY, null );
            return null;
        }
    }
}
