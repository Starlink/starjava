package uk.ac.starlink.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Defines how HTTP 3xx redirects are handled.
 *
 * @author   Mark Taylor
 * @since    22 Jun 2020
 */
public abstract class Redirector {

    /** RFC 7231 section 6.4/7.1.2. */
    private static final String LOCATION_HEADER = "Location";

    /**
     * Default instance.  Redirects 301, 302, 303, 307 and 308 are followed,
     * and all protocol redirections are permitted except from HTTPS to
     * other (less secure) protocols.
     */
    public static final Redirector DEFAULT =
        createStandardInstance( new int[] { 301, 302, 303, 307, 308 } );

    /** No redirections are performed. */
    public static final Redirector NO_REDIRECT =
        createStandardInstance( new int[ 0 ] );

    /**
     * Indicates whether a given HTTP response code should be followed
     * as a redirect.  This will generaly return true for some or all
     * 3xx values and false otherwise.
     * Codes for which true is returned are assumed to imply an
     * accompanying Location header.
     *
     * @param  responseCode   HTTP response code
     * @return  true iff responseCode should result in following a Location
     *          header
     */
    public abstract boolean isRedirect( int responseCode );

    /**
     * Indicates whether HTTP 3xx redirection is permitted between
     * two URL protocols.  
     *
     * <p>The behaviour of the J2SE
     * {@link java.net.HttpURLConnection#setInstanceFollowRedirects} method
     * is that redirection is only permitted beween identical protocols
     * (for instance HTTP-&gt;HTTP, but not HTTP-&gt;HTTPS),
     * see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4620571.
     * This is for security reasons, but considerations of the same
     * arguments could reasonably lead to weaker restrictions.
     *
     * @param  fromProto   protocol part of source URL
     * @param  toProto   protocol part of destination URL
     * @see   java.net.URL#getProtocol
     */
    public abstract boolean willRedirect( String fromProto, String toProto );

    /**
     * Returns a URL to which connections should be redirected from
     * a supplied URL connection, according to this redirector's policy.
     *
     * @param   conn   URL connection; will be connected if it is not already
     * @return   URL to which requests should be redirected if any;
     *           otherwise null
     * @throws   IOException  if there is a problem with the redirection
     *                        (for instance missing Location header)
     */
    public URL getRedirectUrl( URLConnection conn ) throws IOException {
        if ( conn instanceof HttpURLConnection ) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
            int hcode = hconn.getResponseCode();
            if ( isRedirect( hcode ) ) {
                URL url0 = hconn.getURL();
                String loc1 = hconn.getHeaderField( LOCATION_HEADER );
                if ( loc1 == null || loc1.trim().length() == 0 ) {
                    throw new IOException( "No Location field for " + hcode
                                         + " response (" + url0 + ")" );
                }
                URL url1;
                try {
                    url1 = new URI( loc1 ).toURL();
                }
                catch ( MalformedURLException | URISyntaxException
                                              | IllegalArgumentException e ) {
                    throw (IOException)
                          new IOException( "Bad Location field for " + hcode
                                         + " response: " + loc1 )
                             .initCause( e );
                }
                String proto0 = url0.getProtocol().toLowerCase();
                String proto1 = url1.getProtocol().toLowerCase();
                if ( ! willRedirect( proto0, proto1 ) ) {
                    throw new IOException( "Refuse to redirect " + proto0
                                         + " URL to " + proto1 );
                }
                return url1;
            }
        }
        return null;
    }

    /**
     * Returns an instance that redirects on a given list of 3xx codes.
     * If the supplied list is empty, no redirection is performed.
     * Redirection between protocols is permitted except from HTTPS
     * to other (less secure) protocols; not this differs from J2SE
     * behaviour.
     *
     * @param  redirectCodes  list of zero or more 3xx codes which are
     *                        to be treated as redirects
     * @return  new instance
     */
    public static Redirector createStandardInstance( int[] redirectCodes ) {
        final int[] rcodes = redirectCodes.clone();
        return new Redirector() {
            public boolean isRedirect( int responseCode ) {
                for ( int redirCode : rcodes ) {
                    if ( responseCode == redirCode ) {
                        return true;
                    }
                }
                return false;
            }
            public boolean willRedirect( String fromProto, String toProto ) {
                if ( "https".equalsIgnoreCase( fromProto ) &&
                     ! "https".equalsIgnoreCase( toProto ) ) {
                    return false;
                }
                else {
                    return true;
                }
            }
        };
    }
}
