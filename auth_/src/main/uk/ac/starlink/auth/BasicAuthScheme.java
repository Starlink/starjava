package uk.ac.starlink.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Base64;

/**
 * Implements HTTP Basic Authentication as defined in RFC7617.
 *
 * @author   Mark Taylor
 * @since    15 Jun 2020
 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
 */
public class BasicAuthScheme implements AuthScheme {

    /** Sole instance. */
    public static final BasicAuthScheme INSTANCE = new BasicAuthScheme();

    /**
     * Private constructor prevents external instantiation of singleton class.
     */
    private BasicAuthScheme() {
    }

    /**
     * @return  "Basic"
     */
    public String getName() {
        return "Basic";
    }

    public ContextFactory createContextFactory( Challenge challenge, URL url )
            throws BadChallengeException {
        if ( "Basic".equalsIgnoreCase( challenge.getSchemeName() ) ) {
            String realm = challenge.getRequiredRealm();
            return new BasicContextFactory( this, url, realm );
        }
        else {
            return null;
        }
    }

    /**
     * Encodes the user:password pair as a Base64-encoded string,
     * as described by RFC 7617.
     *
     * @param  user  user name
     * @param  pass  password
     * @return  base64-encoded string
     */
    public static String encodeUserPass( String user, char[] pass ) {
        int nc = user.length() + 1 + pass.length;
        char[] chrs = new char[ nc ];
        int ichr = 0;
        for ( char c : user.toCharArray() ) {
            chrs[ ichr++ ] = c;
        }
        chrs[ ichr++ ] = ':';
        for ( char c : pass ) {
            chrs[ ichr++ ] = c;
        }
        assert ichr == nc;
        byte[] bbuf = new String( chrs ).getBytes( AuthUtil.UTF8 );
        byte[] bb64 = Base64.getEncoder().encode( bbuf );
        return new String( bb64, AuthUtil.UTF8 );
    }

    /**
     * ContextFactory implementation for Basic auth scheme.
     */
    private static class BasicContextFactory implements ContextFactory {

        private final BasicAuthScheme scheme_;
        private final String realm_;
        private final URL url_;

        /**
         * Constructor.
         *
         * @param  scheme  auth scheme
         * @param  url   URL from which the original 
         * @param  realm  challenge realm
         */
        BasicContextFactory( BasicAuthScheme scheme, URL url, String realm ) {
            scheme_ = scheme;
            realm_ = realm;
            url_ = url;
        }

        public AuthContext createContext( UserInterface ui ) {
            String[] msg = {
                "Login URL: " + url_,
                "HTTP Basic Authentication (Realm: " + realm_ + ")",
            };
            UserPass userpass = ui.readUserPassword( msg );
            return userpass == null
                 ? null
                 : new BasicAuthContext( scheme_, url_, realm_, userpass );
        }

        public AuthContext createUnauthContext() {
            return new BasicAuthContext( scheme_, url_, realm_, null );
        }
    }

    /**
     * AuthContext implementation for Basic.
     */
    private static class BasicAuthContext implements AuthContext {

        private final BasicAuthScheme scheme_;
        private final String urlTxt_;
        private final String realm_;
        private final UserPass userpass_;
        private final ProtectionSpace pspace_;
        private final String scope_;

        /**
         * Constructor.
         *
         * @param  scheme  scheme
         * @param  url   URL for which challenge was issued
         * @param  realm  challenge realm
         * @param  userpass   username and password, or null for unauthentiated
         */
        public BasicAuthContext( BasicAuthScheme scheme, URL url, String realm,
                                 UserPass userpass ) {
            scheme_ = scheme;
            urlTxt_ = url.toString();
            realm_ = realm;
            userpass_ = userpass;
            pspace_ = new ProtectionSpace( url, realm );
            scope_ = url.toString().replaceFirst( "/[^/]+$", "/" );
        }

        public AuthScheme getScheme() {
            return scheme_;
        }

        public boolean hasCredentials() {
            return userpass_ != null;
        }

        public boolean isUrlDomain( URL url ) {
            // See RFC 7617 section 2.2.
            return url.toString().startsWith( scope_ );
        }

        public boolean isChallengeDomain( Challenge challenge, URL url ) {
            // See RFC 7235 section 2.2.
            try {
                return scheme_.createContextFactory( challenge, url ) != null
                    && new ProtectionSpace( url, challenge.getRealm() )
                      .equals( pspace_ );
            }
            catch ( BadChallengeException e ) {
                return false;
            }
        }

        public boolean isExpired() {
            return false;
        }

        public void configureConnection( HttpURLConnection connection )
                throws IOException {
            if ( userpass_ != null ) {
                String userpass64 = encodeUserPass( userpass_.getUsername(),
                                                    userpass_.getPassword() );
                connection.setRequestProperty( AuthUtil.AUTH_HEADER,
                                               "Basic " + userpass64 );
            }
        }

        public String[] getCurlArgs( URL url, boolean showSecrets ) {
            if ( userpass_ == null ) {
                return new String[ 0 ];
            }
            else {
                return new String[] {
                    "--basic",
                    "--user",
                    userpass_.getUsername() + ":" +
                    ( showSecrets ? new String( userpass_.getPassword() )
                                  : "[PASSWORD]" ),
                };
            }
        }
    }
}
