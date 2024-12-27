package uk.ac.starlink.util.gui;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * List data listener which delegates to another one as long as it is
 * reachable, but only retains a weak reference to it.
 * Adding a listener to an object in this way will not prevent the listener
 * (and any of its references) from being garbage collected.
 *
 * @author   Mark Taylor
 * @since    20 Jan 2005
 */
public class WeakListDataListener implements ListDataListener {

    private final Reference<ListDataListener> baseRef_;

    /**
     * Constructs a new listener based on an existing one.
     *
     * @param base listener
     */
    public WeakListDataListener( ListDataListener base ) {
        baseRef_ = new WeakReference<ListDataListener>( base );
    }

    public void contentsChanged( ListDataEvent evt ) {
        ListDataListener base = baseRef_.get();
        if ( base != null ) {
            base.contentsChanged( evt );
        }
    }

    public void intervalAdded( ListDataEvent evt ) {
        ListDataListener base = baseRef_.get();
        if ( base != null ) {
            base.intervalAdded( evt );
        }
    }

    public void intervalRemoved( ListDataEvent evt ) {
        ListDataListener base = baseRef_.get();
        if ( base != null ) {
            base.intervalRemoved( evt );
        }
    }
}
