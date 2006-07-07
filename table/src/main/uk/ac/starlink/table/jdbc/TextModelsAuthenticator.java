package uk.ac.starlink.table.jdbc;

import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * JDBC authenticator which keeps its data in the models of text GUI
 * components ({@link javax.swing.text.Document} objects).
 * The advantage of this is that multiple GUI compoents can share
 * the same data, and changing the password in one will have the effect
 * of changing it in all.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2006
 */
public class TextModelsAuthenticator implements JDBCAuthenticator {

    private final Document userDocument_;
    private final Document passDocument_;

    /**
     * Constructor.
     */
    public TextModelsAuthenticator() {
        userDocument_ = new JTextField().getDocument();
        passDocument_ = new JPasswordField().getDocument();
    }

    /**
     * Returns the text model for the username string.
     *
     * @return   username document
     */
    public Document getUserDocument() {
        return userDocument_;
    }

    /**
     * Returns the text model for the password string.
     *
     * @return  password document
     */
    public Document getPasswordDocument() {
        return passDocument_;
    }

    public String[] authenticate() {
        try {
            return new String[] {
                userDocument_.getText( 0, userDocument_.getLength() ),
                passDocument_.getText( 0, passDocument_.getLength() ),
            };
        }
        catch ( BadLocationException e ) {
            throw new AssertionError( e );
        }
    }
}
