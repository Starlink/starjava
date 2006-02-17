package uk.ac.starlink.plastic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import org.apache.xmlrpc.XmlRpcHandler;
import org.votech.plastic.PlasticHubListener;
import org.votech.plastic.PlasticListener;

/**
 * PLASTIC hub implementation.
 *
 * <p>A {@link #main} method is provided for standalone use.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2006
 */
public class PlasticHub implements PlasticHubListener, XmlRpcHandler {

    private final URI hubId_;
    private final Map agentMap_;
    private final ServerSet servers_;
    private boolean stopped_;
    private PrintStream out_;
    private int nReg_;
    private int nReq_;
    private boolean verbose_;

    private static final String HUB_ID_KEY = "hub.id";
    static final URI HUB_STOPPING =
        createURI( "ivo://votech.org/hub/event/HubStopping" );
    static final URI APP_REG =
        createURI( "ivo://votech.org/hub/event/ApplicationRegistered" );
    static final URI APP_UNREG =
        createURI( "ivo://votech.org/hub/event/ApplicationUnregistered" );

    /**
     * Constructs a new hub, given running server objects.
     *
     * @param   servers   object encapsulating listening servers
     */
    public PlasticHub( ServerSet servers ) throws RemoteException {
        servers_ = servers;
        agentMap_ = new HashMap();
        hubId_ = createId( this, "hub", 0 );

        /* Listen for PLASTIC requests on RMI server. */
        servers_.getRmiServer()
                .publish( PlasticHubListener.class, this,
                          new Class[] { PlasticListener.class } );

        /* Listen for PLASTIC requests on XML-RPC server. */
        servers_.getXmlRpcServer().addHandler( "plastic.hub", this );

        /* Arrange to tidy up if the JVM terminates. */
        Runtime.getRuntime().addShutdownHook( new Thread() {
            public void run() {
                PlasticHub.this.stop();
            }
        } );
    }

    public URI getHubId() {
        return hubId_;
    }

    public URI registerRMI( String name, List supportedMessages,
                            PlasticListener caller ) {
        Agent agent = new RmiAgent( ++nReg_, name,
                                    (URI[]) supportedMessages
                                           .toArray( new URI[ 0 ] ),
                                    caller );
        register( agent );
        return agent.getId();
    }

    public URI registerXMLRPC( String name, List supportedMessages,
                               URL callbackURL ) {
        URI[] messages = new URI[ supportedMessages.size() ];
        for ( int i = 0; i < supportedMessages.size(); i++ ) {
            Object msg = supportedMessages.get( i );
            if ( msg instanceof URI ) {
                messages[ i ] = (URI) msg;
            }
            else if ( msg instanceof String ) {
                try {
                    messages[ i ] = new URI( (String) msg );
                }
                catch ( URISyntaxException e ) {
                }
            }
        }
        Agent agent = new XmlRpcAgent( ++nReg_, name, messages, callbackURL );
        register( agent );
        return agent.getId();
    }

    public URI registerNoCallBack( String name ) {
        Agent agent = new NoCallBackAgent( ++nReg_, name );
        register( agent );
        return agent.getId();
    }

    public void unregister( URI id ) {
        while ( agentMap_.containsKey( id ) ) {
            Agent agent = (Agent) agentMap_.get( id );
            if ( verbose_ ) {
                out( "Unregister: " + agent );
                out();
            }
            requestAsynch( hubId_, APP_UNREG,
                           Arrays.asList( new Object[] { id } ) );
            agentMap_.remove( id );
        }
    }

    /**
     * Performs generic registration of an agent (listening application).
     *
     * @param  agent  object representing the registering application
     */
    private void register( Agent agent ) {
        URI id = agent.getId();
        agentMap_.put( id, agent );
        if ( verbose_ ) {
            out( "Register: " + agent );
            URI[] msgs = agent.getSupportedMessages();
            if ( msgs.length > 0 ) {
                out( "    Supported Messages:" );
                for ( int i = 0; i < msgs.length; i++ ) {
                    out( "        " + msgs[ i ] );
                }
            }
            out();
        }
        requestAsynch( hubId_, APP_REG,
                       Arrays.asList( new Object[] { id } ) );
    }

    public List getRegisteredIds() {
        return new ArrayList( agentMap_.keySet() );
    }

    public Object execute( String method, Vector params )
            throws IOException, URISyntaxException {

        /* For each known hub operation, turn it from an XML-RPC method name
         * to a call of one of the methods on this object.  Some of the
         * arguments need changing from XML-RPC types to the types 
         * defined by the PlasticHubListener interface. */
        List paramList = new ArrayList( params );
        final Object result;
        if ( "plastic.hub.request".equals( method ) ) {
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
            result = requestToSubset( sender, message, args, recipientIds );
        }
        else if ( "plastic.hub.requestToSubsetAsynch".equals( method ) ) {
            URI sender = new URI( (String) paramList.remove( 0 ) );
            URI message = new URI( (String) paramList.remove( 0 ) );
            List args = (List) paramList.remove( 0 );
            List recipientIds = (List) paramList.remove( 0 );
            requestToSubsetAsynch( sender, message, args, recipientIds );
            result = null;
        }
        else if ( "plastic.hub.getHubId".equals( method ) ) {
            result = getHubId().toString();
        }
        else if ( "plastic.hub.getRegisteredIds".equals( method ) ) {
            result = getRegisteredIds();
        }
        else if ( "plastic.hub.registerNoCallBack".equals( method ) ) {
            String name = (String) paramList.remove( 0 );
            result = registerNoCallBack( name ).toString();
        }
        else if ( "plastic.hub.registerXMLRPC".equals( method ) ) {
            String name = (String) paramList.remove( 0 );
            List supportedMessages = (List) paramList.remove( 0 );
            URL callBackUrl = new URL( (String) paramList.remove( 0 ) );
            result = registerXMLRPC( name, supportedMessages, callBackUrl )
                    .toString();
        }
        else if ( "plastic.hub.unregister".equals( method ) ) {
            URI id = new URI( (String) paramList.remove( 0 ) );
            unregister( id );
            result = null;
        }
        else {
            throw new UnsupportedOperationException( "No method " + method 
                                                   + " on hub" );
        }
        if ( result instanceof Throwable ) {
            Throwable e = (Throwable) result;
            throw (IOException) new IOException( e.getMessage() )
                 .initCause( e );
        }
        return result;
    }

    public Map request( URI sender, URI message, List args ) {
        return requestTo( sender, message, args,
                          (Agent[]) agentMap_.values()
                                             .toArray( new Agent[ 0 ] ) );
    }

    public Map requestToSubset( URI sender, URI message, List args,
                                List recipientIds ) {
        List agentList = new ArrayList();
        for ( Iterator it = recipientIds.iterator(); it.hasNext(); ) {
            Agent agent = (Agent) agentMap_.get( it.next() );
            if ( agent != null ) {
                agentList.add( agent );
            }
        }
        return requestTo( sender, message, args,
                          (Agent[]) agentList.toArray( new Agent[ 0 ] ) );
    }

    public void requestAsynch( URI sender, URI message, List args ) {
        requestAsynchTo( sender, message, args,
                         (Agent[]) agentMap_.values()
                                            .toArray( new Agent[ 0 ] ) );
    }

    public void requestToSubsetAsynch( URI sender, URI message, List args,
                                       List recipientIds ) {
        List agentList = new ArrayList();
        for ( Iterator it = recipientIds.iterator(); it.hasNext(); ) {
            Agent agent = (Agent) agentMap_.get( it.next() );
            if ( agent != null ) {
                agentList.add( agent );
            }
        }
        requestAsynchTo( sender, message, args,
                         (Agent[]) agentList.toArray( new Agent[ 0 ] ) );
    }

    /**
     * Performs the work of a synchronous request to a given list of agents.
     *
     * @param  sender  sender ID
     * @param  message  message ID
     * @param  args    message arguments
     * @param  agents  list of agents to which the request will be multiplexed
     */
    private Map requestTo( URI sender, URI message, List args,
                           Agent[] agents ) {

        /* Log request with unique ID. */
        final int reqId = ++nReq_;
        if ( verbose_ ) {
            out( "Synch request " + reqId );
            out( "    Sender:  " + stringify( sender ) );
            out( "    Message: " + message );
            if ( args.size() > 0 ) {
                out( "    Args:    " + stringify( args ) );
            }
        }

        /* Assemble a list of threads which will perform the synchronous
         * calls on each agent. */
        List threadList = new ArrayList();
        for ( int i = 0; i < agents.length; i++ ) {
            final Agent agent = agents[ i ];
            if ( agent.supportsMessage( message ) ) {
                threadList.add( new RequestThread( agent, sender,
                                                   message, args ) {
                    public void run() {
                        if ( verbose_ ) {
                            out( "        -> " + agent );
                        }
                        super.run();
                        if ( verbose_ ) {
                            String result;
                            try {
                                result = stringify( getResult() );
                            }
                            catch ( IOException e ) {
                                result = stringify( e );
                            }
                            out( "        <- " + agent + ": " + result );
                        }
                    }
                } );
            }
        }
        RequestThread[] threads =
            (RequestThread[]) threadList.toArray( new RequestThread[ 0 ] );

        /* Start all the threads off. */
        Map results = new HashMap();
        for ( int i = 0; i < threads.length; i++ ) {
            threads[ i ].start();
        }

        /* Wait for all the threads to finish and assemble the result map. */
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
        if ( verbose_ ) {
            out();
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
    private void requestAsynchTo( URI sender, URI message, List args,
                                  Agent[] agents ) {

        /* Log request with unique ID. */
        final int reqId = ++nReq_;
        if ( verbose_ ) {
            out( "Asynch request " + reqId );
            out( "    Sender:  " + stringify( sender ) );
            out( "    Message: " + message );
            if ( args.size() > 0 ) {
                out( "    Args:    " + stringify( args ) );
            }
            out();
        }

        /* Invoke message asynchronously on all listed agents. */
        for ( int i = 0; i < agents.length; i++ ) {
            Agent agent = agents[ i ];
            if ( agent.supportsMessage( message ) ) {
                if ( verbose_ ) {
                    out( "        -> " + agent );
                }
                try {
                    agent.requestAsynch( sender, message, args );
                }
                catch ( IOException e ) {
                }
            }
        }
    }

    /**
     * Sets a stream for this hub to perform logging to.
     * If <code>out</code> is null (the default), no logging is performed.
     *
     * @param  out  logging print stream
     */
    public void setLogStream( PrintStream out ) {
        verbose_ = out != null;
        out_ = out;
    }

    /**
     * Logs a line of output.
     *
     * @param   line   line to write
     */
    private void out( String line ) {
        out_.println( line );
    }

    /**
     * Logs an empty line.
     */
    private void out() {
        out_.println();
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
                requestAsynch( getHubId(), HUB_STOPPING, new ArrayList() );
            }
            catch ( Exception e ) {
            }
            try {
                servers_.stop();
            }
            catch ( Exception e ) {
            }
            if ( verbose_ ) {
                out( "Hub stopped." );
            }
        }
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
     * Utility method to construct a URI without pesky checked exceptions.
     *
     * @param  uri  URI text
     * @return  URI
     * @throws  IllegalArgumentException  if there's a problem
     */
    private static URI createURI( String uri ) {
        try {
            return new URI( uri );
        }
        catch ( URISyntaxException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad URI: " + uri )
                 .initCause( e );
        }
    }

    /**
     * Turns a value into a human-readable string.
     * This is used for serialising argument lists etc prior to writing
     * them to a logging stream.
     *
     * @param  value  value
     * @return  stringified form
     */
    private String stringify( Object value ) {
        if ( value == null ) {
            return "null";
        }
        else if ( value instanceof URI ) {
            Object agent = agentMap_.get( value );
            if ( agent instanceof Agent ) {
                return "id:" + agent.toString();
            }
            else if ( value.equals( hubId_ ) ) {
                return "id:" + "hub";
            }
            else {
                return value.toString();
            }
        }
        else if ( value instanceof Collection ) {
            Collection set = (Collection) value;
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( '(' );
            if ( ! set.isEmpty() ) {
                for ( Iterator it = set.iterator(); it.hasNext(); ) {
                    sbuf.append( ' ' );
                    sbuf.append( stringify( it.next() ) );
                    sbuf.append( it.hasNext() ? ',' : ' ' );
                }
            }
            sbuf.append( ')' );
            return sbuf.toString();
        }
        else if ( value instanceof Throwable ) {
            ((Throwable) value).printStackTrace();
            return value.toString();
        }
        else {
            String s = value == null ? "null" : value.toString();
            s = s.length() < 60 ? s : ( s.substring( 0, 57 ) + "..." );
            s = s.replaceAll( "\n", "\\n" );
            return s;
        }
    }

    /**
     * Creates and starts a PlasticHub running, optionally 
     * writing the config information into a given file and
     * logging output to a print stream.
     * The config file is usually 
     * {@link org.votech.plastic.PlasticHubListener#PLASTIC_CONFIG_FILENAME}
     * in the user's home directory.  This file will be deleted automatically
     * under normal circumstances.
     *
     * @param   configFile  file to write setup information to,
     *          if null no file is written
     * @param   out  logging output stream (may be null for no logging)
     */
    public static PlasticHubListener startHub( File configFile,
                                               PrintStream out )
            throws RemoteException, IOException {
        final ServerSet servers = new ServerSet( configFile );
        final PlasticHub hub = new PlasticHub( servers );
        hub.setLogStream( out );
        Runtime.getRuntime().addShutdownHook( new Thread() {
            public void run() {
                hub.stop();
            }
        } );
        return hub;
    }

    /**
     * Starts a hub.
     *
     * <h2>Flags</h2>
     * <dl>
     * <dt>-verbose</dt>
     * <dd>Causes verbose messages to be written to standard output 
     *     logging hub operations.</dd>
     * <dt>-help</dt>
     * <dd>Prints a help message and exits.</dd>
     * </dl>
     */
    public static void main( String[] args )
            throws RemoteException, IOException {
        String usage = "Usage: " + PlasticHub.class.getName() + ":" 
                     + " [-verbose]";
        PrintStream out = null;

        List argList = new ArrayList( Arrays.asList( args ) );
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.equals( "-verbose" ) ) {
                out = System.out;
                it.remove();
            }
            else if ( arg.startsWith( "-h" ) ) {
                System.out.println( usage );
                System.exit( 0 );
            }
        }
        if ( ! argList.isEmpty() ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        startHub( new File( System.getProperty( "user.home" ),
                            PlasticHubListener.PLASTIC_CONFIG_FILENAME ),
                  out );
    }
}
