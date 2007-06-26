package uk.ac.starlink.plastic;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.logging.Logger;
import net.ladypleaser.rmilite.Client;
import net.ladypleaser.rmilite.Server;
import org.apache.xmlrpc.WebServer;
import org.votech.plastic.PlasticListener;
import org.votech.plastic.PlasticHubListener;

/**
 * Provides the servers (XML-RPC and RMI-LITE) which are required for
 * a functioning hub.
 *
 * @author   Mark Taylor
 * @since    16 Feb 2006
 */
public class ServerSet {

    private final Server rmiServer_;
    private final int rmiPort_;
    private final WebServer xmlrpcServer_;
    private final URL xmlrpcUrl_;
    private final File configFile_;
    private final String serverIdValue_;
    private final static String SERVER_ID_KEY = "uk.ac.starlink.plastic.servid";
    private boolean stopped_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.plastic" );

    /**
     * Constructs a new ServerSet.  If a <code>configFile</code> is specified
     * it will be used to record details of the current configuration,
     * in the standard PLASTIC way (serialised properties).
     * If it's null, this information will not be recorded.
     *
     * @param   configFile  file for storing configuration of the running
     *          PLASTIC hub 
     */
    public ServerSet( File configFile ) throws IOException, RemoteException {
        configFile_ = configFile;
        serverIdValue_ = this.toString();

        /* Check if the config file already exists. */
        if ( configFile != null && configFile.exists() ) {

            /* Load config file. */
            Properties props = new Properties();
            try {
                InputStream propStream =
                    new BufferedInputStream(
                        new FileInputStream( configFile ) );
                props.load( propStream );
                propStream.close();
            }
            catch ( IOException e ) {
                throw (IOException)
                      new IOException( "File " + configFile + "exists " 
                                     + "but is unreadable" )
                     .initCause( e );
            }
            String rmiPort = props.getProperty( PlasticHubListener
                                               .PLASTIC_RMI_PORT_KEY );

            /* Examine it to see if it describes a living or dead hub. */
            boolean badFile = false;
            if ( rmiPort != null ) {
                try {
                    Client client = 
                        new Client( "localhost",
                                     Integer.parseInt( rmiPort ) );
                    client.exportInterface( PlasticListener.class );
                    PlasticHubListener hub = (PlasticHubListener)
                       client.lookup( PlasticHubListener.class );
                }
                catch ( NumberFormatException e ) {
                    badFile = true;
                }
                catch ( Exception e ) {
                    if ( configFile_.delete() ) {
                        logger_.warning( "Apparently moribund " + configFile +
                                         " - deleting" );
                    }
                    else {
                        throw new IOException( "Tried and failed to delete "
                                             + "apparently moribund "
                                             + configFile ); 
                    }
                }
            }
            else {
                badFile = true;
            }

            if ( configFile.exists() ) {
                String msg = badFile
                    ? "File " + configFile + " exists but looks wrong" +
                      " - delete it?"
                    : "Hub described at " + configFile + " is already running";
                throw new IOException( msg );
            }
        }

        /* Start an RMI server on a suitable port. */
        int rmiPort = Server.DEFAULT_PORT - 1;
        Server rmiServer = null;
        RemoteException rmiBindError = null;
        for ( int i = 0; i < 20 && rmiServer == null; i++ ) {
            rmiPort++;
            try {
                rmiServer = new Server( rmiPort );
            }
            catch ( RemoteException e ) {
                rmiBindError = e;
            }
        }
        if ( rmiServer == null ) {
            throw rmiBindError;
        }
        rmiPort_ = rmiPort;
        rmiServer_ = rmiServer;

        /* Start an XML-RPC server on a suitable port. */
        int xrPort = PlasticUtils.getUnusedPort( 2112 );
        WebServer xrServer = null;
        try {
            xrServer = new WebServer( xrPort );
            xrServer.start();
        }
        catch ( RuntimeException e ) {
            throw new RemoteException( "Can't start XML-RPC server", e );
        }
        xmlrpcServer_ = xrServer;
        xmlrpcUrl_ =
            new URL( "http://" +
                     InetAddress.getLocalHost().getCanonicalHostName() +
                     ":" + xrPort + "/" );

        /* Write the config file if requested to. */
        if ( configFile != null ) {
            storeConfig( configFile );
        }

        /* Arrange to delete the config file on shutdown. */
        Runtime.getRuntime().addShutdownHook( new Thread() {
            public void run() {
                ServerSet.this.stop();
            }
        } );
    }

    /**
     * Returns a running RMI server associated with this object.
     *
     * @return   RMI server
     */
    public Server getRmiServer() {
        return rmiServer_;
    }

    /**
     * Returns a running XML-RPC server associated with this object.
     *
     * @return  XML-RPC server
     */
    public WebServer getXmlRpcServer() {
        return xmlrpcServer_;
    }

    /**
     * Returns the URL for the XML-RPC server associated with this object.
     *
     * @return  XML-RPC URL
     */
    public URL getXmlRpcUrl() {
        return xmlrpcUrl_;
    }

    /**
     * Writes the relevant information about this object to a PLASTIC-format
     * properties file.
     *
     * @param  configFile  output file
     */
    private void storeConfig( File configFile ) throws IOException {
        Properties props = new Properties();
        props.setProperty( PlasticHubListener.PLASTIC_VERSION_KEY,
                           PlasticListener.CURRENT_VERSION );
        props.setProperty( PlasticHubListener.PLASTIC_RMI_PORT_KEY,
                           Integer.toString( rmiPort_ ) );
        props.setProperty( PlasticHubListener.PLASTIC_XMLRPC_URL_KEY, 
                           xmlrpcUrl_.toString() );
        props.setProperty( SERVER_ID_KEY, serverIdValue_ );
        OutputStream out = new FileOutputStream( configFile );
        try {
            props.store( out, "PLASTIC server " + this.getClass().getName() );
        }
        finally {
            out.close();
        }
    }

    /**
     * Tidies up when the servers provided by this object are no
     * longer requires.  This includes shutting down the servers and
     * deleting the config file, as long as it's the one we wrote.
     * May safely be called multiple times.
     */
    public void stop() {
        boolean stop;
        synchronized ( this ) {
            stop = ! stopped_;
            stopped_ = true;
        }
        if ( stop ) {
            try {
                if ( configFile_ != null && configFile_.exists() ) {
                    Properties props = new Properties();
                    InputStream configStream =
                        new FileInputStream( configFile_ );
                    try {
                        props.load( configStream );
                    }
                    finally {
                        configStream.close();
                    }
                    if ( serverIdValue_
                        .equals( props.getProperty( SERVER_ID_KEY ) ) ) {
                        configFile_.delete();
                    }
                }
                xmlrpcServer_.shutdown();
                // Should shut down RMI server here, but no method exists.
            }
            catch ( IOException e ) {
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
}
