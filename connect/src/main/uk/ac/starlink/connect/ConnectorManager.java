package uk.ac.starlink.connect;

import java.awt.Component;
import java.awt.event.ActionEvent;
import uk.ac.starlink.util.Loader;

/**
 * Marshals provision of {@link Connector} objects.
 *
 * <p>By default, if the requisite classes are available in this JVM,
 * the following connectors are known:
 * <ul>
 * <li>{@link uk.ac.starlink.srb.SRBConnector} (SRB)
 * <li>{@link uk.ac.starlink.astrogrid.AcrConnector} (MySpace)
 * </ul>
 *
 * @author   Mark Taylor
 * @since    21 Feb 2005
 */
public class ConnectorManager {

    private static Connector[] connectors_;
    private static ConnectorAction[] connectorActions_;

    /** List of classnames of known {@link Connector} implementations. */
    private static final String[] KNOWN_CONNECTORS = new String[] {
        "uk.ac.starlink.astrogrid.AcrConnector",
        "uk.ac.starlink.srb.SRBConnector",
    };
            
    /**     
     * Name of property containing colon-separated list of additional
     * {@link uk.ac.starlink.connect.Connector} implementations to be made
     * available from this manager.
     */
    public static final String CONNECTORS_PROPERTY = "star.connectors";

    /**
     * Returns a list of all the known Connector objects.
     * This includes any of Connector classes which are known about by 
     * default, if the requisite classes are present, as well as 
     * any whose classes are named in the {@link #CONNECTORS_PROPERTY} 
     * system property.
     *
     * @return   array of avilable connectors
     */
    public static Connector[] getConnectors() {
        if ( connectors_ == null ) {
            connectors_ = Loader.getClassInstances( KNOWN_CONNECTORS, 
                                                    CONNECTORS_PROPERTY,
                                                    Connector.class )
                                .toArray( new Connector[ 0 ] );
        }
        return connectors_.clone();
    }

    /**
     * Returns a list of all the currently available ConnectorAction
     * objects.  There will be one for each of the {@link Connector}s
     * returned by {@link #getConnectors}.  Since a <code>ConnectorAction</code>
     * holds open a single connection at a time, this is a sensible method
     * to use if you want to get a list of the current connections to
     * various places.  If you want to be able to open guaranteed new
     * connections, use <code>getConnectors</code> itself instead.
     *
     * @return   array of connector actions
     */
    public static synchronized ConnectorAction[] getConnectorActions() {
        if ( connectorActions_ == null ) {
            Connector[] connectors = getConnectors();
            int nconn = connectors.length;
            connectorActions_ = new ConnectorAction[ nconn ];
            for ( int i = 0; i < nconn; i++ ) {
                connectorActions_[ i ] = new ConnectorAction( connectors[ i ] );
            }
        }
        return connectorActions_.clone();
    }

    /**
     * Pops up a modal dialogue which
     * invites the user to log into a connection using a given connector.
     * The return value will be either a new connection, or null if
     * the user declines to supply correct values.  The user will be
     * informed of any errors that occur and invited to retry.
     *
     * @param  parent component for the dialogue
     * @param  connector   connector
     * @return  new connection, or null
     */
    public static Connection showConnectionDialog( Component parent, 
                                                   Connector connector ) {
        ConnectorAction connAct = new ConnectorAction( connector );
        Connection connection = connAct.getConnection();
        if ( connection != null ) {
            return connection;
        }
        else {
            connAct.actionPerformed( new ActionEvent( parent, 0, "Log In" ) );
            return connAct.getConnection();
        }
    }

}
