package uk.ac.starlink.astrogrid;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import org.astrogrid.community.common.exception.CommunityException;
import org.astrogrid.store.tree.Container;
import org.astrogrid.store.tree.TreeClient;
import org.astrogrid.store.tree.TreeClientException;
import uk.ac.starlink.vo.RemoteTreeBrowser;

/**
 * Tree browser for MySpace files.
 *
 * @author   Mark Taylor (Starlink)
 * @since    31 Jan 2005
 */
public class MyspaceTreeBrowser extends RemoteTreeBrowser {

    private final Map trees_ = new HashMap();
    private AGConnector connector_;
    private final static Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.astrogrid" );

    protected TreeModel logIn() throws IOException {
        AGConnector conn = getConnector();
        TreeClient tc = null;
        try {
            tc = conn.getConnection();
            if ( tc != null ) {

                /* Get textual information about the session. */
                String community = conn.getCommunity();
                String user = conn.getUser();
                Container rootContainer;
                String ivorn;
                try {
                    ivorn = UserAGConnector.getIvorn( community, user )
                                           .toString();
                }
                catch ( CommunityException e ) {
                    // it's unlikely that this will happen, since it must have
                    // used the Ivorn to make the connection, but if it does
                    // just carry on without it.
                    ivorn = "???";
                }

                /* Try to get the root node from the tree. */
                rootContainer = tc.getRoot();
                TreeNode rootTreeNode =
                    new MyspaceTreeNode( null, rootContainer );
                TreeModel tm = new DefaultTreeModel( rootTreeNode );
                trees_.put( tm, tc );
                return tm;
            }
            else {
                 return null;
            }
        }

        /* Failed to get a root node.  Clear up and consider ourselves
         * logged out. */
        catch ( TreeClientException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    protected void logOut( TreeModel tree ) {
        TreeClient tc = (TreeClient) trees_.get( tree );
        try {
            tc.logout();
        }
        catch ( TreeClientException e ) {
            logger_.info( "Error logging out of MySpace " + e );
        }
    }

    /**
     * Returns the login dialogue to use.
     *
     * @return  dialogue
     */
    private AGConnector getConnector() {

        /* Lazily construct a dialogue component. */
        if ( connector_ == null ) {
            connector_ = AGConnectorFactory.getInstance().getConnector( this );
        }
        return connector_;
    }

}
