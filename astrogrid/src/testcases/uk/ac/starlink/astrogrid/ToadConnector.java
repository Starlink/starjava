package uk.ac.starlink.astrogrid;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.astrogrid.community.common.exception.CommunityException;
import org.astrogrid.store.tree.TreeClient;
import org.astrogrid.store.tree.TreeClientException;

public class ToadConnector implements AGConnector {

    final static String COMMUNITY = "org.astrogrid.sc2004";
    final static String USER = "toad";
    final static String PASSWORD = "qwerty";

    static {
        System.setProperty( "org.astrogrid.registry.query.endpoint",
                            "http://twmbarlwm.astrogrid.org:8080" +
                            "/astrogrid-registry-SNAPSHOT/" +
                            "services/RegistryQuery" );
        Logger.getLogger( "org" ).setLevel( Level.SEVERE );
    }

    private TreeClient client_;

    public String getCommunity() {
        return COMMUNITY;
    }

    public String getUser() {
        return USER;
    }

    public TreeClient getConnection() throws TreeClientException {
        if ( client_ == null ) {
            try {
                client_ = UserAGConnector
                         .openConnection( COMMUNITY, USER, 
                                          PASSWORD.toCharArray() );
            }
            catch ( CommunityException e ) {
                throw new TreeClientException( e.toString() );
            }
        }
        return client_;
    }
}
