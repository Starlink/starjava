package uk.ac.starlink.table.view;

import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
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

    protected JMenu fileMenu;

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
        setLocationRelativeTo( parent );

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
