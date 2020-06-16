package uk.ac.starlink.auth;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities used by authentication classes.
 *
 * @author   Mark Taylor
 * @since    15 Jun 2020
 */
public class AuthUtil {

    /** UTF-8 charset, guaranteed present. */
    public static final Charset UTF8 = Charset.forName( "UTF-8" );

    /** RFC 7235 challenge header key {@value} (RFC 7235 sec 4.1). */
    public static final String CHALLENGE_HEADER = "WWW-Authenticate";

    /** RFC7235 Authorization header key {@value} (RFC 7235 sec 4.2). */
    public static final String AUTH_HEADER = "Authorization";

    /** Header giving user authenticated ID {@value} (SSO_next). */
    public static final String AUTHID_HEADER = "X-VO-Authenticated";

    /**
     * Name of system property {@value} which if set "true"
     * will allow reporting of sensitive information such as passwords
     * through the logging system.
     */
    public static final String LOGSECRETS_PROP = "auth.logsecrets";

    /** Global config: if true, passwords etc may be logged by logger. */
    public static boolean LOG_SECRETS =
        "true".equalsIgnoreCase( System.getProperty( LOGSECRETS_PROP ) );

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.auth" );

    /**
     * Private constructor prevents authentication.
     */
    private AuthUtil() {
    }

    /**
     * Returns the HTTP response code from a URL connection.
     * In case of error (including if the connection is not an HTTP one),
     * -1 is returned.
     *
     * @param  conn  URL connection
     * @return   HTTP response code, or -1
     */
    public static int getResponseCode( URLConnection conn ) {
        if ( conn instanceof HttpURLConnection ) {
            try {
                return ((HttpURLConnection) conn).getResponseCode();
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING,
                             "Error retrieving HTTP status code: " + e, e );
                return -1; 
            }
        }
        else {   
            return -1;
        }
    }

    /**
     * Extracts challenges from an HTTP response.
     *
     * @param  conn  open URL connection (typically, but not necessarily, 401)
     * @return   challenges indicated in WWW-Authenticate header(s)
     */
    public static Challenge[] getChallenges( URLConnection conn ) {
        List<Challenge> challenges = new ArrayList<>();
        if ( conn != null ) {
            for ( Map.Entry<String,List<String>> entry :
                  conn.getHeaderFields().entrySet() ) {
                if ( CHALLENGE_HEADER.equalsIgnoreCase( entry.getKey() ) ) {
                    for ( String challengeTxt : entry.getValue() ) {
                        challenges.addAll( Challenge
                                          .parseChallenges( challengeTxt ) );
                    }
                }
            }
        }
        return challenges.toArray( new Challenge[ 0 ] );
    }

    /**
     * Returns the authenticated user ID recorded in the headers of
     * a URL connection.
     * This attempts to read the non-standard header {@value AUTHID_HEADER}.
     * If the header is absent, some placeholder non-null value is returned.
     * If the connection does not look like an authenticated one,
     * null is returned.
     *
     * @param  aconn   connection to endpoint expected to yield an auth ID
     * @return  real or placeholder authenticated user ID, or null
     */
    public static String getAuthenticatedId( AuthConnection aconn ) {
        URLConnection conn = aconn.getConnection();
        HttpURLConnection hconn =
            conn instanceof HttpURLConnection ? (HttpURLConnection) conn
                                              : null;
        if ( conn == null ) {
            return null;
        }
        int code;
        try {
            code = hconn.getResponseCode();
        }
        catch ( IOException e ) {
            return null;
        }
        AuthContext context = aconn.getContext();
        if ( code == 403 || code == 401 ) {
            return null;
        }
        else if ( code == 404 ) {
            return null;
        }
        else if ( context == null || ! context.hasCredentials() ) {
            return null;
        }
        else {
            String authId = hconn.getHeaderField( AUTHID_HEADER );
            return authId != null && authId.trim().length() > 0
                 ? authId
                 : "authenticated(unknownID)";
        }
    }

    /**
     * Prepares a short user-readable message indicating the state of a
     * connection that failed because of auth issues.
     *
     * @param   hconn  open connection, should usually be 401 or 403
     * @return  short message
     */
    public static String authFailureMessage( HttpURLConnection hconn ) {
        StringBuffer sbuf = new StringBuffer();
        try {
            int code = hconn.getResponseCode();
            sbuf.append( code )
                .append( ": " );
            switch ( code ) {
                case 401:
                    sbuf.append( "Unauthorized - bad credentials" );
                    break;
                case 403:
                    sbuf.append( "Forbidden - insufficient privileges" );
                    break;
                default:
                    sbuf.append( hconn.getResponseMessage() );
            }
        }
        catch ( IOException e ) {
            sbuf.append( "trouble!" );
        }
        return sbuf.toString();
    }

    /**
     * Returns the input string, unless it's null, in which case it
     * returns the empty string.
     *
     * @param  txt  string
     * @return   non-null equivalent string
     */
    public static String unNullString( String txt ) {
        return txt == null ? "" : txt;
    }

    /**
     * Returns a string suitable for reporting through the logging system
     * to represent a cookie.  Depending on the value of {@link #LOG_SECRETS},
     * it will or will not contain sensitive information.
     *
     * @param  cookie  cookie to represent
     * @return   loggable text
     */
    public static String cookieLogText( HttpCookie cookie ) {
        return LOG_SECRETS ? cookie.toString() : cookie.getName();
    }

    /**
     * Posts name=value pairs over HTTP  in
     * <code>application/x-www-form-urlencoded</code> format.
     *
     * @param  url  destination URL
     * @param  params   map of name-&gt;value pairs
     * @return an opened HTTP connection from which exit status and
     *         output content can be read
     */
    public static HttpURLConnection postForm( URL url,
                                              Map<String,String> params )
            throws IOException {
        URLConnection conn = url.openConnection();
        if ( conn instanceof HttpURLConnection ) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
            postForm( hconn, params );
            return hconn;
        }
        else {
            throw new IOException( "Not an HTTP URL: " + url );
        }
    }

    /**
     * Posts name=value pairs to an HTTP connection
     * in <code>application/x-www-form-urlencoded</code> format.
     * The supplied connection can be the result of a call to
     * <code>URL.openConnection()</code>, with or without some customization.
     *
     * @param  hconn  unopened connection
     * @param  params   map of name-&gt;value pairs
     */
    public static void postForm( HttpURLConnection hconn,
                                 Map<String,String> params )
            throws IOException {
        byte[] postBytes = toPostedBytes( params );
        hconn.setRequestMethod( "POST" );
        hconn.setRequestProperty( "Content-Type",
                                  "application/x-www-form-urlencoded" );
        hconn.setInstanceFollowRedirects( false );
        hconn.setDoOutput( true );
        hconn.connect();
        OutputStream hout = hconn.getOutputStream();
        try {
            hout.write( postBytes );
        }
        finally {
            hout.close();
        }
    }

    /**
     * Encodes a name-&gt;value mapping as an array of bytes suitable for
     * <code>application/x-www-form-urlencoded<code> transmission.
     *
     * @param   paramMap  name-&gt;value mapping
     * @return   byte array suitable for POSTing
     */
    private static byte[] toPostedBytes( Map<String,String> paramMap ) {
        StringBuffer sbuf = new StringBuffer();
        for ( Map.Entry<String,String> entry : paramMap.entrySet() ) {
            if ( sbuf.length() != 0 ) {
                sbuf.append( '&' );
            }
            try {
                sbuf.append( URLEncoder.encode( entry.getKey(), "UTF-8" ) )
                    .append( '=' )
                    .append( URLEncoder.encode( entry.getValue(), "UTF-8" ) );
            }
            catch ( UnsupportedEncodingException e ) {
                throw new RuntimeException( "No UTF-8??" );
            }
        }
        int nc = sbuf.length();
        byte[] bbuf = new byte[ nc ];
        for ( int i = 0; i < nc; i++ ) {
            char c = sbuf.charAt( i );
            assert (int) c == ( c & 0x7f );
            bbuf[ i ] = (byte) sbuf.charAt( i );
        }
        return bbuf;
    }
}
