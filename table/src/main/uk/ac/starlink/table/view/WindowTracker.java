package uk.ac.starlink.table.view;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of top level windows and shuts the system down when
 * they have all been closed.
 *
 * @author   Mark Taylor (Starlink)
 */
public class WindowTracker extends WindowAdapter {

    private List windows = new ArrayList();

    /**
     * Registers a Window with this object.  When the last registered
     * window has been disposed, the JVM will be terminated.
     *
     * @param  window  a window to keep track of
     */
    public void register( Window window ) {
        windows.add( window );
        window.addWindowListener( this );
    }

    public void windowClosed( WindowEvent evt ) {
        windows.remove( evt.getWindow() );
        if ( windows.size() == 0 ) {
            System.exit( 0 );
        }
    }

}
