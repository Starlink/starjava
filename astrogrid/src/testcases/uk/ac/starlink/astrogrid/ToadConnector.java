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
    private boolean tried_;

    public String getCommunity() {
        return COMMUNITY;
    }

    public String getUser() {
        return USER;
    }

    public TreeClient getConnection() throws TreeClientException {
        if ( tried_ ) {
            return client_;
        }
        tried_ = true;
        try {
            try {
                client_ = UserAGConnector
                         .openConnection( COMMUNITY, USER, 
                                          PASSWORD.toCharArray() );
            }
            catch ( CommunityException e ) {
                throw new TreeClientException( e.toString() );
            }
            return client_;
        }
        catch ( Exception e ) {
            for ( Throwable th = e; th != null; th = e.getCause() ) {
                String msg = e.getMessage();
                if ( msg.matches( ".*timed out.*" ) ) {
                    Logger.getLogger( "uk.ac.starlink.astrogrid" )
                          .warning( msg + " - probably not an " +
                                    "error in test code" );
                    return null;
                }
            }
            if ( e instanceof TreeClientException ) {
                throw (TreeClientException) e;
            }
            else {
                throw new TreeClientException( e.getMessage(), e );
            }
        }
    }
}
