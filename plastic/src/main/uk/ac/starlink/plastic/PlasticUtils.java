package uk.ac.starlink.plastic;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import net.ladypleaser.rmilite.Client;
import org.apache.xmlrpc.WebServer;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.votech.plastic.PlasticListener;
import org.votech.plastic.PlasticHubListener;

/**
 * Utility methods for use with the PLASTIC tool interop protocol.
 *
 * @author   Mark Taylor
 * @since    8 Feb 2006
 * @see      <a href="http://plastic.sourceforge.net/">PLASTIC</a>
 */
public class PlasticUtils {

    /** Location in the user's home directory of the PLASTIC rendezvous file. */
    public static final String PLASTIC_FILE = ".plastic";

    /** Version of PLASTIC interface implemented. */
    public static final String PLASTIC_VERSION =
        PlasticListener.CURRENT_VERSION;

    /** Prefix for XML-RPC "perform" method. */
    public static final String XMLRPC_PREFIX = 
        Double.parseDouble( PLASTIC_VERSION ) > 0.399 ? null
                                                      : "plastic.client";

    /**
     * Private sole constructor blocks instantiation.
     */
    private PlasticUtils() {
    }

    /**
     * Returns the PLASTIC hub running on the local host.
     * If it can't be done (for instance because no hub is running)
     * a more or less informative IOException will be thrown.
     *
     * @return   local PLASTIC hub
     */
    public static PlasticHubListener getLocalHub() throws IOException {
        try {
            return (PlasticHubListener)
                   getLocalClient().lookup( PlasticHubListener.class );
        }
        catch ( NotBoundException e ) {
            throw (IOException)
                  new IOException( "Error connecting to PLASTIC hub" )
                 .initCause( e );
        }
        catch ( SocketException e ) {
            throw (IOException)
                  new IOException( "No connection to PLASTIC hub "
                                 + "(not running?)" )
                 .initCause( e );
        }
    }

    /**
     * Returns the URL to use for the hub's XML-RPC server.
     *
     * @return  URL for communicating with the current hub via XML-RPC
     */
    public static URL getXmlRpcUrl() throws IOException {
        return new URL( getPlasticProperty( "plastic.xmlrpc.url" ) );
    }

    /**
     * Returns an RMI-Lite client associated with PLASTIC services on the
     * local host.  The client is configured to <em>export</em> the
     * <code>org.votech.plastic.PlasticListener</code> interface,
     * which means that you can use PlasticListener implementations which
     * don't have to be Serializable.
     *
     * <p>If no hub can be obtained, for instance because no hub is running,
     * a more or less informative IOException will be thrown.
     *
     * @return   PLASTIC client
     * @see      <a href="http://rmi-lite.sourceforge.net">RMI-Lite</a>
     */
    private static Client getLocalClient() throws IOException {

        /* Create a new client.  I considered doing this lazily, but it's 
         * probably not expensive, I don't think there's any objection to
         * having multiple live clients of the same server in the same JVM,
         * and like this you have a better chance of getting a client
         * that hasn't expired. */
        Client localClient = new Client( "localhost", getPlasticPort() );

        /* This bit of RMI-Lite magic ensures that you can use PlasticListener
         * implementations which are not Serializable (RMI-Lite passes
         * stubs around instead). */
        localClient.exportInterface( PlasticListener.class );

        /* Return. */
        return localClient;
    }

    /**
     * Attempts to determine if a plastic hub is currently running.
     * The results are not completely guaranteed to be true.
     *
     * @return  true  if a hub appears to be running, false if it doesn't
     */
    public static boolean isHubRunning() {
        try {
            getPlasticProperties();
            return true;
        }
        catch ( Throwable e ) {
            return false;
        }
    }

    /**
     * Returns the properties set which is stored in {@link #PLASTIC_FILE}.
     *
     * @return  plastic properties
     */
    public static Properties getPlasticProperties() throws IOException {
        File rvfile = new File( System.getProperty( "user.home" ),
                                PLASTIC_FILE );
        if ( rvfile.exists() ) {
            Properties props = new Properties();
            InputStream propStream =
                new BufferedInputStream( new FileInputStream( rvfile ) );
            try {
                props.load( propStream );
            }
            finally {
                propStream.close();
            }
            return props;
        }
        else {
            throw new NoHubException( "No PLASTIC hub detected "
                                    + "(no file ~/.plastic)" );
        }
    }

    /**
     * Returns the value of a given property defined in the 
     * {@link #PLASTIC_FILE} file.  If it does not exist or has no value
     * an IOException is thrown.  Null will not be returned.
     *
     * @param  key   property key
     * @return  value  property value
     */
    public static String getPlasticProperty( String key ) throws IOException {
        String value = getPlasticProperties().getProperty( key );
        if ( value == null ) {
            throw new IOException( "No property " + key + " in file " +
                                   new File( System.getProperty( "user.home" ),
                                             PLASTIC_FILE ) );
        }
        else {
            return value;
        }
    }

    /**
     * Returns the port number on which the PLASTIC hub is running an RMI
     * server.  Throws a more or less informative IOException if it can't
     * be done.
     *
     * @return  port number on local host of PLASTIC RMI server
     */
    private static int getPlasticPort() throws IOException {
        String sport = getPlasticProperty( "plastic.rmi.port" );
        try {
            return Integer.parseInt( sport );
        }
        catch ( NumberFormatException e ) {
            throw (IOException)
                  new IOException( "Bad .plastic file: " 
                                 + "plastic.rmi.port=" + sport )
                 .initCause( e );
        }
    }

    /**
     * Registers a PlasticApplication with the current hub using RMI-type
     * communication.
     *
     * @param   app  application to register
     * @return  registered ID
     */
    public static URI registerRMI( final PlasticApplication app )
            throws IOException {
        HubManager hubber = new HubManager( app.getName(),
                                            app.getSupportedMessages() ) {
            public Object doPerform( URI sender, URI message, List args ) {
                return app.perform( sender, message, args );
            }
        };
        hubber.register();
        return hubber.getRegisteredId();
    }

    /**
     * Registers a PlasticApplication with the current hub using XML-RPC type
     * communication.
     *
     * @param  app  application to register
     * @return  registered ID
     */
    public static URI registerXMLRPC( final PlasticApplication app )
            throws IOException {
        final XmlRpcClient client = new XmlRpcClient( getXmlRpcUrl() );
        final WebServer server;
        int port = getUnusedPort( 3112 );
        try {
            server = new WebServer( port );
            server.start();
        }
        catch ( RuntimeException e ) {  // probably from a BindException
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }

        URL serverUrl = new URL( "http://"
                               + InetAddress.getLocalHost().getHostName()
                               + ":" + port + "/" );
        XmlRpcHandler handler = new XmlRpcHandler() {
            public Object execute( String method, Vector params ) 
                    throws URISyntaxException {
                URI sender = new URI( (String) params.get( 0 ) );
                URI message = new URI( (String) params.get( 1 ) );
                List args = (List) params.get( 2 );
                return app.perform( sender, message, args );
            } 
        };
        server.addHandler( XMLRPC_PREFIX == null ? "$default" : XMLRPC_PREFIX,
                           handler );

        Vector argv = new Vector();
        argv.add( app.getName() );
        argv.add( XmlRpcAgent.doctorObject( app.getSupportedMessages() ) );
        argv.add( XmlRpcAgent.doctorObject( serverUrl ) );
        Object result;
        try {
            result = client.execute( "plastic.hub.registerXMLRPC", argv );
            if ( result instanceof XmlRpcException ) {
                throw (XmlRpcException) result;
            }
            else if ( result instanceof String ) {
                final URI id = new URI( (String) result );
                Runtime.getRuntime().addShutdownHook( new Thread() {
                    public void run() {
                        Vector argv = new Vector();
                        argv.add( id.toString() );
                        try {
                            client.execute( "plastic.hub.unregister", argv );
                        }
                        catch ( Throwable e ) {
                        }
                    }
                } );
                return id;
            }
            else {
                return null; // only happens if hub returns a bad ID
            }
        }
        catch ( XmlRpcException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
        catch ( URISyntaxException e ) {
            return null;  // only happens if hub returns a bad ID
        }
    }

    /**
     * Sends a single asynchronous message to the hub without registering
     * for callback.
     *
     * @param   name  application name
     * @param   message  message ID
     * @param   args   message arguments
     */
    public static void singleRequestAsynch( String name, URI message,
                                            List args ) 
            throws IOException {
        PlasticHubListener hub = getLocalHub();
        URI id = null;
        try {
            id = hub.registerNoCallBack( name );
        }
        catch ( Throwable e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
        if ( id == null ) {
            throw new IOException( "Can't connect to hub" );
        }
        try {
            hub.requestAsynch( id, message, args );
        }
        finally {
            hub.unregister( id );
        }
    }

    /**
     * Attempts to start a hub running in an external process.
     * There's no guarantee that this will be successful.
     */
    public static void startExternalHub() throws IOException {
        String[] args = new String[] {
            "java",
            "-classpath",
            System.getProperty( "java.class.path" ),
            PlasticHub.class.getName(),
            "-verbose",
        };
        Runtime.getRuntime().exec( args );
    }

    /**
     * Returns an array of ApplicationItem objects representing the 
     * applications currently registered with a given hub.
     * 
     * @param   hub  the hub
     * @return   apps registered with <code>hub</code>
     */
    public static ApplicationItem[]
                  getRegisteredApplications( PlasticHubListener hub ) {
        List ids = hub.getRegisteredIds();
        List apps = new ArrayList();
        for ( Iterator it = hub.getRegisteredIds().iterator(); it.hasNext(); ) {
            String idString = it.next().toString();
            try {
                URI id = new URI( idString );
                String name = hub.getName( id );
                List msgs = hub.getUnderstoodMessages( id );
                apps.add( new ApplicationItem( id, name, msgs ) );
            }
            catch ( URISyntaxException e ) {
            }
        }
        return (ApplicationItem[]) apps.toArray( new ApplicationItem[ 0 ] );
    }

    /**
     * Convenience method to turn a String into a URI without throwing
     * any pesky checked exceptions.
     * 
     * @param  uri  URI text
     * @return  URI
     * @throws  IllegalArgumentException   if uri doesn't look like a URI
     */
    public static URI createURI( String uri ) {
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
     * Returns an unused port number on the local host.
     *
     * @param   startPort  suggested port number; ports nearby will be
     *          chosen if this is in use
     */
    static int getUnusedPort( int startPort ) throws IOException {
        final int nTry = 20;
        for ( int iPort = startPort; iPort < startPort + nTry; iPort++ ) {
            try {
                Socket trySocket = new Socket( "localhost", iPort );
                if ( ! trySocket.isClosed() ) {

                    /* This line causes "java.util.NoSuchElementException" to
                     * be written to standard error, at least at J2SE1.4.
                     * Not my fault! */
                    trySocket.close();
                }
            }
            catch ( ConnectException e ) {

                /* Can't connect - this hopefully means that the socket is
                 * unused. */
                return iPort;
            }
        }
        throw new IOException( "Can't locate an unused port in range " + 
                               startPort + " ... " + ( startPort + nTry ) );
    }
}
