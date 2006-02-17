package uk.ac.starlink.plastic;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.NotBoundException;
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
     * Returns the properties set which is stored in {@link #PLASTIC_FILE}.
     *
     * @return  plastic properties
     */
    public static Properties getPlasticProperties() throws IOException {
        File rvfile = new File( System.getProperty( "user.home" ),
                                PLASTIC_FILE );
        if ( rvfile.exists() ) {
            Properties props = new Properties();
            props.load( new BufferedInputStream(
                             new FileInputStream( rvfile ) ) );
            return props;
        }
        else {
            throw new FileNotFoundException( "No PLASTIC hub detected "
                                           + "(no file ~/.plastic" );
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
        int port = 3112 - 1;
        WebServer server = null;
        IOException error = null;
        for ( int i = 0; i < 20 && server == null; i++ ) {
            port++;
            try {
                server = new WebServer( port );
                server.start();
            }
            catch ( RuntimeException e ) {  // probably from a BindException
                server = null;
                error = (IOException) new IOException( e.getMessage() )
                                     .initCause( e );
            }
        }
        if ( server == null ) {
            throw error;
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
        server.addHandler( "plastic.client", handler );

        Vector argv = new Vector();
        argv.add( app.getName() );
        argv.add( XmlRpcAgent.doctorArgs( app.getSupportedMessages() ) );
        argv.add( XmlRpcAgent.doctorArgs( serverUrl.toString() ) );
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
}
