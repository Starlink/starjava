package uk.ac.starlink.astrogrid;

import org.astrogrid.community.common.exception.CommunityException;
import org.astrogrid.community.common.ivorn.CommunityAccountIvornFactory;
import org.astrogrid.store.Ivorn;
import org.astrogrid.store.tree.TreeClient;
import org.astrogrid.store.tree.TreeClientException;
import org.astrogrid.store.tree.TreeClientFactory;

/**
 * Abstract AGConnector implementation which pursues the general policy
 * of asking the user once for authentication information and when 
 * it's got a working connection hangs on to it.
 *
 * @author   Mark Taylor (Starlink)
 * @since    3 Dec 2004
 */
public abstract class UserAGConnector implements AGConnector {

    private TreeClient client_;
    private String user_;
    private String community_;
    private boolean initialised_;

    /** Name of system property containing default user id. */
    public static final String USER_PROPERTY = "user.name";

    /** Name of system property containing default community id. */
    public static final String COMMUNITY_PROPERTY = 
        "org.astrogrid.community.ident";

    /**
     * Sets the user value. 
     * May be used to set a default, but it's OK to override it.
     *
     * @param  user  user id
     */
    public abstract void setUser( String user );

    /**
     * Sets the default community value.
     * May be used to set a default, but it's OK to override it.
     *
     * @param   community  community id
     */
    public abstract void setCommunity( String community );

    /**
     * Interact with the user to log in to an AstroGrid community, and
     * return an open TreeClient.
     *
     * @return  TreeClient ready for use, or null
     */
    public abstract TreeClient openNewConnection();
    
    public TreeClient getConnection() {

        /* If we don't have a working connection, try to acquire one. */
        if ( client_ == null || client_.getToken() == null ) {

            /* If we haven't tried this before, set default values for 
             * some authentication information. */
            if ( ! initialised_ ) {

                /* Community. */
                String comm = getCommunity();
                if ( comm == null || comm.trim().length() == 0 ) {
                    try {
                        String pComm = System.getProperty( COMMUNITY_PROPERTY );
                        if ( pComm != null ) {
                            setCommunity( pComm );
                        }
                    }
                    catch ( SecurityException e ) {
                        // never mind.
                    }
                }

                /* User name. */
                String user = getUser();
                if ( user == null || user.trim().length() == 0 ) {
                    try {
                        String pUser = System.getProperty( USER_PROPERTY );
                        if ( pUser != null ) {
                            setUser( pUser );
                        }
                    }
                    catch ( SecurityException e ) {
                        // never mind.
                    }
                }
            }

            /* Attempt to acquire the connection. */
            client_ = openNewConnection();
        }

        /* Return the result, whether it's null or not. */
        return client_;
    }

    /**
     * Utility method for getting an Ivorn from a community and user string.
     *
     * @param  community  community identifier
     * @param  user       user identifier
     * @return   ivorn
     */
    public static Ivorn getIvorn( String community, String user ) 
            throws CommunityException {
        return CommunityAccountIvornFactory.createIvorn( community, user );
    }

    /**
     * Utility method for opening a connection given the relevant information.
     *
     * @param  community   community identifier
     * @param  user        user identifier
     * @param  password    password
     * @return  open tree client, or <tt>null</tt>
     */
    public static TreeClient openConnection( String community, String user, 
                                             char[] password )
            throws CommunityException, TreeClientException {
        TreeClient tc = new TreeClientFactory().createClient();
        tc.login( getIvorn( community, user ), new String( password ) );
        return tc;
    }

}
