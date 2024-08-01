package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;

/**
 * Application-style JDialog subclass.
 * This parallels AuxWindow, providing similar boilerplate items
 * but for a JDialog rather than a JFrame.
 * It doesn't include all the capabilities of AuxWindow, since
 * so far they are not needed, and AuxWindow probably attempts to
 * enforce more uniformity than is really helpful.
 * But more things can be added if required.
 *
 * @author   Mark Taylor
 * @since    22 Sep 2017
 */
public class AuxDialog extends JDialog {

    private final JToolBar toolBar_;
    private final JMenu windowMenu_;
    private final Action closeAct_;
    private JMenu helpMenu_;

    /**
     * Constructor.
     *
     * @param  title  dialog title
     * @param  owner  dialog owner window
     */
    @SuppressWarnings("this-escape")
    public AuxDialog( String title, Window owner ) {
        super( owner, title );
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        setContentPane( new JPanel( new BorderLayout() ) );

        /* Set up a basic menubar with a Window menu. */
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar( menuBar );
        windowMenu_ = new JMenu( "Window" );
        windowMenu_.setMnemonic( KeyEvent.VK_W );
        menuBar.add( windowMenu_ );
        Action controlAct =
                new BasicAction( "Control Window", ResourceIcon.CONTROL,
                                 "Ensure Control Window is visible" ) {
            public void actionPerformed( ActionEvent evt ) {
                ControlWindow.getInstance().makeVisible();
            }
        };
        controlAct.putValue( Action.MNEMONIC_KEY, KeyEvent.VK_W );
        closeAct_ = new BasicAction( "Close", ResourceIcon.CLOSE,
                                     "Close this window" ) {
            public void actionPerformed( ActionEvent evt ) { 
                AuxDialog.this.dispose();
            }
        };
        Action exitAct = new BasicAction( "Exit", ResourceIcon.EXIT,
                                          "Exit the application" ) {
            public void actionPerformed( ActionEvent evt ) {
                ControlWindow.getInstance().exit( true );
            }
        };
        windowMenu_.add( controlAct );
        JMenuItem closeItem = windowMenu_.add( closeAct_ );
        closeItem.setMnemonic( KeyEvent.VK_C );
        if ( Driver.isStandalone() ) {
            JMenuItem exitItem = windowMenu_.add( exitAct );
            exitItem.setMnemonic( KeyEvent.VK_X );
        }

        /* Set up a toolbar. */
        toolBar_ = new JToolBar();
        toolBar_.addSeparator();
        toolBar_.setFloatable( false );
        getContentPane().add( toolBar_, BorderLayout.NORTH );
    }

    /** 
     * Adds standard actions to this window, in the menu and toolbar.
     * This method should generally be called by subclasses after they
     * have added any other menus and toolbar buttons specific to their
     * function, since the standard buttons appear as the last ones.
     * <p>
     * An ID can be supplied to indicate the page which should be shown
     * in the help viewer when context-sensitive help is requested.
     * This may be <code>null</code> if no change in the help page should
     * be made (for instance if there is no help specific to this window).
     *
     * @param  helpID  the ID of the help item for this window
     */
    protected void addHelp( String helpID ) {

        /* Add a new help menu. */
        helpMenu_ = new JMenu( "Help" );
        helpMenu_.setMnemonic( KeyEvent.VK_H );
        getJMenuBar().add( helpMenu_ );

        /* Add an action to activate the help browser. */
        Action helpAct = new HelpAction( helpID, this );
        toolBar_.add( helpAct );

        /* Add items to the help menu. */
        helpMenu_.add( new HelpAction( null, this ) );
        if ( helpID != null ) {
            helpMenu_.add( helpAct );
        }
        helpMenu_.add( BrowserHelpAction.createManualAction( this ) );
        helpMenu_.add( BrowserHelpAction.createManual1Action( this ) );
        if ( helpID != null ) {
            helpMenu_.add( BrowserHelpAction.createIdAction( helpID, this ) );
        }
        helpMenu_.addSeparator();

        /* Add an About action. */
        Action aboutAct =
                new AbstractAction( "About TOPCAT",
                                    ResourceIcon.getTopcatLogoSmall() ) {
            public void actionPerformed( ActionEvent evt ) {
                TopcatUtils.showAbout( AuxDialog.this );
            }
        };
        helpMenu_.add( aboutAct );

        /* Add a close button. */
        toolBar_.add( closeAct_ );
        toolBar_.addSeparator();
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
        return toolBar_;
    }

    /**
     * Returns this window's "Window" menu.
     *
     * @return  the window menu
     */
    public JMenu getWindowMenu() {
        return windowMenu_;
    }

    /**
     * Returns this window's "Help" menu.
     *
     * @return  the help menu
     */
    public JMenu getHelpMenu() {
        return helpMenu_;
    }
}
