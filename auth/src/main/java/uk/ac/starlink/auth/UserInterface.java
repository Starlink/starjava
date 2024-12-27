package uk.ac.starlink.auth;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Abstraction via which authentication classes can acquire credentials
 * from the user.
 *
 * @author   Mark Taylor
 * @since    15 Jun 2020
 */
public abstract class UserInterface {

    private Component parent_;

    /** Command-line instance. */
    public static final UserInterface CLI = createCli();

    /** Instance that uses Swing popup dialogues. */
    public static final UserInterface GUI = createGui();

    /** Instance that will not authenticate. */
    public static final UserInterface NO_AUTH = createNone();

    /** Name of username system property for headless UI instance ({@value}). */
    public static final String USERNAME_PROP = "auth.username";

    /** Name of password system property for headless UI instance ({@value}). */
    public static final String PASSWORD_PROP = "auth.password";

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.auth" );

    /**
     * Returns a username/password pair, or null if the user declines
     * to supply one.
     *
     * @param   msgLines  message to output to the user first
     * @return   credentials, or null if the user declines
     */
    public abstract UserPass readUserPassword( String[] msgLines );

    /**
     * Sends a message to the user.
     * In the case of a GUI this will typically provoke a popup window,
     * so it should not be used gratitously.
     *
     * @param  lines  message to pass to the user
     */
    public abstract void message( String[] lines );

    /**
     * Indicates whether there is any point in repeated attempts to
     * acquire credentials from the user.  In an interactive context,
     * this would typically return true, but in a headless context
     * with preset credentials that will not change between invocations,
     * it should return false.
     *
     * @return  true if repeated requests may return different results
     */
    public abstract boolean canRetry();

    /**
     * Sets a GUI component to which this UI is subordinate.
     * For non-GUI implementations, this method is likely to have no effect,
     * but for GUI-based implementations, it can be used to manage
     * the ownership of popup dialogues.
     *
     * @param   parent  new parent component, may be null
     */
    public void setParent( Component parent ) {
        parent_ = parent;
    }

    /**
     * Returns the GUI component to which this UI is subordinate.
     * Likely to be irrelevant for non-GUI implementations.
     *
     * @return  parent component, may be null
     */
    public Component getParent() {
        return parent_;
    }

    /**
     * Creates an instance that communicates with the user via modal
     * Swing popup dialogues.
     *
     * @return  GUI instance
     */
    private static UserInterface createGui() {
        final String winTitle = "Authentication";
        return new UserInterface() {
            public boolean canRetry() {
                return true;
            }
            public void message( String[] lines ) {
                JOptionPane
               .showMessageDialog( getParent(), lines, winTitle,
                                   JOptionPane.INFORMATION_MESSAGE );
            }
            public UserPass readUserPassword( String[] msgLines ) {
                JTextField userField = new JTextField();
                JPasswordField passField = new JPasswordField();
                JComponent authPanel =
                    createAuthPanel( msgLines, userField, passField );
                setGrabFocusInDialog( userField );
                String[] options = { "Authenticate", "Anonymous" };
                int response =
                    JOptionPane
                   .showOptionDialog( getParent(), authPanel, winTitle,
                                      JOptionPane.DEFAULT_OPTION,
                                      JOptionPane.QUESTION_MESSAGE,
                                      null, options, options[ 0 ] );
                if ( response == 0 ) {
                    String user = userField.getText();
                    return user != null && user.trim().length() > 0
                         ? new UserPass( user, passField.getPassword() )
                         : null;
                }
                else {
                    return null;
                }
            }
        };
    }

    /**
     * Returns a headless implementation with a fixed user name and password
     * supplied as strings.
     *
     * @param  username  username
     * @param  password  password
     * @return  headless instance
     */
    public static UserInterface createFixed( String username,
                                             String password ) {
        return createFixed( new UserPass( username, password.toCharArray() ) );
    }

    /**
     * Returns a headless implementation with a fixed username+password object.
     *
     * @param  userpass  credentials object
     * @return  headless instance
     */
    public static UserInterface createFixed( final UserPass userpass ) {
        return new UserInterface() {
            public boolean canRetry() {
                return false;
            }
            public void message( String[] lines ) {
                logger_.info( "Auth message: " + String.join( "; ", lines ) );
            }
            public UserPass readUserPassword( String[] msgLines ) {
                String line = String.join( "; ", msgLines );
                if ( userpass != null ) {
                    logger_.warning( "Auth: " + line );
                    logger_.warning( "Auth: supplied credentials for user "
                                   + userpass.getUsername() );
                }
                else {
                    logger_.info( "Auth: " + line );
                    logger_.info( "Auth: no credentials" );
                }
                return userpass;
            }
        };
    }

    /**
     * Returns a headless UI that takes username and password from
     * the system properties {@link #USERNAME_PROP} and {@link #PASSWORD_PROP}
     * respectively, if both properties are set.
     *
     * <p>If the first character of either property value is "@",
     * the remainder is interpreted as a filename containing the value.
     *
     * <p>Use the result of this method with care,
     * since it risks leaking credentials to
     * sites for which they are not intended.
     *
     * <p>This is a shortcut for
     * {@link #getPropertiesUi(java.lang.String,java.lang.String)
     *         getPropertiesUi}({@link #USERNAME_PROP},
                                {@link #PASSWORD_PROP}).
     *
     * @return  new headless UserInterface instance,
     *          or null if not both properties are set
     */
    public static UserInterface getPropertiesUi() {
        return getPropertiesUi( USERNAME_PROP, PASSWORD_PROP );
    }

    /**
     * Returns a headless UI that takes username and password from
     * two named System Properties.
     * If the system properties, read at the time of this invocation,
     * have not been defined, then null is returned.
     *
     * <p>If the first character of either property value is "@",
     * the remainder is interpreted as a filename containing the value.
     *
     * <p>Use the result of this method with care,
     * since it risks leaking credentials to
     * sites for which they are not intended.
     *
     * @param  userProp  system property name for acquiring username
     * @param  passProp  system property name for acquiring password
     * @return  new headless UserInterface instance,
     *          or null if not both properties are set
     */
    public static UserInterface getPropertiesUi( String userProp,
                                                 String passProp ) {
        String user = readPropertyText( userProp );
        String pass = readPropertyText( passProp );
        return user != null && pass != null
             ? createFixed( user, pass )
             : null;
    }

    /**
     * Returns the text associated with a credential property.
     * The result is either the value of the system property itself,
     * or the content of a file it references, if property value is
     * of the form "<code>@&lt;filename&gt;</code>".
     *
     * @return   propName  name of system property, may be null
     */
    private static String readPropertyText( String propName ) {
        String propval = System.getProperty( propName );
        if ( propval == null ) {
            return null;
        }
        else if ( propval.length() > 0 && propval.charAt( 0 ) == '@' ) {
            File f = new File( propval.substring( 1 ) );
            if ( f.exists() ) {
                BufferedReader rdr = null;
                try {
                    rdr = new BufferedReader(
                              new InputStreamReader( new FileInputStream( f ),
                                                     AuthUtil.UTF8 ) );
                    String line = rdr.readLine();
                    logger_.config( "Read credential from file " + f );
                    return line;
                }
                catch ( IOException e ) {
                    logger_.log( Level.WARNING,
                                 "Error reading credential file " + f, e );
                    return null;
                }
                finally {
                    if ( rdr != null ) {
                        try {
                            rdr.close();
                        }
                        catch ( IOException e ) {
                        }
                    }
                }
            }
            else {
                logger_.warning( "No such credential file " + f );
                return null;
            }
        }
        else {
            return propval;
        }
    }

    /**
     * Returns an instance that communicates via the console.
     *
     * @return  command-line instance
     */
    private static UserInterface createCli() {
        return new UserInterface() {
            public boolean canRetry() {
                return true;
            }
            public void message( String[] lines ) {
                for ( String line : lines ) {
                    System.console().writer().println( line );
                }
            }
            public UserPass readUserPassword( String[] msgLines ) {
                Console console = System.console();
                PrintWriter writer = console.writer();
                for ( String line : msgLines ) {
                    console.writer().println( line );
                }
                String user = console.readLine( "Username: " );
                char[] pass = console.readPassword( "Password: " );
                return ( ( user == null || user.trim().length() == 0 ) &&
                         ( pass == null || pass.length == 0 ) )
                     ? null
                     : new UserPass( user, pass );
            }
        };
    }

    /**
     * Returns an instance that will not supply any credentials.
     *
     * @return  unhelpful implementation
     */
    private static UserInterface createNone() {
        return new UserInterface() {
            public boolean canRetry() {
                return false;
            }
            public void message( String[] lines ) {
                logger_.info( "Auth: " + String.join( "; ", lines ) );
            }
            public UserPass readUserPassword( String[] msgLines ) {
                logger_.warning( "Auth: " + String.join( "; ", msgLines ) );
                logger_.warning( "Auth: user interaction declined" );
                return null;
            }
        };
    }

    /**
     * Places the components for acquiring username and password.
     *
     * @param  msgLines   message to display to user
     * @param  userField  field for username
     * @param  passField  field for password
     */
    public static JComponent createAuthPanel( String[] msgLines,
                                              JTextField userField,
                                              JPasswordField passField ) {
        GridBagLayout layer = new GridBagLayout();
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.weightx = 0.0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        JPanel panel = new JPanel( layer );
        for ( String line : msgLines ) {
            JLabel txtLabel = new JLabel( line );
            gc.gridwidth = GridBagConstraints.REMAINDER;
            gc.insets = new Insets( 0, 0, 3, 0 );
            layer.setConstraints( txtLabel, gc );
            panel.add( txtLabel );
            gc.gridy++;
        }
        gc.gridwidth = 1;
        
        JLabel userLabel = new JLabel( "User: " );
        gc.insets = new Insets( 5, 0, 5, 0 );
        gc.gridx = 0;
        layer.setConstraints( userLabel, gc );
        panel.add( userLabel );
        gc.gridx = 1;
        gc.weightx = 1.0;
        layer.setConstraints( userField, gc );
        panel.add( userField );
        gc.weightx = 0;
        gc.gridy++;

        JLabel passLabel = new JLabel( "Password: " );
        gc.insets = new Insets( 0, 0, 0, 0 );
        gc.gridx = 0;
        layer.setConstraints( passLabel, gc );
        panel.add( passLabel );
        gc.gridx = 1;
        gc.weightx = 1.0;
        layer.setConstraints( passField, gc );
        panel.add( passField );
        gc.weightx = 0;
        gc.gridy++;

        return panel;
    }

    /**
     * This causes the given component to be given focus when it appears
     * in a JOptionPane.  That means the user doesn't need to click on
     * a field when the window pops up, the cursor is already there.
     * You'd think this would be the default, but it is not.
     * Magic code copied from
     * https://bugs.java.com/bugdatabase/view_bug?bug_id=5018574
     *
     * @param  component  component that should grab focus
     */
    private static void setGrabFocusInDialog( JComponent component ) {
        component.addHierarchyListener( new HierarchyListener() {
            public void hierarchyChanged( HierarchyEvent evt ) {
                final Component c = evt.getComponent();
                if ( c.isShowing() &&
                     ( evt.getChangeFlags() &
                       HierarchyEvent.SHOWING_CHANGED ) != 0 ) {
                    Window toplevel = SwingUtilities.getWindowAncestor( c );
                    toplevel.addWindowFocusListener( new WindowAdapter() {
                        public void windowGainedFocus( WindowEvent evt2 ) {
                            c.requestFocus();
                        }
                    } );
                }
            }
        } );
    }
}
