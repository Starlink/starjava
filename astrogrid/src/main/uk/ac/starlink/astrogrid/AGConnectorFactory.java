package uk.ac.starlink.astrogrid;

import java.awt.Component;

/**
 * Supplies {@link AGConnector} objects.  The usual way of obtaining
 * a connector is 
 * <pre>
 *     AGConnector conn = AGConnectorFactory.getInstance().getConnector();
 * </pre>
 * This might return a new or old connector which does or doesn't interact
 * with the user in a graphical or non-graphical way, according to 
 * system configuration and history.
 *
 * @author   Mark Taylor (Starlink)
 * @since    6 Dec 2004
 */
public class AGConnectorFactory {

    private static AGConnectorFactory instance_;
    private AGConnector connector_;

    /**
     * Constructor.
     */
    protected AGConnectorFactory() {
    }

    /**
     * Returns a default connector factory.
     *
     * @return  factory
     */
    public static AGConnectorFactory getInstance() {
        if ( instance_ == null ) {
            instance_ = new AGConnectorFactory();
        }
        return instance_;
    }

    /**
     * Returns a connector from which a connection to AstroGrid resources
     * may be obtained.
     *
     * @return   connector
     */
    public AGConnector getConnector() {
        if ( connector_ == null ) {
            connector_ = new SwingAGConnector( null );
        }
        return connector_;
    }

    /**
     * Returns a connector associated with a given graphical component.
     * If any user interaction is required, it will presumably be
     * graphical, and positioned relative to <tt>parent</tt> in some way.
     *
     * @param  parent   parent component
     */
    public AGConnector getConnector( Component parent ) {
        if ( connector_ == null ) {
            connector_ = new SwingAGConnector( parent );
        }
        else if ( connector_ instanceof SwingAGConnector ) {
            ((SwingAGConnector) connector_).setParent( parent );
        }
        else {
            SwingAGConnector conn = new SwingAGConnector( parent );
            conn.setCommunity( connector_.getCommunity() );
            conn.setUser( connector_.getUser() );
            connector_ = conn;
        }
        return connector_;
    }
}
