package uk.ac.starlink.auth;

import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * IvoaAuthScheme concrete subclass for working with cookies.
 * A cookie (or possibly cookies) is acquired by presenting credentials
 * to a URL provided in a challenge, and is subsequently used for 
 * access to protected resources.
 * Cookies themselves contain relevant scope information.
 *
 * <p>This implements the {@value #SCHEME_NAME} scheme.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2021
 * @see  <a href="https://tools.ietf.org/html/rfc2965">RFC 2965</a>
 */
public class CookieIvoaAuthScheme extends IvoaAuthScheme {

    /** Singleton instance. */
    public static final CookieIvoaAuthScheme INSTANCE =
        new CookieIvoaAuthScheme();

    /** Name and identifier for this scheme ({@value}). */
    public static final String SCHEME_NAME = "ivoa_cookie";

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.auth" );

    /**
     * Private constructor prevents external instantiation.
     */
    private CookieIvoaAuthScheme() {
        super( SCHEME_NAME );
    }

    public ContextFactory createContextFactory( LoginProtocol loginProto,
                                                URL loginUrl,
                                                URL challengeUrl ) {
        return new ContextFactory() {
            public AuthContext createContext( UserInterface ui ) {

                /* There are privacy implications for starting a cookie-based
                 * session (see RFC2965 sec 6.1), so it's good practice to
                 * inform the user that this is going on.
                 * However, given that the whole point of this is
                 * authentication, the privacy implications (that the software
                 * could "track the path of a user through the server")
                 * ought to be pretty obvious.
                 * At present, messages to the user do include the name
                 * of this scheme ("ivoa_cookie"), though not in a
                 * particularly prominent way.  It would be possible
                 * to add emphasis. */
                CookieManager cookieManager =
                    readAuth( loginProto, CookieIvoaAuthScheme.this, loginUrl,
                              ui, CookieIvoaAuthScheme::readCookies );
                return cookieManager == null
                     ? null
                     : createCookieContext( cookieManager );
            }
            public AuthContext createUnauthContext() {
                return createCookieContext( null );
            }
            private AuthContext createCookieContext( CookieManager cookMan ) {
                return new CookieContext( CookieIvoaAuthScheme.this,
                                          loginUrl, cookMan );
            }
        };
    }

    /**
     * Reads cookies from a login response.
     * 
     * <p>The return value is a CookieManager rather than a simple HttpCookie,
     * since the CookieManager knows how to do all the validity calculus
     * defined in RFC2965 sec 3.3.4.
     *
     * @param  conn  connection
     * @return  cookie manager containing any cookies present in the response
     */
    private static CookieManager readCookies( HttpURLConnection conn )
            throws IOException {
        URL url = conn.getURL();
        CookieManager cookieManager = new CookieManager();
        try {
            URI uri = url.toURI();
            cookieManager.put( uri, conn.getHeaderFields() );
        }
        catch ( URISyntaxException e ) {
        }
        conn.getInputStream().close();
        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
        int ncookie = cookies.size();
        if ( ncookie > 0 ) {
            StringBuffer sbuf = new StringBuffer();
            if ( ncookie == 1 ) {
                HttpCookie cookie = cookies.get( 0 );
                sbuf.append( "Acquired cookie: " )
                    .append( AuthUtil.cookieLogText( cookie ) );
            }
            else {
                sbuf.append( "Acquired cookies:" );
                for ( HttpCookie cookie : cookies ) {
                    sbuf.append( ' ' )
                        .append( AuthUtil.cookieLogText( cookie ) );
                }
            }
            logger_.info( sbuf.toString() );
            return cookieManager;
        }
        else {
            throw new IOException( "No cookie aquired from " + url );
        }
    }

    /**
     * Concatenates a list of field values for a Cookie or Cookie2 header.
     * The values are delimited using a semicolon.
     *
     * @param  cookieValues  list of field values for cookie request headers
     * @return  input items joined using a semicolon delimiter
     * @see   java.net.CookieManager#get
     */
    private static String getCookieHeaderValue( List<String> cookieValues ) {
        return cookieValues.stream().collect( Collectors.joining( "; " ) );
    }

    /**
     * AuthContext implementation used by this class.
     */
    private static class CookieContext implements AuthContext {

        private final CookieIvoaAuthScheme scheme_;
        private final String loginUrl_;
        private final CookieManager cookieManager_;

        /**
         * Constructor.
         *
         * @param   scheme   auth scheme from which this context arose
         * @param  loginUrl   cookie login URL
         * @param  cookieManager   cookie manager containing cookies to
         *                         present for authentication,
         *                         or null for unauthenticated access
         */
        CookieContext( CookieIvoaAuthScheme scheme, URL loginUrl,
                       CookieManager cookieManager ) {
            scheme_ = scheme;
            loginUrl_ = loginUrl.toString();
            cookieManager_ = cookieManager;
        }

        public AuthScheme getScheme() {
            return scheme_;
        }

        public boolean hasCredentials() {
            return cookieManager_ != null;
        }

        public void configureConnection( HttpURLConnection connection )
                throws IOException {
            if ( cookieManager_ != null ) {
                URI uri;
                try {
                    uri = connection.getURL().toURI();
                }
                catch ( URISyntaxException e ) {
                    assert false;
                    return;
                }

                /* Extract and present all the cookies that are relevant for
                 * this connection.  The CookieManager takes care of working
                 * out which ones they are, in accordance with RFC 2965
                 * section 3.3. */
                Map<String,List<String>> cookieProps =
                    cookieManager_.get( uri, connection.getRequestProperties());
                for ( Map.Entry<String,List<String>> entry :
                      cookieProps.entrySet() ) {
                    String hdrKey = entry.getKey();
                    String hdrValue = getCookieHeaderValue( entry.getValue() );
                    connection.addRequestProperty( hdrKey, hdrValue );
                }
            }
        }

        public String[] getCurlArgs( URL url, boolean showSecret ) {
            if ( cookieManager_ == null ) {
                return new String[ 0 ];
            }
            else {
                URI uri;
                try {
                    uri = url.toURI();
                }
                catch ( URISyntaxException e ) {
                    return new String[ 0 ];
                }
                Map<String,List<String>> cookieProps;
                try {
                    cookieProps =
                        cookieManager_
                       .get( uri, new HashMap<String,List<String>>() );
                }
                catch ( IOException e ) {
                    return new String[ 0 ];
                }
                List<String> args = new ArrayList<String>();
                for ( Map.Entry<String,List<String>> entry :
                      cookieProps.entrySet() ) {
                    String hdrKey = entry.getKey();
                    String hdrValue = getCookieHeaderValue( entry.getValue() );
                    args.add( "--header" );
                    args.add( hdrKey + ": " + hdrValue );
                }
                return args.toArray( new String[ 0 ] );
            }
        }

        public boolean isUrlDomain( URL url ) {
            URI uri;
            try {
                uri = url.toURI();
            }
            catch ( URISyntaxException e ) {
                return false;
            }

            // RFC 2965 sec 3.3.4.
            return cookieManager_ != null
                && cookieManager_.getCookieStore().get( uri ).size() > 0;
        }

        public boolean isChallengeDomain( Challenge challenge, URL url ) {

            /* Consider any ivoa_cookie challenge with the same login URL
             * as suitable.  Evil services could falsely present the same
             * login URL, but unlike for instance bearer tokens, the
             * CookieManager used by this context will not pass the cookie
             * on to any URL not allowed by the cookie's scoping rules,
             * so this does not present a security risk.
             * isUrlDomain will also return false for disallowed scopes. */
            try {
                return scheme_.createContextFactory( challenge, url ) != null
                    && loginUrl_.equals( challenge.getParams()
                                                  .get( ACCESSURL_PARAM ) );
            }
            catch ( BadChallengeException e ) {
                return false;
            }
        }

        public boolean isExpired() {

            /* The CookieManager automatically purges expired cookies. */
            return cookieManager_ != null
                && cookieManager_.getCookieStore().getCookies().size() == 0;
        }
    }
}
