package uk.ac.starlink.table.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import uk.ac.starlink.table.jdbc.Connector;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.table.jdbc.SwingAuthenticator;
import uk.ac.starlink.table.jdbc.TextModelsAuthenticator;

/**
 * A component for eliciting a JDBC access type URL string from the user.
 *
 * @author   Mark Taylor (Starlink)
 */
public class SQLPanel extends JPanel {

    private LabelledComponentStack stack;
    private JComboBox<String> protoField;
    private JComboBox<String> hostField;
    private JTextField dbField;
    private JTextComponent refField;
    private JTextField tableField;
    private JTextField userField;
    private JPasswordField passField;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.gui" );

    /**
     * Constructs an SQLPanel using a given annotation for the 'ref'
     * part of the URL (the part after the '#' character).
     *
     * @param   refString  the string used for annotating
     * @param   refArea  true to use a multi-line text area for the ref field,
     *                   false for a one-line field
     */
    public SQLPanel( String refString, boolean refArea ) {
        super( new BorderLayout() );
        stack = new LabelledComponentStack();
        add( stack, BorderLayout.NORTH );
        Font inputFont = stack.getInputFont();

        /* Protocol input field. */
        protoField = new JComboBox<String>();
        protoField.addItem( "" );
        protoField.addItem( "mysql" );
        protoField.addItem( "postgresql" );
        protoField.setEditable( true );
        protoField.setFont( inputFont );
        stack.addLine( "Protocol", "jdbc:", protoField );
        
        /* Host input field. */
        hostField = new JComboBox<String>();
        hostField.addItem( "" );
        hostField.addItem( "localhost" );
        hostField.setEditable( true );
        hostField.setFont( inputFont );
        stack.addLine( "Host", "://", hostField );

        /* Database field. */
        dbField = new JTextField( 12 );
        stack.addLine( "Database name", "/", dbField );

        /* Reference field in the one-line case. */ 
        if ( ! refArea ) {
            refField = new JTextField( 32 );
            stack.addLine( refString, "#", refField );
        }

        /* Username input field. */
        userField = new JTextField( 12 );
        userField.setText( System.getProperty( "user.name" ) );
        stack.addLine( "User name", null, userField );

        /* Password input field. */
        passField = new JPasswordField( 12 );
        stack.addLine( "Password", null, passField );

        /* Reference field in the multi-line case. */
        if ( refArea ) {
            JComponent refHolder = new JPanel( new BorderLayout() );
            Box labelBox = Box.createVerticalBox();
            labelBox.add( new JLabel( refString + ": # " ) );
            labelBox.add( Box.createVerticalGlue() );
            refHolder.add( labelBox, BorderLayout.WEST );
            refField = new JTextArea();
            refField.setFont( Font.decode( "Monospaced" ) );
            refHolder.add( Box.createVerticalStrut( 5 ), BorderLayout.NORTH );
            refHolder.add( new JScrollPane( refField ), BorderLayout.CENTER );
            refHolder.setPreferredSize( new Dimension( 400, 100 ) );
            add( refHolder, BorderLayout.CENTER );
        }
    }

    /**
     * Initialises this dialog's fields in accordance with a given 
     * JDBCAuthenticator object.  In general, this will call
     * {@link uk.ac.starlink.table.jdbc.JDBCAuthenticator#authenticate}
     * and fill the user and password fields with the result.
     * However, if <code>auth</code> is a
     * {@link uk.ac.starlink.table.jdbc.TextModelsAuthenticator}, it
     * will actually use its models in the user and password fields.
     *
     * @param   auth   authenticator object to configure from
     */
    public void useAuthenticator( JDBCAuthenticator auth ) {
        if ( auth instanceof TextModelsAuthenticator ) {
            TextModelsAuthenticator tAuth = (TextModelsAuthenticator) auth;
            userField.setDocument( tAuth.getUserDocument() );
            passField.setDocument( tAuth.getPasswordDocument() );
        }
        else {
            if ( auth instanceof SwingAuthenticator ) {
                ((SwingAuthenticator) auth).setParentComponent( this );
            }
            try {
                String[] up = auth.authenticate();
                userField.setText( up[ 0 ] );
                passField.setText( up[ 1 ] );
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING,
                             "Authentication attempt failed", e );
            }
        }
    }

    /**
     * Returns a <code>Connector</code> object which will make connections
     * based on the information entered in this dialog.  Note the
     * connections will use the information current at the time this
     * method is called, rather than reflecting subsequent changes 
     * in this object's state when the connector's <code>getConnection</code>
     * method is invoked.
     *
     * @return   object which gets JDBC connections
     */
    public Connector getConnector() {
        return new Connector() {
            private String baseURL = getBaseURL();
            private String user = userField.getText();
            private String pass = new String( passField.getPassword() );
            public Connection getConnection() throws SQLException {
                return DriverManager.getConnection( baseURL, user, pass );
            }
        };
    }

    /**
     * Returns the basic URL which the user has specified by filling in the
     * boxes.  This does not include the 'ref' part.
     *
     * @return  the basic URL entered by the user as used for getting
     *          an SQL connection
     */
    public String getBaseURL() {
        return new StringBuffer()
            .append( "jdbc:" ) 
            .append( protoField.getSelectedItem() )
            .append( "://" )
            .append( hostField.getSelectedItem() )
            .append( "/" )
            .append( dbField.getText() )
            .toString();
    }

    /**
     * Returns the 'ref' part of the URL, if a ref string was specified
     * in the constructor.  Otherwise null.
     *
     * @return  any ref string entered by the user
     */
    public String getRef() {
        return refField == null ? null : refField.getText();
    }

    /**
     * Returns the full URL specified by the user in this box (including
     * the 'ref' part if there is one).
     *
     * @return   full URL-like string
     */
    public String getFullURL() {
        String ref = getRef();
        return getBaseURL() + ( ( ref != null ) ? ( "#" + ref ) : "" );
    }

    public boolean isAvailable() {
        return isSqlAvailable();
    }

    /**
     * Returns the container for query components.
     *
     * @return   query component stack
     */
    protected LabelledComponentStack getStack() {
        return stack;
    }

    /**
     * Indicates whether JDBC is set up so that it might work.
     *
     * @return  true iff any JDBC drivers are installed
     */
    public static boolean isSqlAvailable() {
        return DriverManager.getDrivers().hasMoreElements();
    }
}
