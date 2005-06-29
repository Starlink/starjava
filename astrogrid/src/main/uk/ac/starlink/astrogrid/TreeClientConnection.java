package uk.ac.starlink.astrogrid;

import java.io.IOException;
import java.util.Map;
import org.astrogrid.community.common.security.data.SecurityToken;
import org.astrogrid.store.Ivorn;
import org.astrogrid.store.tree.TreeClient;
import org.astrogrid.store.tree.TreeClientException;
import uk.ac.starlink.connect.Branch;
import uk.ac.starlink.connect.Connection;
import uk.ac.starlink.connect.Connector;

/**
 * AstroGrid connection based on a TreeClient object.
 * Instances of this connection are automatically logged out on JVM exit.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Feb 2005
 */
class TreeClientConnection extends Connection {

    private final TreeClient tc_;
    private final Branch root_;
    private boolean finished_;

    /**
     * Constructor.
     *
     * @param  connector   connector
     * @param  authValues   map of AuthKey->string pairs giving authorization
     *                      information
     * @param  tc         tree client
     * @param  ivorn     IVO resource name
     */
    public TreeClientConnection( Connector connector, Map authValues,
                                 TreeClient tc, final Ivorn ivorn )
            throws TreeClientException {
        super( connector, authValues );
        tc_ = tc;
        root_ = new MyspaceBranch( tc_.getRoot() ) {
            public String toString() {
                return ivorn.toString();
            }
        };
        setLogoutOnExit( true );
    }

    public void logOut() throws IOException {
        finished_ = true;
        try {
            tc_.logout();
        }
        catch ( TreeClientException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    public boolean isConnected() {
        if ( finished_ ) {
            return false;
        }
        else {
            try {
                SecurityToken token = tc_.getToken();
                return token == null ? false
                                     : token.isValid();
            }

            /* This is a workaround for what I presume is a bug in TreeClient;
             * getToken() seems to throw an UnsupportedOperationException.
             * Prior to a logout, the connection ought still to be live,
             * so in this case we just return true and hope for the best. */
            catch ( UnsupportedOperationException e ) {
                return true;
            }
        }
    }

    public Branch getRoot() {
        return root_;
    }
}
