package uk.ac.starlink.plastic;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;
import org.apache.xmlrpc.XmlRpcHandler;
import org.votech.plastic.PlasticListener;
import org.votech.plastic.PlasticHubListener;

/**
 * Minimal PLASTIC hub implementation.  No fancy add-ons such as logging
 * are provided.  See {@link PlasticHub} for a more user-friendly 
 * implementation.  This class is provided mainly to make it
 * as clear as possible what the implementation of a hub involves.
 *
 * @author   Mark Taylor 
 * @since    18 Jul 2006
 */
public class MinimalHub implements PlasticHubListener, XmlRpcHandler {

    private final ServerSet servers_;
    private final Map agentMap_;
    private final URI hubId_;
    private int nReg_;
    private boolean stopped_;

    private static final String HUB_ID_KEY = "hub.id";
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.plastic" );

    /**
     * Constructs a new hub, given running server objects.
     *
     * @param  servers  object encapsulating listening servers
     */
    public MinimalHub( ServerSet servers ) throws RemoteException {
        servers_ = servers;
        agentMap_ = Collections.synchronizedMap( new HashMap() );

        /* Listen for PLASTIC requests on RMI server. */
        servers_.getRmiServer()
                .publish( PlasticHubListener.class, this,
                          new Class[] { PlasticListener.class } );

        /* Listen for PLASTIC requests on XML-RPC server. */
        servers_.getXmlRpcServer().addHandler( "plastic.hub", this );

        /* Arrange to tidy up if the JVM terminates. */
        Runtime.getRuntime().addShutdownHook( new Thread() {
            public void run() {
                MinimalHub.this.stop();
            }
        } );

        /* Create and register an application which will serve as a
         * representative of this hub.  It will answer questions like
         * what version are you, what is your name, etc. */
        BasicApplication servApp = new BasicApplication( "hub" );
        servApp.setDescription( "PlasKit hub" );
        hubId_ = registerRMI( servApp.getName(),
                              Arrays.asList( servApp.getSupportedMessages() ),
                              servApp );
    }

    /**
     * Returns a map which maps the IDs of currently registered applications
     * to their Agent objects.
     *
     * <p>Note that this map is <strong>synchronized</strong>, and use of
     * any iterator obtained from it must be contained in a 
     * <code>synchronized</code> block.
     *
     * @return   URI->Agent map for registered listeners
     * @see     java.util.Collections#synchronizedMap
     */
    Map getAgentMap() {
        return agentMap_;
    }

    public URI getHubId() {
        return hubId_;
    }

    public URI registerRMI( String name, List supportedMessages,
                            PlasticListener caller ) {
        Agent agent = new RmiAgent( ++nReg_, name,
                                    toUriArray( supportedMessages ), caller );
        register( agent );
        return agent.getId();
    }

    public URI registerXMLRPC( String name, List supportedMessages,
                               URL callbackURL ) {
        Agent agent = new XmlRpcAgent( ++nReg_, name,
                                       toUriArray( supportedMessages ),
                                       callbackURL );
        register( agent );
        return agent.getId();
    }

    public URI registerNoCallBack( String name ) {
        Agent agent = new NoCallBackAgent( ++nReg_, name );
        register( agent );
        return agent.getId();
    }

    /**
     * Performs generic registration of an agent (listening application).
     *
     * @param  agent  object representing the registering application
     */
    void register( Agent agent ) {
        URI id = agent.getId();
        agentMap_.put( id, agent );
        requestAsynch( hubId_, MessageId.HUB_APPREG,
                       Collections.singletonList( id.toString() ) );
    }

    public void unregister( URI id ) {
        while ( agentMap_.containsKey( id ) ) {
            Agent agent = (Agent) agentMap_.get( id );
            requestAsynch( hubId_, MessageId.HUB_APPUNREG,
                           Collections.singletonList( id.toString() ) );
            agentMap_.remove( id );
        }
    }

    public List getRegisteredIds() {
        return new ArrayList( agentMap_.keySet() );
    }

    public String getName( URI id ) {
        Agent agent = (Agent) agentMap_.get( id );
        return agent == null ? null : agent.getName();
    }

    public List getUnderstoodMessages( URI id ) {
        Agent agent = (Agent) agentMap_.get( id );
        return agent == null
             ? null
             : new ArrayList( Arrays.asList( agent.getSupportedMessages() ) );
    }

    public List getMessageRegisteredIds( URI message ) {
        List supporters = new ArrayList();
        synchronized ( agentMap_ ) {
            for ( Iterator it = agentMap_.entrySet().iterator();
                  it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                URI id = (URI) entry.getKey();
                Agent agent = (Agent) entry.getValue();
                URI[] messages = agent.getSupportedMessages();
                boolean add = ( messages == null || messages.length == 0 )
                            ? isPre05()
                            : Arrays.asList( messages ).contains( message );
                if ( add ) {
                    supporters.add( id );
                }
            }
        }
        return supporters;
    }

    /**
     * This method implements all the XML-RPC services offered by the hub.
     * It works out which hub method is intended and forwards the arguments
     * to the relevant java method of this object.  A certain amount of
     * fiddling around is required to get the types right.
     *
     * @param   method  XML-RPC method name, including the "plastic.hub" 
     *                  prefix
     * @param   params  list of method arguments
     */
    public Object execute( String method, Vector params )
            throws IOException, URISyntaxException {
        List paramList = new ArrayList( params );
        final Object result;
        if ( "plastic.hub.getRegisteredIds".equals( method ) ) {
            result = getRegisteredIds();
        }
        else if ( "plastic.hub.getHubId".equals( method ) ) {
            result = getHubId().toString();
        }
        else if ( "plastic.hub.getName".equals( method ) ) {
            URI id = new URI( (String) paramList.remove( 0 ) );
            result = getName( id );
        }
        else if ( "plastic.hub.getUnderstoodMessages".equals( method ) ) {
            URI id = new URI( (String) paramList.remove( 0 ) );
            result = getUnderstoodMessages( id );
        }
        else if ( "plastic.hub.getMessageRegisteredIds".equals( method ) ) {
            URI message = new URI( (String) paramList.remove( 0 ) );
            result = getMessageRegisteredIds( message );
        }

        else if ( "plastic.hub.registerXMLRPC".equals( method ) ) {
            String name = (String) paramList.remove( 0 );
            List supportedMessages = (List) paramList.remove( 0 );
            URL callBackUrl = new URL( (String) paramList.remove( 0 ) );
            result = registerXMLRPC( name, toUriList( supportedMessages ),
                                     callBackUrl );
        }
        else if ( "plastic.hub.registerRMI".equals( method ) ) {
            throw new IOException( "Can't registerRMI using XML-RPC" );
        }
        else if ( "plastic.hub.registerNoCallBack".equals( method ) ) {
            String name = (String) paramList.remove( 0 );
            result = registerNoCallBack( name );
        }
        else if ( "plastic.hub.unregister".equals( method ) ) {
            URI id = new URI( (String) paramList.remove( 0 ) );
            unregister( id );
            result = null;
        }

        else if ( "plastic.hub.request".equals( method ) ) {
            URI sender = new URI( (String) paramList.remove( 0 ) );
            URI message = new URI( (String) paramList.remove( 0 ) );
            List args = (List) paramList.remove( 0 );
            result = request( sender, message, args );
        }
        else if ( "plastic.hub.requestAsynch".equals( method ) ) {
            URI sender = new URI( (String) paramList.remove( 0 ) );
            URI message = new URI( (String) paramList.remove( 0 ) );
            List args = (List) paramList.remove( 0 );
            requestAsynch( sender, message, args );
            result = null;
        }
        else if ( "plastic.hub.requestToSubset".equals( method ) ) {
            URI sender = new URI( (String) paramList.remove( 0 ) );
            URI message = new URI( (String) paramList.remove( 0 ) );
            List args = (List) paramList.remove( 0 );
            List recipientIds = (List) paramList.remove( 0 );
            result = requestToSubset( sender, message, args,
                                      toUriList( recipientIds ) );
        }
        else if ( "plastic.hub.requestToSubsetAsynch".equals( method ) ) {
            URI sender = new URI( (String) paramList.remove( 0 ) );
            URI message = new URI( (String) paramList.remove( 0 ) );
            List args = (List) paramList.remove( 0 );
            List recipientIds = (List) paramList.remove( 0 );
            requestToSubsetAsynch( sender, message, args,
                                   toUriList( recipientIds ) );
            result = null;
        }

        else {
            throw new UnsupportedOperationException( "No method " + method
                                                   + " on hub" );
        }
        Object xResult = XmlRpcAgent.doctorObject( result );
        return xResult;
    }

    public Map request( URI sender, URI message, List args ) {
        return requestTo( sender, message, args, getOtherAgents( sender ) );
    }

    public Map requestToSubset( URI sender, URI message, List args,
                                List recipientIds ) {
        return requestTo( sender, message, args,
                          getSelectedAgents( recipientIds ) );
    }

    public void requestAsynch( URI sender, URI message, List args ) {
        requestAsynchTo( sender, message, args, getOtherAgents( sender ) );
    }

    public void requestToSubsetAsynch( URI sender, URI message, List args,
                                       List recipientIds ) {
        requestAsynchTo( sender, message, args,
                         getSelectedAgents( recipientIds ) );
    }

    /**
     * Not supported.
     *
     * @throws  UnsupportedOperationException
     */
    public URI registerPolling( String name, List supportedMessages ) {
        throw new UnsupportedOperationException( "Polling not supported" );
    }

    /**
     * Not supported.
     *
     * @throws  UnsupportedOperationException
     */
    public List pollForMessages( URI id ) {
        throw new UnsupportedOperationException( "Polling not supported" );
    }

    /**
     * Returns an array of all registered agents except for the one with
     * a given identifier.
     *
     * @param   excluded  URI to exclude
     * @return   array of all other agents
     */
    private Agent[] getOtherAgents( URI excluded ) {
        List agentList = new ArrayList();
        synchronized ( agentMap_ ) {
            for ( Iterator it = agentMap_.keySet().iterator(); it.hasNext(); ) {
                URI id = (URI) it.next();
                if ( id != null && ! id.equals( excluded ) ) {
                    Agent agent = (Agent) agentMap_.get( id );
                    if ( agent != null ) {
                        agentList.add( agent );
                    }
                }
            }
        }
        return (Agent[]) agentList.toArray( new Agent[ 0 ] );
    }

    /**
     * Returns an array of the agents corresponding to a given list of
     * identifiers.
     *
     * @param  idList  list of URIs identifying registered agents
     * @return  array of agents corresponding to <code>idList</code>
     */
    private Agent[] getSelectedAgents( List idList ) {
        List agentList = new ArrayList();
        for ( Iterator it = idList.iterator(); it.hasNext(); ) {
            Agent agent = (Agent) agentMap_.get( it.next() );
            if ( agent != null ) {
                agentList.add( agent );
            }
        }
        return (Agent[]) agentList.toArray( new Agent[ 0 ] );
    }

    /**
     * Performs the work of a synchronous request to a given list of agents.
     *
     * @param  sender  sender ID
     * @param  message  message ID
     * @param  args    message arguments
     * @param  agents  list of agents to which the request will be multiplexed
     */
    Map requestTo( URI sender, URI message, List args, Agent[] agents ) {

        /* Construct and start worker threads. */
        RequestThread[] threads = 
            startRequestThreads( sender, message, args, agents );

        /* Wait for all the threads to finish and assemble the result map. */
        Map results = new HashMap();
        for ( int i = 0; i < threads.length; i++ ) {
            RequestThread thread = threads[ i ];
            Object result;
            try {
                thread.join();
                result = thread.getResult();
            }
            catch ( IOException e ) {
                result = null;
            }
            catch ( InterruptedException e ) {
                result = null;
            }
            results.put( thread.getAgent().getId(), result );
        }

        /* Return the result map. */
        return results;
    }

    /**
     * Performs the work of an asynchronous request to a given list of agents.
     *
     * @param  sender  sender ID
     * @param  message  message ID
     * @param  args    message arguments
     * @param  agents  list of agents to which the request will be multiplexed
     */
    void requestAsynchTo( URI sender, URI message, List args, Agent[] agents ) {
        startRequestThreads( sender, message, args, agents );
    }

    /**
     * Prepares and starts working an array of threads which perform a
     * given request to zero or more agents.
     *
     * @param  sender  sender ID
     * @param  message message ID
     * @param  agents  list of agents to which the request will be multiplexed
     *                 (if supported)
     */
    private RequestThread[] startRequestThreads( URI sender, URI message,
                                                 List args, Agent[] agents ) {

        /* Assemble a list of threads which will perform the synchronous
         * calls on each agent. */
        List threadList = new ArrayList();
        for ( int i = 0; i < agents.length; i++ ) {
            final Agent agent = agents[ i ];
            if ( agent.supportsMessage( message ) ) {
                threadList.add( createRequestThread( agent, sender, message,
                                                     args ) );
            }
        }
        RequestThread[] threads = 
            (RequestThread[]) threadList.toArray( new RequestThread[ 0 ] );

        /* Start each one off. */
        for ( int i = 0; i < threads.length; i++ ) {
            threads[ i ].start();
        }

        /* Return the thread list. */
        return threads;
    }

    /**
     * Creates a new thread whose run method will execute a PLASTIC 
     * request to a given agent.
     *
     * @param  agent   agent representing the listening application
     * @param  sender  source of the message
     * @param  message message ID
     * @param  args    messsage arguments
     */
    RequestThread createRequestThread( Agent agent, URI sender, URI message,
                                       List args ) {
        return new RequestThread( agent, sender, message, args );
    }

    /**
     * Indicates whether this implementation is prior to version 0.5 of
     * the PLASTIC protocol.  Before 0.5 applications registering with
     * an empty list should receive all messages.  At 0.5 and later 
     * it has no special meaning.
     *
     * @return   true iff the PLASTIC protocol version is earlier than 0.5
     */
    boolean isPre05() {
        return true;
    }

    /**
     * Shuts down this hub and tidies up.  Its main job is to send HUB_STOPPING
     * messages to all registered listeners.
     * May safely be called multiple times.
     * It is good practice to call this method if the hub is no longer
     * required to run.  However, it will be called automatically if the
     * hub is finalised or the JVM shuts down normally.
     */
    public void stop() {
        boolean stop;
        synchronized ( this ) {
            stop = ! stopped_;
            stopped_ = true;
        }
        if ( stop ) {
            try {
                requestAsynch( getHubId(), MessageId.HUB_STOPPING,
                               new ArrayList() );
            }
            catch ( Exception e ) {
            }
            try {
                servers_.stop();
            }
            catch ( Exception e ) {
            }
        }
    }

    /**
     * Returns whether the hub has finished operations.
     *
     * @return  true iff this hub has terminated
     */
    synchronized boolean isStopped() {
        return stopped_;
    }

    public void finalize() throws Throwable {
        try {
            stop();
        }
        finally {
            super.finalize();
        }
    }

    /**
     * Turns a List of URI objects into an array of URI objects.
     *
     * @param  list  uri list
     * @return uri array
     */
    private URI[] toUriArray( List list ) {
        return (URI[]) list.toArray( new URI[ 0 ] );
    }

    /**
     * Turns a list of string-or-URI objects into a list of URI objects.
     *
     * @param  list  list of objects
     * @return list of URIs
     */
    private List toUriList( List list ) {
        if ( list == null ) {
            return null;
        }
        List uriList = new ArrayList();
        for ( Iterator it = list.iterator(); it.hasNext(); ) {
            Object obj = it.next();
            if ( obj instanceof URI ) {
                uriList.add( obj );
            }
            else if ( obj instanceof String ) {
                try {
                    uriList.add( new URI( (String) obj ) );
                }
                catch ( URISyntaxException e ) {
                    logger_.warning( "Bad URI string: " + obj );
                }
            }
            else {
               logger_.warning( "Can't make URI from " + obj );
            }
        }
        return uriList;
    }

    /**
     * Creates a unique URI.
     * This utility method is used to come up with a new ID not used before.
     *
     * @param  obj  object the ID is for
     * @param  text  short descriptive name of the type of object
     * @param  iseq  integer value which differs from values previously
     *         passed to this routine with the same object type and text
     * @param  ID which differs from previous ones returned by this method
     */
    static URI createId( Object obj, String text, int iseq ) {
        text = text.replaceAll( "[^A-Za-z0-9_]+", "_" );
        try {
            return new URI( "plastic://" + obj.getClass().getName() + "/" +
                            Integer.toHexString( System
                                                .identityHashCode( obj ) ) +
                            "-" + iseq + "-" + text );
        }
        catch ( URISyntaxException e ) {
            throw new AssertionError( e );
        }
    }

    /**
     * Main method.  Starts the hub.  Nothing fancy.
     * For more configurability and usability features, see
     * {@link PlasticHub#main}.
     */
    public static void main( String[] args )
            throws IOException, RemoteException {
        if ( args.length > 0 ) {
            System.err.println( "Usage: " + MinimalHub.class.getName() );
            System.err.println( "See uk.ac.starlink.plastic.PlasticHub "
                              + "for fancy options"  );
            System.exit( 1 );
        }
        final ServerSet servers =
            new ServerSet( new File( System.getProperty( "user.home" ),
                           PlasticHubListener.PLASTIC_CONFIG_FILENAME ) );
        final MinimalHub hub = new MinimalHub( servers );
        Runtime.getRuntime().addShutdownHook( new Thread() {
            public void run() {
                hub.stop();
            }
        } );
    }
}
