package uk.ac.starlink.topcat;

import java.awt.Component;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import uk.ac.starlink.table.gui.LabelledComponentStack;

/**
 * Authenticator for password-protected HTTP connections that pops up a
 * dialogue asking the user for credentials.
 *
 * @author   Mark Taylor
 * @since    12 Jun 2013
 */
public class SwingHttpAuthenticator extends Authenticator {

    private final Component parent_;
    private final JTextField userField_;
    private final JPasswordField passField_;

    /**
     * Constructor.
     *
     * @param  parent  dialogue parent
     */
    public SwingHttpAuthenticator( Component parent ) {
        super();
        parent_ = parent;
        userField_ = new JTextField();
        passField_ = new JPasswordField();
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication() {

        /* There is an issue here with multithreading.
         * It can happen that multiple calls to this method happen at almost
         * the same time.  Typically they are to the same protected service,
         * so require the same credentials.  At present the effect is
         * that the dialogue pops up several times and the user has to
         * click OK on it each time.  This is a bit annoying but not too bad.
         * It's probably possible to improve on this behaviour, but it
         * partly involves second guessing the implementation of whatever's
         * calling this method.  Leave it unless it looks like a serious
         * problem or annoyance. */

        /* Acquire information about who is asking for credentials. */
        String src = getRequestingHost();
        if ( src == null ) {
            src = String.valueOf( getRequestingURL() );
        }
        String prompt = getRequestingPrompt();

        /* Construct the GUI.  Reuse the same text components each time,
         * which means second time around they will be populated with
         * the same values they had first time.  The authentication framework
         * usually seems to ask only about new URLs, so that could be an
         * annoyance, but in the case of multi-threaded accesses to the
         * same service, it's useful not to have to type these in more
         * than once. */
        JComponent box = Box.createVerticalBox();
        box.add( new LineBox( new JLabel( "Authentication required for "
                                        + src ) ) );
        if ( prompt != null ) {
            box.add( new LineBox( new JLabel( prompt ) ) );
        }
        LabelledComponentStack stack = new LabelledComponentStack();
        stack.addLine( "User", userField_ );
        stack.addLine( "Password", passField_ );
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
             : new PasswordAuthentication( userField_.getText(),
                                           passField_.getPassword() );
    }
}
