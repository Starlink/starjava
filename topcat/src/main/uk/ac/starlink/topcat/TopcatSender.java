package uk.ac.starlink.topcat;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Response;
import org.astrogrid.samp.Subscriptions;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.SampException;
import org.astrogrid.samp.httpd.ResourceHandler;
import org.astrogrid.samp.httpd.ServerResource;
import org.astrogrid.samp.httpd.UtilServer;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * Object capable of sending a table location to a running TOPCAT instance.
 *
 * @author   Mark Taylor
 * @since    2 Aug 2011
 */
public abstract class TopcatSender {
 
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
            throws IOException, SampException {
        URL locUrl = URLUtils.makeURL( location );
        String uloc = locUrl == null ? location : locUrl.toString();
        Message msg =
            createSendMessage( locUrl == null ? location : locUrl.toString(),
                               format == null ? "" : format );
        return connection_.callAndWait( topcatId_, msg, -1 );
    }

    /**
     * Returns a Message that can be sent to TOPCAT to receive a table.
     *
     * @param   uloc   location, if possible in the form of a URL
     * @param   format  STIL-friendly table format name; may be an empty string
     *                  (for auto-detection) but may not be null
     * @return  load table message
     */
    protected abstract Message createSendMessage( String uloc, String format )
            throws IOException;

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

        /* Prepare metadata for this client. */
        Metadata meta = new Metadata();
        meta.setName( "topcat-sender" );
        meta.setDescriptionText( "Attempts to send tables to "
                               + "a running TOPCAT instance" );
        meta.setIconUrl( "http://www.starlink.ac.uk/topcat/"
                       + "images/to_tc3.png" );
        meta.put( "author.name", "Mark Taylor" );
        conn.declareMetadata( meta );

        /* Prepare a message to check that TOPCAT is listening. */
        Message pingMsg = new Message( "samp.app.ping" );
        int pingWait = 2;
       
        /* Look for a TOPCAT client which subscribes to the STIL-friendly
         * table load message (versions before 3.9 may not).
         * If we find one, we can just send the location and given format
         * (which may be auto-detection). */
        Map<?,?> clientMap1 = conn.getSubscribedClients( TOPCAT_LOAD_MTYPE );
        for ( Object cid : clientMap1.keySet() ) {
            String clientId = String.valueOf( cid );
            if ( isTopcatMetadata( conn.getMetadata( clientId ) ) ) {
                conn.callAndWait( clientId, pingMsg, pingWait );
                return new TopcatSender( conn, clientId ) {
                    protected Message createSendMessage( String uloc,
                                                         String format ) {
                        return new Message( TOPCAT_LOAD_MTYPE )
                              .addParam( "url", uloc )
                              .addParam( "format", format );
                    }
                };
            }
        }

        /* Failing that, look for a TOPCAT client which subscribes to
         * table.load.votable (any topcat should do this).  We may have
         * to work harder to construct the load message in this case. */
        Map<?,?> clientMap2 = conn.getSubscribedClients( "table.load.votable" );
        for ( Object cid : clientMap2.keySet() ) {
            String clientId = String.valueOf( cid );
            if ( isTopcatMetadata( conn.getMetadata( clientId ) ) ) {
                conn.callAndWait( clientId, pingMsg, pingWait );
                final Subscriptions subs = conn.getSubscriptions( clientId );
                return new TopcatSender( conn, clientId ) {
                    protected Message createSendMessage( String uloc,
                                                         String format )
                            throws IOException {
                        return createTypedSendMessage( uloc, format, subs );
                    }
                };
            }
        }

        /* Looks like there are no topcats in sight - return null. */
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

    /**
     * Creates a table load message from a table location,
     * suitable for a client with a given subscription list.
     *
     * @param   uloc   location, if possible in the form of a URL
     * @param   format  STIL-friendly table format name; may be an empty string
     *                  (for auto-detection) but may not be null
     * @param   subs   subscription list of recipient client
     */
    private static Message createTypedSendMessage( String uloc, String format,
                                                   Subscriptions subs )
            throws IOException {

        /* If the client is subscribed to the known format, and the location
         * is actually a URL, we can just send the location directly using
         * the appropriate format-specific MType.  Probably applies only
         * to VOTable and FITs, but others are conceivable. */
        String directMtype = "table.load." + format.toLowerCase();
        if ( subs.isSubscribed( directMtype ) ) {
            try {
                return new Message( directMtype )
                      .addParam( "url", URLUtils.newURL( uloc ).toString() );
            }
            catch ( MalformedURLException e ) {
            }
        }

        /* Otherwise, we need to read the table and make it available from
         * an internal HTTP server as a VOTable, then construct a message
         * which loads that. */
        final StarTable table =
            new StarTableFactory( false ).makeStarTable( uloc, format );
        final VOTableWriter voWriter =
            new VOTableWriter( DataFormat.BINARY, true );
        ServerResource tResource = new ServerResource() {
            public long getContentLength() {
                return -1L;
            }
            public String getContentType() {
                return voWriter.getMimeType();
            }
            public void writeBody( OutputStream out ) throws IOException {
                out = new BufferedOutputStream( out );
                voWriter.writeStarTable( table, out );
            }
        };
        ResourceHandler rHandler =
            UtilServer.getInstance().getResourceHandler();
        URL voUrl = rHandler.addResource( "table", tResource );
        return new Message( "table.load.votable" )
              .addParam( "url", voUrl.toString() );
    }
}
