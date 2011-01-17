package uk.ac.starlink.vo;

import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
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
     * Returns a registry query suitable for this query factory which
     * queries a given list of IVO identifiers.
     *
     * @param  ivoids  ivo:-type resource identifiers
     * @return  registry query whose results are suitable for a result
     *          of this query factory; may be null
     */
    RegistryQuery getIdListQuery( String[] ivoids )
        throws MalformedURLException;

    /**
     * Returns the registry component object associated with this object.
     *
     * @return   registry selector
     */
    RegistrySelector getRegistrySelector();

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
     * @param  listener  listener
     */
    void removeEntryListener( ActionListener listener );
}
