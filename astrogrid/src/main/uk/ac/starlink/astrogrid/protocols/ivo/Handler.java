package uk.ac.starlink.astrogrid.protocols.ivo;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import uk.ac.starlink.astrogrid.AcrConnection;
import uk.ac.starlink.astrogrid.AcrConnector;

/**
 * Protocol handler for "<code>ivo:</code>" type URLs.
 * This currently interprets them in the way they are used by AstroGrid's
 * MySpace (as per ACR v2006.3).  They look like this:
 * <pre>
 *    ivo://uk.ac.le.star/filemanager#node-2201/demo/messier.vot
 * </pre>
 *
 * <p>Both input and output (but not from the same connection)
 * are supported.
 *
 * @author   Mark Taylor
 * @since    25 Aug 2006
 * @see      java.net.URL#URL(java.lang.String,java.lang.String,int,java.lang.String)
 * @see      uk.ac.starlink.util.URLUtils#installCustomHandlers
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
        return new IvoURLConnection( getConnection(), url );
    }

    protected void parseURL( URL url, String spec, int start, int limit ) {

        /* It's necessary to doctor the URL to make sense of URLs as used
         * by the MySpace system.  The reason is that MySpace uses the 
         * ref part (after the "#") to state the file path, and this
         * violates rules about what URLs are supposed to do.
         * So for the purposes of the configured URL object we have to
         * move the content of the ref part into the path.
         * This might give applications using these URLs a shock, but
         * until MySpace URLs are fixed to use refs properly this is
         * making the best of a bad job. */
        super.parseURL( url, spec, start, limit );
        String us = url.toString();
        assert us.equals( url.toString() );
        String protocol = url.getProtocol();
        String host = url.getHost();
        int port = url.getPort();
        String authority = url.getAuthority();
        String userInfo = url.getUserInfo();
        String path = url.getPath();
        String query = url.getQuery();
        String ref = url.getRef();
        if ( ref != null && ref.length() > 0 ) {
            path = path + "#" + ref;
            ref = null;
        }
        setURL( url, protocol, host, port, authority, userInfo, path,
                query, ref );
        assert us.equals( url.toString() );
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
