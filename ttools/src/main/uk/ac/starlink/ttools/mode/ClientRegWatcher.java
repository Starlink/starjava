package uk.ac.starlink.ttools.mode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Response;
import org.astrogrid.samp.Subscriptions;
import org.astrogrid.samp.client.CallableClient;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.SampException;

/**
 * Callable client implementation which can watch for registration of a
 * given named application.
 *
 * @author   Mark Taylor
 * @since    9 Jan 2009
 */
public class ClientRegWatcher implements CallableClient {

    private final HubConnection connection_;
    private final Map clientMap_;
    private final Map responseMap_;
    private static final String REG_MTYPE = "samp.hub.event.register";
    private static final String UNREG_MTYPE = "samp.hub.event.unregister";
    private static final String METADATA_MTYPE = "samp.hub.event.metadata";

    /**
     * Constructor.
     *
     * @param   connection   hub connection
     */
    public ClientRegWatcher( HubConnection connection ) {
        connection_ = connection;
        clientMap_ = new HashMap();
        responseMap_ = new HashMap();
    }

    /**
     * Returns the subscriptions object indicating the MTypes this client
     * can handle.
     *
     * @return   subscriptions
     */
    public Subscriptions getSubscriptions() {
        Subscriptions subs = new Subscriptions();
        subs.put( REG_MTYPE, new HashMap() );
        subs.put( UNREG_MTYPE, new HashMap() );
        subs.put( METADATA_MTYPE, new HashMap() );
        return subs;
    }

    /**
     * Blocks until a client with a given name is regstered,
     * and then returns its client ID.
     * If a given timeout value is exceeded, null is returned
     *
     * @param   name  client name sought
     * @param   waitMillis  maximum number of milliseconds to wait
     * @return  client ID for client with name <code>name</code>,
     *          or null if timeout is exceeded
     */
    public synchronized String waitForIdFromName( String name,
                                                  long waitMillis ) {
        long endTime = waitMillis >= 0 ? System.currentTimeMillis() + waitMillis
                                       : Long.MAX_VALUE;
        String id;
        while ( ( id = getIdFromName( name ) ) == null &&
                endTime - System.currentTimeMillis() > 0 ) {
            try {
                wait( endTime - System.currentTimeMillis() );
            }
            catch ( InterruptedException e ) {
                return null;
            }
        }
        return id;
    }

    /**
     * Returns the client ID of a client with a given name if one is registered.
     *
     * @param   name  client name
     * @return  ID of client with name <code>name</code>,
     *          or null if none is currently registered
     */
    public synchronized String getIdFromName( String name ) {
        for ( Iterator it = clientMap_.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            String id = (String) entry.getKey();
            Map meta = (Map) entry.getValue();
            if ( meta != null ) {
                String clientName = (String) meta.get( Metadata.NAME_KEY );
                if ( name.equalsIgnoreCase( name ) ) {
                    return id;
                }
            }
        }
        return null;
    }

    /**
     * Blocks until a response with a given tag is received, then returns
     * that response.
     *
     * @param  msgTag  message tag
     * @return   response
     */
    public synchronized Response waitForResponse( String msgTag ) {
        while ( ! responseMap_.containsKey( msgTag ) ) {
            try {
                wait();
            }
            catch ( InterruptedException e ) {
                return null;
            }
        }
        return (Response) responseMap_.get( msgTag );
    }

    public void receiveCall( String senderId, String msgId, Message msg )
            throws SampException {
        receiveNotification( senderId, msg );
        connection_.reply( msgId,
                           Response.createSuccessResponse( new HashMap() ) );
    }

    public synchronized void receiveNotification( String senderId,
                                                  Message msg ) {
        String mtype = msg.getMType();
        if ( REG_MTYPE.equals( mtype ) ) {
            String id = (String) msg.getParam( "id" );
            if ( ! clientMap_.containsKey( id ) ) {
                clientMap_.put( id, null );
                clientMap_.notifyAll();
            }
        }
        else if ( UNREG_MTYPE.equals( mtype ) ) {
            clientMap_.remove( msg.getParam( "id" ) );
            clientMap_.notifyAll();
        }
        else if ( METADATA_MTYPE.equals( mtype ) ) {
            clientMap_.put( msg.getParam( "id" ),
                            msg.getParam( "metadata" ) );
            clientMap_.notifyAll();
        }
        else {
            throw new UnsupportedOperationException( "MType " + mtype + "??" );
        }
    }

    public synchronized void receiveResponse( String responderId, String msgTag,
                                              Response response ) {
        responseMap_.put( msgTag, response );
        notifyAll();
    }
}
