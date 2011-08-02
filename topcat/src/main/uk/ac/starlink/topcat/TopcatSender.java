package uk.ac.starlink.topcat;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Response;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.SampException;
import uk.ac.starlink.util.URLUtils;

/**
 * Object capable of sending a table location to a running TOPCAT instance.
 *
 * @author   Mark Taylor
 * @since    2 Aug 2011
 */
public class TopcatSender {
 
    private final HubConnection connection_;
    private final String topcatId_;
    public static final String TOPCAT_LOAD_MTYPE = "table.load.stil";
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructor.
     *
     * @param   connection  registered connection to hub
     * @param   topcatId   client ID for currently registered TOPCAT client
     */
    public TopcatSender( HubConnection connection, String topcatId ) {
        connection_ = connection;
        topcatId_ = topcatId;
    }

    /**
     * Instructs an external TOPCAT application to load a table with a given
     * location and table format.
     *
     * @param   location  table URL or filename
     * @param   format   STIL-friendly table format (null for auto)
     * @return  response from table load message
     */
    public Response sendTable( String location, String format )
            throws SampException {
        URL locUrl = URLUtils.makeURL( location );
        String uloc = locUrl == null ? location : locUrl.toString();
        Message msg = new Message( TOPCAT_LOAD_MTYPE )
                     .addParam( "url", uloc )
                     .addParam( "format", format == null ? "" : format );
        return connection_.callAndWait( topcatId_, msg, -1 );
    }

    /**
     * Unregisters the client associated with this sender.
     */
    public void close() {
        unregister( connection_ );
    }

    /**
     * Attempts to create and return a TopcatSender for a given profile.
     * If none can be constructed (for instance no hub or no external
     * TOPCAT client found), null is returned.
     *
     * @param   profile   client profile
     * @return   working topcat sender, or null
     */
    public static TopcatSender createSender( ClientProfile profile ) {
        if ( ! profile.isHubRunning() ) {
            return null;
        }
        HubConnection conn;
        try {
            conn = profile.register();
        }
        catch ( SampException e ) {
            return null;
        }
        if ( conn == null ) {
            return null;
        }
        TopcatSender sender = null;
        try {
            sender = attemptCreateSender( conn );
        }
        catch ( SampException e ) {
            sender = null;
        }
        finally {
            if ( sender == null ) {
                unregister( conn );
            }
        }
        return sender;
    }
 
    /**
     * Attempts to create and return a TopcatSender given a connection.
     *
     * @param  conn  hub connection
     * @return   new sender, or null
     */
    private static TopcatSender attemptCreateSender( HubConnection conn )
            throws SampException {
        Metadata meta = new Metadata();
        meta.setName( "topcat-sender" );
        meta.setDescriptionText( "Attempts to send tables to "
                               + "a running TOPCAT instance" );
        meta.setIconUrl( "http://www.starlink.ac.uk/topcat/"
                       + "images/tc3.gif" );
        meta.put( "author.name", "Mark Taylor" );
        conn.declareMetadata( meta );
        Map clientMap = conn.getSubscribedClients( TOPCAT_LOAD_MTYPE );
        for ( Iterator it = clientMap.keySet().iterator(); it.hasNext(); ) {
            String clientId = (String) it.next();
            if ( isTopcatMetadata( conn.getMetadata( clientId ) ) ) {
                conn.callAndWait( clientId, new Message( "samp.app.ping"), 2 );
                return new TopcatSender( conn, clientId );
            }
        }
        return null;
    }

    /**
     * Unregisters a given hub connection, taking care of exceptions.
     *
     * @param  conn  connection
     */
    private static void unregister( HubConnection conn ) {
        try {
            conn.unregister();
        }
        catch ( SampException e ) {
            logger_.warning( "Failed to unregister: " + e );
        }
    }

    /**
     * Indicates whether a SAMP client appears to be an instance of TOPCAT.
     *
     * @param  meta  client metadata
     * @return  true iff client metadata looks like TOPCAT's
     */
    private static boolean isTopcatMetadata( Metadata meta ) {
        return meta.getName().equalsIgnoreCase( "topcat" );
    }
}
