package uk.ac.starlink.vo;

import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.JComponent;

/**
 * Defines how a registry query is obtained for the RegistryPanel.
 *
 * @author   Mark Taylor
 * @since    19 Dec 2008
 */
public interface RegistryQueryFactory {

    /**
     * Returns the currently selected query.
     *
     * @return  query object
     */
    RegistryQuery getQuery() throws IOException;

    /**
     * May return a component which the user can interact with to select
     * a query.  If it returns null, this factory is considered to be
     * non-interactive (only capable of supplying a single fixed query).
     *
     * @return   GUI component for query selection, or null
     */
    JComponent getComponent();

    /**
     * Adds a listener which will be notified when the user has entered
     * a query.
     *
     * @param   listener  listener
     */
    void addEntryListener( ActionListener listener );

    /**
     * Removes a listener previously added by {@link #addEntryListener}.
     *
     * @return  listener  listener
     */
    void removeEntryListener( ActionListener listener );
}
