package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTable;

/**
 * Provides a common superclass for windows popped up by the TableViewer
 * application.  This class doesn't do very much, but provides common
 * look and feel behaviour such as window titling and close buttons.
 * <p>
 * Some window-type utility methods are also provided.
 *
 * @author   Mark Taylor (Starlink)
 */
public class AuxWindow extends JFrame {

    private JMenu fileMenu;
    private JToolBar toolBar;
    private JLabel headingLabel;
    private JPanel mainArea;
    private JPanel controlPanel;
    private JMenuBar menuBar;

    private Action helpAct;

    private static final Cursor busyCursor = new Cursor( Cursor.WAIT_CURSOR );
    private static final Logger logger = 
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructs an AuxWindow based on a <tt>StarTable</tt>.
     * 
     * @param  baseTitle  the window basic title (the name of <tt>startab</tt>
     *         will be incorporated into the title if it has one
     * @param  startab   the StarTable which this window describes
     * @param  parent   the parent component of the new window - may be
     *         used for positioning
     */
    public AuxWindow( String baseTitle, StarTable startab, Component parent ) {
        setTitle( makeTitle( baseTitle, startab ) );
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        if ( parent != null ) {
            positionAfter( parent, this );
        }

        /* Set up a basic menubar with a File menu. */
        menuBar = new JMenuBar();
        setJMenuBar( menuBar );
        fileMenu = new JMenu( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );
        Action closeAct = new BasicAction( "Close", ResourceIcon.CLOSE,
                                           "Close this window" ) {
            public void actionPerformed( ActionEvent evt ) {
                dispose();
            }
        };
        Action exitAct = new BasicAction( "Exit", "Exit the application" ) {
            public void actionPerformed( ActionEvent evt ) {
                System.exit( 0 );
            }
        };
        JMenuItem closeItem = fileMenu.add( closeAct );
        closeItem.setIcon( null );
        closeItem.setMnemonic( KeyEvent.VK_C );
        if ( TableViewer.isStandalone() ) {
            JMenuItem exitItem = fileMenu.add( exitAct );
            exitItem.setIcon( null );
            exitItem.setMnemonic( KeyEvent.VK_X );
        }

        /* Set up a toolbar. */
        toolBar = new JToolBar();
        toolBar.add( closeAct );
        toolBar.addSeparator();
        getContentPane().add( toolBar, BorderLayout.NORTH );

        /* Divide the main area into heading, main area, and control panels. */
        JPanel overPanel = new JPanel();
        overPanel.setLayout( new BoxLayout( overPanel, BoxLayout.Y_AXIS ) );
        headingLabel = new JLabel();
        headingLabel.setAlignmentX( 0.0f );
        Box headingBox = new Box( BoxLayout.X_AXIS );
        headingBox.add( headingLabel );
        headingBox.add( Box.createHorizontalGlue() );
        mainArea = new JPanel( new BorderLayout() );
        controlPanel = new JPanel();
        overPanel.add( headingBox );
        overPanel.add( mainArea );
        overPanel.add( controlPanel );
        overPanel.setBorder( BorderFactory
                            .createEmptyBorder( 10, 10, 10, 10 ) );
        getContentPane().add( overPanel, BorderLayout.CENTER );
    }

    /**
     * Constructs an AuxWindow.
     *
     * @param  title window title
     */
    public AuxWindow( String title, Component parent ) {
        this( title, (StarTable) null, parent );
    }

    /**
     * Adds help actions to this window, in the menu and toolbar.
     * This method should generally be called by subclasses after they
     * have added any other menus and toolbar buttons specific to their
     * function, since the help buttons appear as the last ones.
     * <p>
     * An ID can be supplied to indicate the page which should be shown
     * in the help viewer when context-sensitive help is requested.
     * This may be <tt>null</tt> if no change in the help page should
     * be made (for instance if there is no help specific to this window).
     *
     * @param  helpID  the ID of the help item for this window
     */
    protected void addHelp( String helpID ) {

        /* Add a new help menu. */
        JMenu helpMenu = new JMenu( "Help" );
        helpMenu.setMnemonic( KeyEvent.VK_H );
        menuBar.add( helpMenu );

        /* Get action to activate the help browser. */
        Action helpAct = new HelpAction( helpID );

        /* Add it to the tool bar. */
        toolBar.add( helpAct );
        toolBar.addSeparator();

        /* Add one or two items to the help menu. */
        if ( helpID != null ) {
            helpMenu.add( new HelpAction( null ) ).setIcon( null );
        }
        helpMenu.add( helpAct ).setIcon( null );
        helpMenu.addSeparator();

        /* Add an About action. */
        Action aboutAct = new AbstractAction( "About TOPCAT" ) {
            public void actionPerformed( ActionEvent evt ) {
                Object[] message = new Object[] {
                    "TOPCAT",
                    "Tool for OPerations on Catalogues And Tables",
                    "Version 0.3b",
                    "Copyright " + '\u00a9' + 
                    " Central Laboratory of the Research Councils",
                    "Authors: Mark Taylor (Starlink)",
                };
                JOptionPane.showMessageDialog( AuxWindow.this, message,
                                               "About TOPCAT",
                                               JOptionPane.INFORMATION_MESSAGE,
                                               ResourceIcon.TOPCAT_LOGO );
            }
        };
        helpMenu.add( aboutAct );
    }

    /**
     * Makes the window look like it's doing something.  This currently
     * modifies the cursor to be busy/normal.
     *
     * @param  busy  whether the window should look busy
     */
    public void setBusy( boolean busy ) {
        setCursor( busy ? busyCursor : null );
    }

    /**
     * Ensures that this window is posted in a visible fashion.
     */
    public void makeVisible() {
        setState( Frame.NORMAL );
        setVisible( true );
    }

    /**
     * Creates a JProgressBar and places it in the the window.
     * It will replace any other progress bar which has been placed
     * by an earlier call of this method.
     *
     * @return   the progress bar which has been placed
     */
    public JProgressBar placeProgressBar() {
        JProgressBar progBar = new JProgressBar();
        getContentPane().add( progBar, BorderLayout.SOUTH );
        return progBar;
    }

    /**
     * Returns this window's toolbar.  Any client which adds a group of
     * tools to the toolbar should add a separator <em>after</em> the
     * group.
     *
     * @return  the toolbar
     */
    public JToolBar getToolBar() {
        return toolBar;
    }

    /**
     * Returns this window's "File" menu.
     *
     * @return  the file menu
     */
    public JMenu getFileMenu() {
        return fileMenu;
    }

    /**
     * Sets the in-window text which heads up the main display area.
     *
     * @param   text  heading text
     */
    public void setMainHeading( String text ) {
        headingLabel.setText( text );
    }

    /**
     * Returns the container which should be used for the main user 
     * component(s) in this window.  It will have a BorderLayout.
     *
     * @return  main container
     */
    public JPanel getMainArea() {
        return mainArea;
    }

    /**
     * Returns the container which should be used for controls and buttons.
     * This will probably be placed below the mainArea.
     *
     * @return  control container
     */
    public JPanel getControlPanel() {
        return controlPanel;
    }

    public Image getIconImage() {
        return ResourceIcon.TOPCAT.getImage();
    }

    /**
     * It beeps.
     */
    public static void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    /**
     * Returns a string suitable for use as a window title given a
     * base title and a StarTable object.
     *
     * @param  baseTitle  the basic part of the title, describing the window
     * @param  the StarTable, if there is one, or <tt>null</tt>
     */
    public static String makeTitle( String baseTitle, StarTable startab ) {
        String name = ( startab == null ) ? null
                                          : startab.getName();
        return name == null ? baseTitle 
                            : ( baseTitle + ": " + name );
    }

    /**
     * Locates one window 'after' another one - probably a bit lower and
     * to the right.  The second window is repositioned relative to the
     * first one.
     * 
     * @param   first   first window, or <tt>null</tt>
     * @param   second  second window
     */
    public static void positionAfter( Component first, Window second ) {
        Point pos = null; 
        if ( first != null ) {
            pos = first.getLocation();
        }
        if ( pos == null ) {
            pos = new Point( 20, 20 );
        }
        pos.x += 60;
        pos.y += 60;
        second.setLocation( pos );
    }

    /**
     * Helper class providing an action for invoking help.
     */
    public class HelpAction extends AbstractAction {

        private String helpID;
        private HelpWindow helpWin;

        /**
         * Constructs a new help window for a given helpID.
         * If helpID is non-null, the corresponding topic will be displayed
         * in the browser when the action is invoked.  If it's null,
         * the browser will come up as is.
         *
         * @param  helpID  help id string
         */
        public HelpAction( String helpID ) {
            this.helpID = helpID;
            putValue( SMALL_ICON, ResourceIcon.HELP );
            putValue( NAME, helpID == null ? "Help" : "Help for window" );
            putValue( SHORT_DESCRIPTION, 
                      helpID == null 
                          ? "Display help browser" 
                          : "Display help for this window in browser" );
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( helpWin == null ) {
                helpWin = HelpWindow.getInstance( AuxWindow.this );
            }
            else {
                helpWin.makeVisible();
            }
            helpWin.setID( helpID );
        }
            
    }

}
