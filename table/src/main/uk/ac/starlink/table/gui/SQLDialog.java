package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.awt.Font;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import uk.ac.starlink.table.jdbc.Connector;

/**
 * A popup dialog for eliciting a JDBC access type URL string from the user.
 * This class itself could be used on its own to get a JDBC connection, 
 * but to get the URL-like string for reading or writing a table see its
 * subclasses {@link SQLReadDialog} and {@link SQLWriteDialog}, which
 * know about the 'ref' part of the URLs used by 
 * {@link uk.ac.starlink.table.jdbc.JDBCFormatter} and
 * {@link uk.ac.starlink.table.jdbc.JDBCHandler}.
 *
 * @author   Mark Taylor (Starlink)
 */
public class SQLDialog extends JOptionPane {

    private JComboBox protoField;
    private JComboBox hostField;
    private JTextField dbField;
    private JTextField refField;
    private JTextField tableField;
    private JTextField userField;
    private JPasswordField passField;

    /**
     * Constructs an SQLDialog using a given annotation for the 'ref'
     * part of the URL (the part after the '#' character).
     *
     * @param   refString  the string used for annotating
     */
    public SQLDialog( String refString ) {

        /* JOptionPane configuration. */
        InputFieldStack stack = new InputFieldStack();
        Font inputFont = stack.getInputFont();
        setMessage( stack );
        setOptionType( OK_CANCEL_OPTION );

        /* Protocol input field. */
        protoField = new JComboBox();
        protoField.addItem( "" );
        protoField.addItem( "mysql" );
        protoField.setEditable( true );
        protoField.setFont( inputFont );
        stack.addLine( "Protocol", "jdbc:", protoField );
        
        /* Host input field. */
        hostField = new JComboBox();
        hostField.addItem( "" );
        hostField.addItem( "localhost" );
        hostField.setEditable( true );
        hostField.setFont( inputFont );
        stack.addLine( "Host", "://", hostField );

        /* Database field. */
        dbField = new JTextField( 12 );
        stack.addLine( "Dababase name", "/", dbField );

        /* Reference field. */ 
        if ( refString != null ) {
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
    }

    /**
     * Returns a <tt>Connector</tt> object which will make connections
     * based on the information entered in this dialog.  Note the
     * connections will use the information current at the time this
     * method is called, rather than reflecting subsequent changes 
     * in this object's state when the connector's <tt>getConnection</tt>
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

}
