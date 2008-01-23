package uk.ac.starlink.plastic;

import java.awt.Component;
import java.awt.Graphics;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import net.ladypleaser.rmilite.Client;
import org.apache.xmlrpc.WebServer;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcClientLite;
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

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.plastic" );

    private static Icon sendIcon_;
    private static Icon broadcastIcon_;

    /**
     * True if spurious java.util.NoSuchElementExceptions should be flagged.
     * If future JVMs fix this bug, this should be set false or the code
     * removed.
     */
    public static boolean WARN_ABOUT_NOSUCHELEMENTEXCEPTIONS = true;

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
        return new URL( getPlasticProperty( PlasticHubListener
                                           .PLASTIC_XMLRPC_URL_KEY ) );
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
     * @return  connection corresponding to <code>app</code> registered
     *          with the hub
     * @throws  IOException if the application could not register
     */
    public static PlasticConnection registerRMI( final PlasticApplication app )
            throws IOException {
        final HubManager hubber = new HubManager( app.getName(),
                                                  app.getSupportedMessages() ) {
            public Object doPerform( URI sender, URI message, List args ) {
                return app.perform( sender, message, args );
            }
        };
        hubber.register();
        final URI id = hubber.getRegisteredId();
        return new PlasticConnection() {
            public URI getId() {
                return id;
            }
            public void unregister() {
                hubber.unregister();
            }
        };
    }

    /**
     * Registers a PlasticApplication with the current hub using XML-RPC type
     * communication.
     *
     * @param  app  application to register
     * @return  registered ID
     */
    public static PlasticConnection registerXMLRPC( final
                                                    PlasticApplication app )
            throws IOException {
        final XmlRpcClient client = createXmlRpcClient( getXmlRpcUrl() );
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

        URL serverUrl =
            new URL( "http://"
                   + InetAddress.getLocalHost().getCanonicalHostName()
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
                PlasticConnection conn = new PlasticConnection() {
                    private boolean unreg_;
                    public URI getId() {
                        return id;
                    }
                    public synchronized void unregister() {
                        if ( ! unreg_ ) {
                            unreg_ = true;
                            Vector argv = new Vector();
                            argv.add( id.toString() );
                            try {
                                client.execute( "plastic.hub.unregister",
                                                argv );
                            }
                            catch ( Throwable e ) {
                                logger_.info( "Unregister failed: " + e );
                            }
                        }
                    }
                    protected void finalize() throws Throwable {
                        try {
                            unregister();
                        }
                        finally {
                            super.finalize();
                        }
                    }
                };
                final Reference connRef = new WeakReference( conn );
                Runtime.getRuntime().addShutdownHook( new Thread() {
                    public void run() {
                        PlasticConnection conn =
                            (PlasticConnection) connRef.get();
                        if ( conn != null ) {
                            conn.unregister();
                        }
                    }
                } );
                return conn;
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
     * Gets the result of a request to a single registered application
     * without registering for callback.
     *
     * @param   name  application name
     * @param   message  message ID
     * @param   args   message arguments
     * @param   target   application ID of a registered application to which
     *          the request is to be made
     * @return  the response from the target application
     */
    public static Object targetRequest( String name, URI message, List args,
                                        URI target )
            throws IOException {
        PlasticHubListener hub = getLocalHub();
        URI id;
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
            return targetRequest( id, message, args, target );
        }
        finally {
            hub.unregister( id );
        }
    }

    /**
     * Gets the result of a request to a single registered application
     * using an existing connection to the hub.
     *
     * @param   sender  application ID of registered listener from which
     *          the request will be made
     * @param   message  message ID
     * @param   args   message arguments
     * @param   target  application ID of a registered application to which
     *          the request is to be made
     * @return  the response from the target application
     */
    public static Object targetRequest( URI sender, URI message, List args,
                                        URI target )
            throws IOException {
        PlasticHubListener hub = getLocalHub();
        Map resultMap =
            hub.requestToSubset( sender, message, args,
                                 Collections.singletonList( target ) );
        return resultMap.get( target );
    }

    /**
     * Sends a single asynchronous message to the hub
     * without registering for callback.
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
     *
     * @param  gui  true iff a window representing the hub is to be posted
     */
    public static void startExternalHub( boolean gui ) throws IOException {
        File javaHome = new File( System.getProperty( "java.home" ) );
        File javaExec = new File( new File( javaHome, "bin" ), "java" );
        String javacmd = ( javaExec.exists() && ! javaExec.isDirectory() )
                       ? javaExec.toString()
                       : "java";
        String[] args = new String[] {
            javacmd,
            "-classpath",
            System.getProperty( "java.class.path" ),
            PlasticHub.class.getName(),
            ( gui ? "-gui" : "-verbose" ),
        };
        StringBuffer cmdbuf = new StringBuffer();
        for ( int iarg = 0; iarg < args.length; iarg++ ) {
            if ( iarg > 0 ) {
                cmdbuf.append( ' ' );
            }
            cmdbuf.append( args[ iarg ] );
        }
        logger_.info( "Starting external hub" );
        logger_.info( cmdbuf.toString() );
        Runtime.getRuntime().exec( args );
    }

    /**
     * Blocks until a successful registration has been achieved.
     * The most common reason for registration to fail is that the
     * hub is not running.  This method is therefore suitable to run
     * (probably on a background thread) while waiting for a hub to 
     * start up.
     *
     * @param  hubMan  hub manager object
     * @param  interval  number of milliseconds between registration attempts
     * @throws  InterruptedException  if the thread is interrupted
     */
    public static void waitToRegister( HubManager hubMan, final int interval )
            throws InterruptedException {
        while ( ! Thread.currentThread().isInterrupted() ) {
            if ( isHubRunning() ) {
                try {
                    hubMan.register();
                    return;
                }
                catch ( IOException e ) {
                }
                catch ( RuntimeException e ) {
                    throw e;
                }
                catch ( Error e ) {
                    throw e;
                }
            }
            Thread.sleep( interval );
        }
        throw new InterruptedException();
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
     * Factory method for obtaining a new XmlRpcClient.
     * Consistent use of this permits switching between client implementations.
     *
     * @param  url  XML-RPC server URL
     * @return   new client for communication with <code>url</code>
     */
    public static XmlRpcClient createXmlRpcClient( URL url ) {
        // return new XmlRpcClientLite( url );
        return new XmlRpcClient( url );
        // return new XmlRpcClient( url, new CustomTransportFactory( url ) );
    }

    /**
     * Returns an icon which conveys the idea of point-to-point transmission.
     *
     * @return  send icon
     */
    public static Icon getSendIcon() {
        if ( sendIcon_ == null ) {
            sendIcon_ = createIcon( "send.gif" );
        }
        return sendIcon_;
    }

    /**
     * Returns an icon which conveys the idea of one-to-many transmission.
     *
     * @return  broadcast icon
     */
    public static Icon getBroadcastIcon() {
        if ( broadcastIcon_ == null ) {
            broadcastIcon_ = createIcon( "broadcast.gif" );
        }
        return broadcastIcon_;
    }

    /**
     * Constructs and returns an icon from the name of an image representing
     * a resource in this package.
     *
     * @param   imageName  name of image file in the same place as this class
     * @return  icon
     */
    private static Icon createIcon( String imageName ) {
        try {
            return new ImageIcon( PlasticUtils.class.getResource( imageName ) );
        }
        catch ( Exception e ) {
            return new Icon() {
                public int getIconHeight() {
                    return 24;
                }
                public int getIconWidth() {
                    return 24;
                }
                public void paintIcon( Component c, Graphics g, int x, int y ) {
                }
            };
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
                     * be written to System.err, at least at J2SE1.4.
                     * Not my fault! */
                    if ( WARN_ABOUT_NOSUCHELEMENTEXCEPTIONS ) {
                        WARN_ABOUT_NOSUCHELEMENTEXCEPTIONS = false;
                        System.err.println(
                            "Please ignore spurious \"" 
                          + "java.util.NoSuchElementException\" messages." );
                    }
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
