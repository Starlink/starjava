package uk.ac.starlink.table.view;

import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import javax.swing.JFrame;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarTableModel;

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
    }

    /**
     * Constructs an AuxWindow based on a <tt>StarTableModel</tt>.
     * 
     * @param  baseTitle  the window basic title (the name of <tt>startab</tt>
     *         will be incorporated into the title if it has one
     * @param  stmodel   the StarTableModel which this window describes
     * @param  parent   the parent component of the new window - may be
     *         used for positioning
     */
    public AuxWindow( String baseTitle, StarTableModel stmodel, 
                      Component parent ) {
        this( baseTitle, stmodel.getStarTable(), parent );
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
