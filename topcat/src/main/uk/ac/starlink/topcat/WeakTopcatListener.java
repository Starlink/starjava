package uk.ac.starlink.topcat;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * TopcatListener implementation which wraps another one but only retains
 * a weak reference to it.
 *
 * @author   Mark Taylor
 * @since    17 Apr 2007
 */
public class WeakTopcatListener implements TopcatListener {

    private final Reference<TopcatListener> baseRef_;

    /**
     * Constructor.
     *
     * @param   base  base listener
     */
    public WeakTopcatListener( TopcatListener base ) {
        baseRef_ = new WeakReference<TopcatListener>( base );
    }

    public void modelChanged( TopcatEvent evt ) {
        TopcatListener base = baseRef_.get();
        if ( base != null ) {
            base.modelChanged( evt );
        }
    }
}
