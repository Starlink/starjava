package uk.ac.starlink.table.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
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
class AuxWindow extends JFrame {

    private JMenu fileMenu;
    private JToolBar toolBar;
    private JLabel headingLabel;
    private JPanel mainArea;
    private JPanel controlPanel;

    private static final Cursor busyCursor = new Cursor( Cursor.WAIT_CURSOR );

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
            setLocationRelativeTo( parent );
        }

        /* Set up a basic menubar with a File menu. */
        JMenuBar mb = new JMenuBar();
        setJMenuBar( mb );
        fileMenu = new JMenu( "File" );
        mb.add( fileMenu );
        Action closeAct = new BasicAction( "Close", "Close this window" ) {
            public void actionPerformed( ActionEvent evt ) {
                dispose();
            }
        };
        Action exitAct = new BasicAction( "Exit", "Exit the application" ) {
            public void actionPerformed( ActionEvent evt ) {
                System.exit( 0 );
            }
        };
        fileMenu.add( closeAct );
        if ( TableViewer.isStandalone() ) {
            fileMenu.add( exitAct );
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
     * Makes the window look like it's doing something.  This currently
     * modifies the cursor to be busy/normal.
     *
     * @param  busy  whether the window should look busy
     */
    public void setBusy( boolean busy ) {
        setCursor( busy ? busyCursor : null );
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
     * Returns this window's toolbar.
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
    public static void positionAfter( Window first, Window second ) {
        Point pos = null; 
        if ( first != null ) {
            pos = first.getLocation();
        }
        if ( pos == null ) {
            pos = new Point( 20, 20 );
        }
        pos.x += 40;
        pos.y += 40;
        second.setLocation( pos );
    }

}
