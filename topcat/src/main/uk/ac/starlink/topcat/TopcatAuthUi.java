package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import uk.ac.starlink.auth.UserInterface;
import uk.ac.starlink.auth.UserPass;

/**
 * Auth UserInterface implementation for use with Topcat.
 * This uses a non-modal dialogue so that the reset of the GUI is
 * still available when the login dialogue is posted.
 *
 * @author   Mark Taylor
 * @since    19 Oct 2023
 */
public class TopcatAuthUi extends UserInterface {

    public boolean canRetry() {
        return true;
    }

    public void message( String[] lines ) {
        JOptionPane.showMessageDialog( getParent(), lines, "Authentication",
                                       JOptionPane.INFORMATION_MESSAGE );
    }

    public UserPass readUserPassword( String[] msgLines ) {
        Component parent = getParent();
        Window window = parent == null
                      ? null
                      : SwingUtilities.getWindowAncestor( parent );

        /* This method should get called from a non-EDT thread.
         * Post the window asynchronously, then block until it has
         * been disposed. */
        AuthDialog dialog = new AuthDialog( window, msgLines );
        dialog.addWindowListener( new WindowListener() {
            public void windowClosed( WindowEvent evt ) {
                done();
            }
            public void windowClosing( WindowEvent evt ) {
                done();
            }
            public void windowIconified( WindowEvent evt ) {
                dialog.dispose();
            }
            public void windowActivated( WindowEvent evt ) {
            }
            public void windowDeactivated( WindowEvent evt ) {
            }
            public void windowDeiconified( WindowEvent evt ) {
            }
            public void windowOpened( WindowEvent evt ) {
                dialog.initFocus();
            }
            private void done() {
                synchronized( dialog ) {
                    dialog.notifyAll();
                }
            }
        } );
        SwingUtilities.invokeLater( () -> {
            dialog.setVisible( true );
        } );
        try {
            synchronized ( dialog ) {
                dialog.wait();
            }
        }
        catch ( InterruptedException e ) {
            return null;
        }

        /* Once the window has been disposed, the auth information is
         * available. */
        return dialog.userPass_;
    }

    /**
     * Dialogue window.
     */
    private static class AuthDialog extends AuxDialog {

        final JTextField userField_;
        final Action authAct_;
        UserPass userPass_;

        /**
         * Constructor.
         *
         * @param  owner  window to which the dialogue will be subordinate
         * @param  msgLines  text to present to user
         */
        AuthDialog( Window owner, String[] msgLines ) {
            super( "Authenticate", owner );
            int nchar = 24;
            userField_ = new JTextField( nchar );
            JPasswordField passField = new JPasswordField( nchar );
            JComponent authPanel =
                createAuthPanel( msgLines, userField_, passField );
            authAct_ = new AbstractAction( "Authenticate" ) {
                public void actionPerformed( ActionEvent evt ) {
                    String user = userField_.getText();
                    userPass_ = user != null && user.trim().length() > 0
                              ? new UserPass( user, passField.getPassword() )
                              : null;
                    dispose();
                }
            };
            Action anonAct = new AbstractAction( "Anonymous" ) {
                public void actionPerformed( ActionEvent evt ) {
                    userPass_ = null;
                    dispose();
                }
            };
            userField_.addCaretListener( new CaretListener() {
                public void caretUpdate( CaretEvent evt ) {
                    updateActions();
                }
            } );
            updateActions();
            JComponent controlLine = Box.createHorizontalBox();
            controlLine.add( Box.createHorizontalGlue() );
            JButton authButt = new JButton( authAct_ );
            getRootPane().setDefaultButton( authButt );
            controlLine.add( authButt );
            controlLine.add( Box.createHorizontalStrut( 5 ) );
            controlLine.add( new JButton( anonAct ) );
            controlLine.add( Box.createHorizontalGlue() );
            authPanel.setBorder( BorderFactory
                               .createEmptyBorder( 5, 5, 5, 5 ) );
            controlLine.setBorder( BorderFactory
                                  .createEmptyBorder( 5, 5, 5, 5 ) );
            JComponent main = new JPanel( new BorderLayout() ); 
            main.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
            main.add( authPanel, BorderLayout.WEST );
            main.add( controlLine, BorderLayout.SOUTH );
            getContentPane().add( main );
            addHelp( "AuthManager" );
            pack();
        }

        /**
         * Configures the window so that the user field has focus.
         */
        void initFocus() {
            userField_.requestFocus();
        }

        /**
         * Updates the status of the actions in this window based on
         * the current state of its components.
         * Should be called if the text fields have been changed.
         */
        private void updateActions() {
            String user = userField_.getText();
            authAct_.setEnabled( user != null && user.trim().length() > 0 );
        }
    }
}
