package uk.ac.starlink.table.jdbc;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Implements JDBCAuthenticator using a GUI.
 */
public class SwingAuthenticator implements JDBCAuthenticator {

    private JTextField userField;
    private JPasswordField passField;
    private JPanel fieldPanel;
    private Component parent;

    private JComponent getFieldPanel() {
        if ( fieldPanel == null ) {
            
            JLabel userLab = new JLabel( "User name: " );
            userField = new JTextField( 16 );
            userField.setText( System.getProperty( "user.name" ) );
            JLabel passLab = new JLabel( "Password: " );
            passField = new JPasswordField( 16 );

            GridBagLayout layer = new GridBagLayout();
            GridBagConstraints clab = new GridBagConstraints();
            clab.anchor = GridBagConstraints.EAST;
            clab.gridx = 0;
            clab.gridy = 0;
            layer.setConstraints( userLab, clab );
            clab.gridy++;
            layer.setConstraints( passLab, clab );
            clab.gridy++;

            GridBagConstraints cfield = new GridBagConstraints();
            cfield.anchor = GridBagConstraints.WEST;
            cfield.gridx = 1;
            cfield.gridy = 0;
            layer.setConstraints( userField, cfield );
            cfield.gridy++;
            layer.setConstraints( passField, cfield );
            cfield.gridy++;

            fieldPanel = new JPanel( layer );
            fieldPanel.add( userLab );
            fieldPanel.add( passLab );
            fieldPanel.add( userField );
            fieldPanel.add( passField );
        }
        return fieldPanel;
    }

    /**
     * Sets the parent component for this authenticator; this may affect
     * the positioning of the dialog box.
     *
     * @param  parent the parent component for the dialog box - 
     *         may be <tt>null</tt>
     */
    public void setParentComponent( Component parent ) {
        this.parent = parent;
    }

    /**
     * Returns the parent component for this authenticator; this may affect
     * the positioning of the dialog box.
     *
     * @return the parent component for the dialog box -
     *         may be <tt>null</tt>
     */
    public Component getParentComponent() {
        return parent;
    }

    public String[] authenticate() throws IOException {
        int opt = JOptionPane
                 .showOptionDialog( parent, getFieldPanel(),
                                    "JDBC Authenticator", 
                                    JOptionPane.OK_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null,
                                    null, userField );
        // should set the focus to the User field, but not sure how
        if ( opt == JOptionPane.CANCEL_OPTION ) {
            throw new IOException( "Authentication refused" );
        }
        else {
            String user = userField.getText();
            String pass = new String( passField.getPassword() );
            return new String[] { user, pass };
        }
    }
 
}
