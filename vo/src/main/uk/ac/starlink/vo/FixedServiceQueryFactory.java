package uk.ac.starlink.vo;

import java.awt.event.ActionListener;
import javax.swing.JComponent;

/**
 * QueryFactory impelementation for selecting resources which have 
 * Capabilities with a given standardID.
 *
 * @author   Mark Taylor
 * @since    19 Dec 2008
 */
public class FixedServiceQueryFactory implements RegistryQueryFactory {

    private final RegistryQuery query_;

    /**
     * Constructor.
     *
     * @param  standardId  required capability/@standardID value
     */
    public FixedServiceQueryFactory( String standardId ) {
        query_ = new RegistryQuery( RegistryQuery.AG_REG,
                                    "capability/@standardID = '" + standardId
                                                                 + "'" );
    }

    public RegistryQuery getQuery() {
        return query_;
    }

    public JComponent getComponent() {
        return null;
    }

    public void addEntryListener( ActionListener listener ) {
    }

    public void removeEntryListener( ActionListener listener ) {
    }
}
