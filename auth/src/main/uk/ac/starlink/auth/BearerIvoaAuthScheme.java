package uk.ac.starlink.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * IvoaAuthScheme concrete subclass for working with bearer tokens.
 *
 * <p>This implements the {@value #SCHEME_NAME} scheme.
 * The bearer token is acquired from the {@value #TOKEN_HEADER} header
 * of the login response, and is subsequently used in accordance with
 * RFC6750 like:
 * <pre>
 *    Authorization: Bearer &lt;token-text&gt;
 * </pre>
 *
 * <p>Note that at time of writing
 * <strong>it is not in general safe to use this scheme</strong>
 * because the details of token scope have not been worked out,
 * leading to the possibility of a third-party malevolent site
 * issuing a challenge that would result in stealing tokens.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2021
 * @see    <a href="https://datatracker.ietf.org/doc/html/rfc6750">RFC 6750</a>
 */
public class BearerIvoaAuthScheme extends IvoaAuthScheme {

    /** Singleton instance. */
    public static final BearerIvoaAuthScheme INSTANCE =
        new BearerIvoaAuthScheme();

    /** Name and identifier for this scheme ({@value}). */
    public static final String SCHEME_NAME = "ivoa_bearer";

    /** Name of login response header containing bearer token. */
    public static final String TOKEN_HEADER = "X-VO-Bearer";

    /**
     * Private constructor prevents instantiation.
     */
    private BearerIvoaAuthScheme() {
        super( SCHEME_NAME );
    }

    public ContextFactory createContextFactory( LoginProtocol loginProto,
                                                URL loginUrl,
                                                URL challengeUrl ) {
        final ProtectionSpace scope = new ProtectionSpace( challengeUrl, null );
        return new ContextFactory() {
            public AuthContext createContext( UserInterface ui ) {
                String token = readAuth( loginProto, BearerIvoaAuthScheme.this,
                                         loginUrl, ui,
                                         BearerIvoaAuthScheme::readBearerToken);
                return token == null ? null
                                     : createBearerContext( token );
            }
            public AuthContext createUnauthContext() {
                return createBearerContext( null );
            }
            private AuthContext createBearerContext( String token ) {
                return new BearerContext( BearerIvoaAuthScheme.this,
                                          loginUrl.toString(), scope, token );
            }
        };
    }

    /**
     * Reads a bearer token from a login response header.
     *
     * @param  conn  login response connection
     * @return   bearer token, not null
     */
    private static String readBearerToken( HttpURLConnection conn )
            throws IOException {
        String token = conn.getHeaderField( TOKEN_HEADER );
        conn.getInputStream().close();
        if ( token == null || token.trim().length() == 0 ) {
            throw new IOException( "No " + TOKEN_HEADER
                                 + " field in login response" );
        }
        else {
            return token;
        }
    }

    /**
     * AuthContext implementation for Bearer tokens.
     *
     * <p>Note this uses the context of a validity scope, which is not
     * currently defined by the ivoa_bearer scheme.
     * But the assumption is made that if this factory is suitable for
     * one URL in a given ProtectionScheme (host+port),
     * it will be suitable for all others.
     *
     * <p>Some other text that may have influence on validity scope
     * can be found in RFC 6750, sec 3:
     * <blockquote>
     *    A "realm" attribute MAY be included to indicate the scope of
     *    protection in the manner described in HTTP/1.1 [RFC2617].  The
     *    "realm" attribute MUST NOT appear more than once.
     * </blockquote>
     * which references RFC 2617 (Basic Auth), section 2:
     * <blockquote>
     *    A client SHOULD assume that all paths at or deeper than the depth of
     *    the last symbolic element in the path field of the Request-URI also
     *    are within the protection space specified by the Basic realm value of
     *    the current challenge. A client MAY preemptively send the
     *    corresponding Authorization header with requests for resources in
     *    that space without receipt of another challenge from the server.
     * </blockquote>
     */
    private static class BearerContext implements AuthContext {

        private final BearerIvoaAuthScheme scheme_;
        private final String loginUrl_;
        private final ProtectionSpace scope_;
        private final String token_;

        /**
         * Constructor.
         *
         * @param  scheme  scheme 
         * @param  loginUrl   URL at which the token can be acquired
         * @param  scope   URL range for which this context is valid
         * @param  token   IVOA bearer token, or null for anonymous
         */
        BearerContext( BearerIvoaAuthScheme scheme, String loginUrl,
                       ProtectionSpace scope, String token ) {
            scheme_ = scheme;
            loginUrl_ = loginUrl;
            scope_ = scope;
            token_ = token;
        }

        public AuthScheme getScheme() {
            return scheme_;
        }

        public boolean hasCredentials() {
            return token_ != null;
        }

        public boolean isUrlDomain( URL url ) {
            return scope_.equals( new ProtectionSpace( url, null ) );
        }

        public boolean isChallengeDomain( Challenge challenge, URL url ) {

            /* Matching protection space here is a gesture to avoiding
             * leakage of the token, but it's not robust and will also
             * block cross-domain usage where it may be legitimate. */
            try {
                return scheme_.createContextFactory( challenge, url ) != null
                    && loginUrl_
                      .equals( challenge.getParams().get( ACCESSURL_PARAM ) )
                    && scope_.equals( new ProtectionSpace( url, null ) );
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
            if ( token_ != null ) {
                connection.setRequestProperty( AuthUtil.AUTH_HEADER,
                                               "Bearer " + token_ );
            }
        }

        public String[] getCurlArgs( URL url, boolean showSecrets ) {
            return token_ == null
                 ? new String[ 0 ]
                 : new String[] {
                       "--header",
                       AuthUtil.AUTH_HEADER + ": Bearer " + token_,
                   };
        }
    }
}
