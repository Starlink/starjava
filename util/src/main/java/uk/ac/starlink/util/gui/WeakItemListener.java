package uk.ac.starlink.util.gui;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Item listener which delegates to another one as long as it is
 * reachable, but only retains a weak reference to it.
 * Adding a listener to an object in this way will not prevent the listener
 * (and any of its references) from being garbage collected.
 *
 * @author   Mark Taylor
 * @since    20 Jan 2005
 */
public class WeakItemListener implements ItemListener {

    private final Reference<ItemListener> baseRef_;

    /**
     * Constructs a new listener based on an existing one.
     *
     * @param base listener
     */
    public WeakItemListener( ItemListener base ) {
        baseRef_ = new WeakReference<ItemListener>( base );
    }

    public void itemStateChanged( ItemEvent evt ) {
        ItemListener base = baseRef_.get();
        if ( base != null ) {
            base.itemStateChanged( evt );
        }
    }
}
