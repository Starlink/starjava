package uk.ac.starlink.topcat;

import java.awt.Component;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.topcat.plot2.LineBox;

/**
 * Authenticator for password-protected HTTP connections that pops up a
 * dialogue asking the user for credentials.
 *
 * @author   Mark Taylor
 * @since    12 Jun 2013
 */
public class SwingHttpAuthenticator extends Authenticator {

    private Component parent_;

    /**
     * Constructor.
     */
    public SwingHttpAuthenticator() {
        super();
    }

    /**
     * Sets the component to use as parent of the dialogue when it is posted.
     *
     * @param  parent  dialogue parent
     */
    public void setParent( Component parent ) {
        parent_ = parent;
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication() {

        /* Acquire information about who is asking for credentials. */
        String src = getRequestingHost();
        if ( src == null ) {
            src = String.valueOf( getRequestingURL() );
        }
        String prompt = getRequestingPrompt();

        /* Construct the GUI.  We use new field components each time.
         * We could re-use the same JTextField/JPasswordField every time,
         * in which case they would be populated with the same values
         * as last time the user saw them.  However, the authentication
         * framework seems only to ask about new URLs, so it's likely
         * that the user will want a different user/password each time. */
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JComponent box = Box.createVerticalBox();
        box.add( new LineBox( new JLabel( "Authentication required for "
                                        + src ) ) );
        if ( prompt != null ) {
            box.add( new LineBox( new JLabel( prompt ) ) );
        }
        LabelledComponentStack stack = new LabelledComponentStack();
        stack.addLine( "User", userField );
        stack.addLine( "Password", passField );
        box.add( new LineBox( stack ) );

        /* Post this using a JOptionPane.  This is very easy to implement,
         * but the dialogue is modal.  That may not be such a great idea,
         * since it's possible that the user might want to do something
         * else while thinking about the password, in general modal
         * dialogues are best avoided.  Possibly replace it with a
         * normal window or a non-modal dialogue if it proves to be
         * a problem in practice. */
        int result = JOptionPane
                    .showConfirmDialog( parent_, box, "Authentication",
                                        JOptionPane.OK_CANCEL_OPTION,
                                        JOptionPane.QUESTION_MESSAGE );

        /* Provide a cancel option, and return null if it is selected.
         * This is quite important, since the authentication framework
         * seems to keep asking for ever (at least until the HTTP service
         * gives up) if the credentials are rejected.  Without cancel,
         * if the user doesn't know the password he will be trapped
         * forever into submitting bad credentials into a modal dialogue. */
        return result == JOptionPane.CANCEL_OPTION
             ? null
             : new PasswordAuthentication( userField.getText(),
                                           passField.getPassword() );
    }
}
