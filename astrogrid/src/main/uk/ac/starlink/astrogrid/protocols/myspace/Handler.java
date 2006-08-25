package uk.ac.starlink.astrogrid.protocols.myspace;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import uk.ac.starlink.astrogrid.AcrConnection;
import uk.ac.starlink.astrogrid.AcrConnector;
import uk.ac.starlink.astrogrid.protocols.ivo.IvoURLConnection;

/**
 * Protocol handler for <code>myspace</code>-protocol URLs.
 * The format is as for Java <code>file</code>-protocol URLs,
 * except that all paths are absolute.
 * Like the file protocol handler, the host part is effectively
 * ignored and may be absent (along with its slashes).
 * So either of 
 * <pre>
 *     myspace:/demo/messier.xml
 *     myspace://localhost/demo/messer.xml
 * </pre>
 * would references a file "messier.xml" in the "demo" directory 
 * under the home directory of the currently logged in user.  
 * Using ACR magic, if you're not logged in, a GUI popup window 
 * will prompt you to do so during resolution of such URLs.
 * 
 * <p>Both input and output (but not from the same connection)
 * are supported.
 *
 * @author   Mark Taylor
 * @since    25 Aug 2006
 */
public class Handler extends URLStreamHandler {

    private final AcrConnector connector_;
    private AcrConnection connection_;

    /**
     * No-arg constructor as required.
     */
    public Handler() {
        super();
        connector_ = new AcrConnector();
    }

    protected URLConnection openConnection( URL url ) throws IOException {

        /* Translate the myspace-protocol URL into an equivalent 
         * ivo-protocol one.  We need the ivo-protocol URI of the user's
         * home directory to do that. */
        AcrConnection aconn = getConnection();
        String home = aconn.getHome();
        URL u2 = new URL( home + url.getPath() );

        /* The ivo protocol handler does the rest of the work. */
        return new IvoURLConnection( getConnection(), u2 );
    }

    /**
     * Returns a connection that can be used to talk to the ACR server.
     *
     * @return   live ACR connection
     */
    private AcrConnection getConnection() throws IOException {
        if ( connection_ == null || ! connection_.isConnected() ) {
            connection_ = (AcrConnection) connector_.logIn();
        }
        return connection_;
    }
}
