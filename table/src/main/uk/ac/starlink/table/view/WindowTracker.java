package uk.ac.starlink.table.view;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of top level windows and shuts the system down when
 * they have all been closed.  An <tt>isActive</tt> flag is also 
 * provided; if unset, this WindowTracker effectively does nothing.
 *
 * @author   Mark Taylor (Starlink)
 */
public class WindowTracker extends WindowAdapter {

    private List windows = new ArrayList();
    private boolean isActive;

    /**
     * Constructs a new active WindowTracker.
     */
    public WindowTracker() {
        this( true );
    }

    /**
     * Constructs a new WindowTracker specifying whether it is to be 
     * active or not.   As long as <tt>isActive</tt> is false, the new
     * tracker will effectively do nothing.
     *
     * @param  isActive  whether the tracker is active or not
     */
    public WindowTracker( boolean isActive ) {
        this.isActive = isActive;
    }

    /**
     * Registers a Window with this object.  When the last registered
     * window has been disposed, then if <tt>isActive</tt> is true, 
     * the JVM will be terminated.
     *
     * @param  window  a window to keep track of
     */
    public void register( Window window ) {
        windows.add( window );
        window.addWindowListener( this );
    }

    /**
     * Sets whether this tracker is active or not.  If set (the default)
     * then closing the last registered window exits the JVM. 
     * If unset, this tracker does nothing.
     */
    public void setActive( boolean isActive ) {
        this.isActive = isActive;
    }

    public void windowClosed( WindowEvent evt ) {
        windows.remove( evt.getWindow() );
        if ( windows.size() == 0 && isActive ) {
            System.exit( 0 );
        }
    }

}
