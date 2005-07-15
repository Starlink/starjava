package uk.ac.starlink.astrogrid;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.astrogrid.community.common.exception.CommunityException;
import org.astrogrid.community.common.ivorn.CommunityAccountIvornFactory;
import org.astrogrid.store.Ivorn;
import org.astrogrid.store.tree.TreeClient;
import org.astrogrid.store.tree.TreeClientException;
import org.astrogrid.store.tree.TreeClientFactory;
import uk.ac.starlink.connect.AuthKey;
import uk.ac.starlink.connect.Connection;
import uk.ac.starlink.connect.Connector;
import uk.ac.starlink.util.Loader;

/**
 * Connector for connecting to AstroGrid community.
 * This permits access to a MySpace filestore.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Feb 2005
 */
public class TreeClientConnector implements Connector {

    /** Name of system property containing default community id. */
    public static final String COMMUNITY_PROPERTY =
        "org.astrogrid.community.ident";

    /** Name of system property containing registry query endpoint. */
    public static final String REGISTRY_PROPERTY =
        "org.astrogrid.registry.query.endpoint";

    private static Icon icon_;
    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.astrogrid" );

    private static final AuthKey REGISTRY_KEY = new AuthKey( "Registry" );
    private static final AuthKey COMMUNITY_KEY = new AuthKey( "Community" );
    private static final AuthKey USER_KEY = new AuthKey( "User" );
    private static final AuthKey PASSWORD_KEY = new AuthKey( "Password" );

    private static final AuthKey[] AUTH_KEYS = new AuthKey[] {
        REGISTRY_KEY,
        COMMUNITY_KEY,
        USER_KEY,
        PASSWORD_KEY,
    };

    static {
        PASSWORD_KEY.setHidden( true );
        REGISTRY_KEY.setRequired( true );
        try {
            Loader.loadProperties();
            COMMUNITY_KEY.setDefault( System
                                     .getProperty( COMMUNITY_PROPERTY ) );
            REGISTRY_KEY.setDefault( System
                                    .getProperty( REGISTRY_PROPERTY ) );
        }
        catch ( SecurityException e ) {
            logger_.info( "Security manager blocks reading default " +
                          "AG login fields" );
        }
        if ( REGISTRY_KEY.getDefault() == null ) {
            REGISTRY_KEY.setDefault(
                "http://grendel02.roe.ac.uk:8080/" +
                "astrogrid-registry-SNAPSHOT/services/RegistryQuery" );
        }
    }

    public String getName() {
        return "MySpace";
    }

    public AuthKey[] getKeys() {
        return (AuthKey[]) AUTH_KEYS.clone();
    }

    public Icon getIcon() {
        if ( icon_ == null ) {
            URL url = getClass().getResource( "AGlogo.gif" );
            if ( url != null ) {
                icon_ = new ImageIcon( url );
            }
        }
        return icon_;
    }

    public Connection logIn( Map authValues ) throws IOException {
        TreeClient tc;
        try {
            tc = new TreeClientFactory().createClient();
        }
        catch ( Throwable th ) {
            throw (IOException)
                  new IOException( "Required classes not present" )
                 .initCause( th );
        }
        String community = (String) authValues.get( COMMUNITY_KEY );
        String user = (String) authValues.get( USER_KEY );
        char[] password = (char[]) authValues.get( PASSWORD_KEY );
        try {
            Ivorn ivorn = getIvorn( community, user );
            tc.login( ivorn , password == null ? "" : new String( password ) );
            return new TreeClientConnection( this, authValues, tc, ivorn );
        }
        catch ( TreeClientException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
        catch ( CommunityException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
        finally {
            if ( password != null ) {
                Arrays.fill( password, ' ' );
            }
        }
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
}
