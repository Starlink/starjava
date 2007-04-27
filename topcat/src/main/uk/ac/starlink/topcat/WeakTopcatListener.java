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

    private final Reference baseRef_;

    /**
     * Constructor.
     *
     * @param   base  base listener
     */
    public WeakTopcatListener( TopcatListener base ) {
        baseRef_ = new WeakReference( base );
    }

    public void modelChanged( TopcatEvent evt ) {
        TopcatListener base = (TopcatListener) baseRef_.get();
        if ( base != null ) {
            base.modelChanged( evt );
        }
    }
}
