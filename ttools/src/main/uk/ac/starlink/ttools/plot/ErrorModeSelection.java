package uk.ac.starlink.ttools.plot;

import java.awt.event.ActionListener;

/**
 * Supplies an ErrorMode.
 *
 * @author   Mark Taylor
 * @since    1 Apr 2008
 */
public interface ErrorModeSelection {

    /**
     * Returns an error mode associated with this object.
     *
     * @return  error mode
     */
    ErrorMode getErrorMode();

    /**
     * Adds a listener which will be notified if the selected mode changes.
     *
     * @param  listener  listener to add
     */
    void addActionListener( ActionListener listener );

    /**
     * Removes a listener added earlier.
     *
     * @param  listener  listener to remove
     */
    void removeActionListener( ActionListener listener );
}
