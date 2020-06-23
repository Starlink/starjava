package uk.ac.starlink.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.auth.UrlConnector;
import uk.ac.starlink.util.CountInputStream;

/**
 * Defines a policy for Content Codings used in HTTP connections.
 * An instance of this class is used to prepare requests and decode responses
 * for some HTTP-based communications in this package.
 * It controls whether HTTP-level content codings are used;
 * typically this means transparent gzip compression of the HTTP
 * response stream where negotiation indicates it is allowed.
 *
 * <p>Two static instances {@link #NONE} and {@link #GZIP} are provided.
 * It should be completely safe to use either of these in any context,
 * since an instance of this class represents only an indication to an
 * HTTP server that a particular coding scheme is supported by the client.
 * A service is therefore always at liberty to ignore this hint/request
 * and provide and unencoded response if, for instance, it does not support
 * the requested compression scheme.  Good practice for use of this class
 * is therefore probably to use GZIP where the response is expected
 * to be large and reasonably compressible (a long VOTable is a good
 * example), and NONE where the response is expected to be short or,
 * especially, not very gzippable (for instance noisy binary floating
 * point data, or a byte stream that has already been compressed).
 *
 * <p>The provided instances also include some logging functionality;
 * information about how many bytes (and where applicable the level of
 * compression) is logged for bytestreams read through these instances.
 * The logging level for this information is currently CONFIG.
 * 
 *
 * @see  <a href="http://www.w3.org/Protocols/rfc2616/rfc2616.html"
 *          >RFC 2616 secs 3.5, 14.3, 14.11</a>
 */
public abstract class ContentCoding implements UrlConnector {

    /** No encoding is requested. */
    public static final ContentCoding NONE =
        createCoding( "NONE", false, Level.CONFIG );

    /** Gzip encoding is requested. */
    public static final ContentCoding GZIP =
        createCoding( "GZIP", true, Level.CONFIG );

    /** Name of HTTP request header to request coded response ({@value}). */
    public static final String ACCEPT_ENCODING = "Accept-Encoding";

    /** Name of HTTP response header to mark coded response ({@value}). */
    public static final String CONTENT_ENCODING = "Content-Encoding";

    /** Name of HTTP content-coding token indicating GZIP ({@value}). */
    private static final String GZIP_NAME = "gzip";

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.util" );

    /**
     * Constructor.
     */
    protected ContentCoding() {
    }

    /**
     * Sets up request headers for the given connection.
     * The connection must not have yet been connected.
     *
     * @param   conn  unconnected connection
     */
    public abstract void prepareRequest( URLConnection conn );

    /**
     * Returns the input stream response from the given connection,
     * which was prepared using this object's <code>prepareRequest</code>
     * method.  Any required decoding will have been done transparently.
     *
     * @param  conn  connection
     * @return   stream decoded as required
     * @see   java.net.URLConnection#getInputStream
     */
    public abstract InputStream getInputStream( URLConnection conn )
            throws IOException;

    /**
     * Returns the error stream response from the given connection,
     * which was prepared using this object's <code>prepareRequest</code>
     * method.  Any required decoding will have been done transparently.
     *
     * @param  conn  connection
     * @return   stream decoded as required
     * @see   java.net.URLConnection#getInputStream
     */
    public abstract InputStream getErrorStream( URLConnection conn )
            throws IOException;

    /**
     * Convenience method to open a new connection prepared in accordance
     * with this object's encoding policy.
     * Opens the connection and calls <code>prepareRequest</code>.
     *
     * @param   url  target URL
     * @return   prepared connection
     */
    public URLConnection openConnection( URL url ) throws IOException {
        URLConnection conn = url.openConnection();
        prepareRequest( conn );
        return conn;
    }

    /**
     * Convenience method to return a byte stream from a given URL
     * in accordance with this object's encoding policy.
     * Opens the connection, prepares the request, and decodes the result.
     *
     * @param  url  target URL
     * @return  unencoded stream from the URL
     */
    public InputStream openStream( URL url ) throws IOException {
        URLConnection conn = url.openConnection();
        prepareRequest( conn );
        return getInputStream( conn );
    }

    /**
     * Convenience method to return a byte stream from a given URL
     * in accordance with this object's encoding policy and with
     * authentication and redirects handled by a given AuthManager
     * (typically {@link uk.ac.starlink.auth.AuthManager#getInstance}).
     *
     * @param  url  target URL
     * @param  authManager  authentication manager
     * @return  unencoded stream from the URL
     */
    public InputStream openStreamAuth( URL url, AuthManager authManager )
            throws IOException {
        return getInputStream( authManager.connect( url, this ) );
    }

    public void connect( HttpURLConnection hconn ) throws IOException {
        prepareRequest( hconn );
        hconn.setInstanceFollowRedirects( false );
        hconn.connect();
    }

    /**
     * Creates a ContentCoding instance that optionally requests compression.
     *
     * @param   name   user-readable name of the coding instance
     * @param   useGzip  if true, requests gzip encoding
     * @param   countLevel  logging level for byte count information
     * @return   new ContentCoding
     */
    private static ContentCoding createCoding( final String name,
                                               final boolean useGzip,
                                               final Level countLevel ) {
        return new ContentCoding() {
            public void prepareRequest( URLConnection conn ) {
                if ( conn instanceof HttpURLConnection && useGzip ) {
                    ((HttpURLConnection) conn)
                        .setRequestProperty( ACCEPT_ENCODING, GZIP_NAME );
                }
            }
    
            public InputStream getInputStream( URLConnection conn )
                    throws IOException {
                InputStream in = conn.getInputStream();
                final boolean isCounting = logger_.isLoggable( countLevel );
                final boolean isGzip = isGzip( conn );
                if ( isCounting ) {
                    in = new CountInputStream( in ) {
                        @Override
                        public void close() throws IOException {
                            super.close();
                            if ( ! isGzip ) {
                                logger_.log( countLevel,
                                             "Bytes read: " + getReadCount() );
                            }
                        }
                    };
                }
                final CountInputStream rawIn =
                    isCounting ? (CountInputStream) in : null;
                if ( isGzip ) {
                    in = Compression.GZIP.decompress( in );
                    if ( isCounting ) {
                        in = new CountInputStream( in ) {
                            @Override
                            public void close() throws IOException {
                                super.close();
                                long rawCount = rawIn.getReadCount();
                                long decodeCount = getReadCount();
                                String msg = new StringBuffer()
                                    .append( "Bytes read: " )
                                    .append( decodeCount )
                                    .append( " (compressed to " )
                                    .append( rawCount )
                                    .append( " = " )
                                    .append( (int) ( rawCount * 100.
                                                     / decodeCount ) )
                                    .append( "% " )
                                    .append( "using HTTP gzip content-coding)" )
                                    .toString();
                                logger_.log( countLevel, msg );
                            }
                        };
                    }
                }
                return in;
            }

            public InputStream getErrorStream( URLConnection conn )
                    throws IOException {
                if ( conn instanceof HttpURLConnection ) {
                    InputStream in =
                        ((HttpURLConnection) conn).getErrorStream();
                    if ( in != null && isGzip( conn ) ) {
                        return Compression.GZIP.decompress( in );
                    }
                    return in;
                }
                else {
                    return null;
                }
            }

            /**
             * Determines whether a given connection has actually
             * declared gzip encoding.  This makes the connection if it
             * hasn't already been done.
             *
             * @param  conn   connection
             * @return   true iff the content-encoding field says gzip
             */
            private boolean isGzip( URLConnection conn ) {
                if ( conn instanceof HttpURLConnection ) {
                    String coding =
                        ((HttpURLConnection) conn)
                       .getHeaderField( CONTENT_ENCODING );
                    return coding != null &&
                           coding.trim().toLowerCase().equals( GZIP_NAME );
                }
                else {
                    return false;
                }
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }
}
