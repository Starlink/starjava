package uk.ac.starlink.plastic;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.util.Properties;
import net.ladypleaser.rmilite.Client;
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
}
