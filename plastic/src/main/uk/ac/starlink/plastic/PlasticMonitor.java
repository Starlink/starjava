package uk.ac.starlink.plastic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.apache.xmlrpc.WebServer;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;

/**
 * Watches and reports on messages sent over a PLASTIC message bus.
 * Designed principally to aid with debugging, both of PLASTIC infrastructure
 * and of PLASTIC-aware applications.
 *
 * <p>This class is intended to be used standalone from its {@link #main}
 * method.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2006
 */
public abstract class PlasticMonitor {

    private final String name_;
    private boolean stopped_;

    /**
     * Constructor.
     *
     * @param  name   implementation-specific label
     */
    private PlasticMonitor( String name ) {
        name_ = name;
    }

    /**
     * Performs registration with the hub.
     *
     * @param  appName  application name to send to the hub
     * @param  supportedMessages   list of supported messages
     */
    abstract URI register( String appName, URI[] supportedMessages )
        throws IOException;

    /**
     * Performs logging of a PLASTIC execution request with given arguments.
     * The HUB_STOPPING message is treated specially: 
     * {@link #stopped_} is set and <code>notifyAll</code> is called on 
     * this object.
     *
     * @param  sender  sender ID
     * @param  message  message ID
     * @parm   args   message argument list
     */
    void logCall( URI sender, URI message, List args ) {
        String summary = new StringBuffer()
            .append( stringify( sender ) )
            .append( ": " )
            .append( stringify( message ) )
            .append( stringify( args ) )
            .toString();
        System.out.println( summary );
        if ( PlasticHub.HUB_STOPPING.equals( message ) ) {
            stopped_ = true;
            synchronized ( this ) {
                notifyAll();
            }
        }
    }

    /**
     * Stringifies an object for logging purposes.
     *
     * @param  value  object to stringify
     * @return  human-readable version of value
     */
    private String stringify( Object value ) {
        if ( value == null ) {
            return "null";
        }
        else if ( value instanceof URI ) {
            return value.toString();
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
        else {
            String s = value == null ? "null" : value.toString();
            s = s.length() < 40 ? s : ( s.substring( 0, 37 ) + "..." );
            s = s.replaceAll( "\n", "\\n" );
            return s;
        }
    }

    /**
     * RMI implementation of PlasticMonitor.
     */
    private static class RmiMonitor extends PlasticMonitor {

         public RmiMonitor() {
             super( "RMI" );
         }

         public URI register( String appName, URI[] supportedMessages )
                 throws IOException {
             HubManager hubber = new HubManager( appName, supportedMessages ) {
                 public Object doPerform( URI sender, URI message, List args ) {
                     logCall( sender, message, args );
                     return null;
                 }
             };
             hubber.register();
             return hubber.getRegisteredId();
         }
    }

    /**
     * XML-RPC implementation of PlasticMontor.
     */
    private static class XmlRpcMonitor extends PlasticMonitor
                                       implements XmlRpcHandler {

         public XmlRpcMonitor() {
             super( "XML-RPC" );
         }

         public URI register( String appName, URI[] supportedMessages )
                 throws IOException {

             final XmlRpcClient client =
                 new XmlRpcClient( PlasticUtils.getXmlRpcUrl() );

             int port = 2112 - 1;
             WebServer server = null;
             RuntimeException error = null;
             for ( int i = 0; i < 20 && server == null; i++ ) {
                 port++;
                 try {
                     server = new WebServer( port );
                     server.start();
                 }
                 catch ( RuntimeException e ) {
                     server = null;
                     error = e;
                 }
             }
             if ( server == null ) {
                 throw error;
             }

             URL serverUrl =
                 new URL( "http://" + InetAddress.getLocalHost().getHostName()
                        + ":" + port + "/" );
             server.addHandler( "plastic.client", (XmlRpcHandler) this );

             Vector argv = new Vector();
             argv.add( appName );
             argv.add( supportedMessages );
             argv.add( serverUrl.toString() );
             try {
                 final Object result =
                     client.execute( "plastic.hub.registerXMLRPC", argv );
                 if ( result instanceof XmlRpcException ) {
                     throw (XmlRpcException) result;
                 }
                 Runtime.getRuntime().addShutdownHook( new Thread() {
                     public void run() {
                         Vector argv = new Vector();
                         argv.add( result );
                         try {
                             client.execute( "plastic.hub.unregister", argv );
                         }
                         catch ( Throwable e ) {
                         }
                     }
                 } );
                 return new URI( (String) result );
             }
             catch ( XmlRpcException e ) {
                 throw (IOException) new IOException( e.getMessage() )
                                    .initCause( e );
             }
             catch ( URISyntaxException e ) {
                 throw (IOException) new IOException( e.getMessage() )
                                    .initCause( e );
             }
         }

         public Object execute( String method, Vector params )
                 throws URISyntaxException {
             List paramList = new ArrayList( params );
             if ( "plastic.client.perform".equals( method ) ) {
                 URI sender = new URI( (String) paramList.remove( 0 ) );
                 URI message = new URI( (String) paramList.remove( 0 ) );
                 List args = new ArrayList( (Collection)
                                            paramList.remove( 0 ) );
                 logCall( sender, message, args );
                 return null;
             }
             else {
                 throw new UnsupportedOperationException( method );
             }
         }
    }

    /**
     * Starts a monitor of the PLASTIC message bus which logs message
     * descriptions to standard output.
     * A choice of Java-RMI and XML-RPC communication is offered.
     *
     * <h2>Flags</h2>
     * <dl>
     * <dt>-rmi</dt>
     * <dd>Use Java-RMI for communications (default)</dd>
     * <dt>-xmlrpc</dt>
     * <dd>Use XML-RPC for communications</dd>
     * </dl>
     */
    public static void main( String[] args ) throws IOException {
        String usage = "Usage: " + PlasticMonitor.class.getClass().getName() 
                     + " [-xmlrpc|-rmi]";
        PlasticMonitor mon = null;

        /* Process flags. */
        List argv = new ArrayList( Arrays.asList( args ) );
        for ( Iterator it = argv.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( "-xmlrpc".equals( arg ) ) {
                it.remove();
                mon = new XmlRpcMonitor();
            }
            else if ( "-rmi".equals( arg ) ) {
                it.remove();
                mon = new RmiMonitor();
            }
            else if ( arg.startsWith( "-h" ) ) {
                System.out.println( usage );
                System.exit( 0 );
            }
        }
        if ( ! argv.isEmpty() ) {
            System.err.println( usage );
            System.exit( 1 );
        }

        /* Select default monitor implementation if none has been specified
         * explicitly. */
        if ( mon == null ) {
            mon = new RmiMonitor();
        }

        /* Register the monitor with the hub. */
        URI id = mon.register( "monitor", new URI[ 0 ] );

        /* Wait on the monitor.  If it receives a HUB_STOPPING message
         * it will be notified and execution of this method can complete. */
        try {
            synchronized ( mon ) {
                while ( ! mon.stopped_ ) {
                    mon.wait();
                }
            }
            System.exit( 0 );
        }
        catch ( InterruptedException e ) {
        }
    }
}
