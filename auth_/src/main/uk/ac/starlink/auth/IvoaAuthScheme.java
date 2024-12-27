package uk.ac.starlink.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Partial AuthScheme implementation for standard SSO challenges.
 * This provides a framework for AuthSchemes in which the login protocol,
 * as defined by the <code>standard_id</code> challenge parameter,
 * is separable from the type of AuthContext that is produced.
 *
 * <p>It matches challenges of the form
 * <blockquote><code>
 *   WWW-Authenticate: &lt;schemeName&gt;
 *                     standard_id=&lt;login-protocol-name&gt;,
 *                     access_url=&lt;login-url&gt;
 * </code></blockquote>
 *
 * @author   Mark Taylor
 * @since    10 Dec 2021
 */
public abstract class IvoaAuthScheme implements AuthScheme {

    private final String schemeName_;

    /** Parameter name for the SSO login protocol standard id ({@value}). */
    public static final String STANDARDID_PARAM = "standard_id";

    /** Parameter name for the SSO login URL ({@value}). */
    public static final String ACCESSURL_PARAM = "access_url";

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.auth" );

    /**
     * Constructor.
     *
     * @param  schemeName  scheme name as presented in challenge
     */
    protected IvoaAuthScheme( String schemeName ) {
        schemeName_ = schemeName;
    }

    public String getName() {
        return schemeName_;
    }

    public ContextFactory createContextFactory( Challenge challenge, URL url )
            throws BadChallengeException {
        if ( schemeName_.equalsIgnoreCase( challenge.getSchemeName() ) ) {
            String stdId = challenge.getRequiredParameter( STANDARDID_PARAM );
            URL accUrl = challenge.getRequiredParameterUrl( ACCESSURL_PARAM );
            LoginProtocol loginProto = IvoaLoginProtocol.getProtocol( stdId );
            return loginProto == null
                 ? null
                 : createContextFactory( loginProto, accUrl, url );
        }
        return null;
    }

    /**
     * Creates a ContextFactory for this scheme with a given
     * login protocol instance.
     * The return value should generally not be null, since if it's got
     * as far as this call, the challenge looks like it's intended
     * for this scheme.
     *
     * @param  loginProto   login protocol
     * @param  accessUrl    login URL
     * @param  challengeUrl  URL from which the challenge was received
     * @return   context factory
     */
    protected abstract ContextFactory
            createContextFactory( LoginProtocol loginProto, URL accessUrl,
                                  URL challengeUrl )
            throws BadChallengeException;

    /**
     * Utility method that attempts to acquire a successful HTTP response
     * from a login URL given a login protocol, with credential input
     * from the user.
     *
     * <p>This method manages user interaction by retrying if appropriate
     * on 401/403 response codes, and returns a response with a 200 code.
     * Authentication schemes for which this is not appropriate are free
     * to manage user interaction without use of this method.
     *
     * @param  loginProto  login protocol
     * @param  authScheme  authentication scheme (used for messaging)
     * @param  loginUrl  URL at which credentials can be swapped for a token
     * @param  ui   user interface for supplying credentials
     * @return  200 response from login URL,
     *          or null if the user has declined to authenticate
     * @throws  IOException  if some communications failed;
     *                       in this case retry is not expected to help
     */
    public static HttpURLConnection
            getSuccessResponse( LoginProtocol loginProto, AuthScheme authScheme,
                                URL loginUrl, UserInterface ui )
            throws IOException {
        String[] msgLines = new String[] {
            "Login URL: " + loginUrl,
            "Auth Scheme: " + authScheme.getName(),
            "Login Protocol: " + loginProto.getProtocolName(),
        };
        do {
            UserPass userpass = ui.readUserPassword( msgLines );
            if ( userpass == null ) { 
                return null;
            }
            String username = userpass.getUsername();
            logger_.info( "Attempt login as " + username + " at " + loginUrl
                        + " (" + loginProto.getProtocolName() + ")" );
            HttpURLConnection conn =
                loginProto.presentCredentials( loginUrl, userpass );
            int code = conn.getResponseCode();
            if ( code == 200 ) { 
                logger_.info( "Login success for " + username );
                return conn;
            }
            else if ( code == 401 || code == 403 ) { 
                String failMsg = AuthUtil.authFailureMessage( conn );
                logger_.info( "Login failure for " + username + ": " + code
                            + " " + failMsg );
                if ( ui.canRetry() ) { 
                    ui.message( new String[] { failMsg } );
                }
                else {
                    throw new IOException( "Login attempt failed: " + failMsg );
                }
            }
            else {
                String failMsg = AuthUtil.authFailureMessage( conn );
                logger_.info( "Login failure for " + username + ": " + code
                            + " " + failMsg );
                throw new IOException( "Login error: " + failMsg );
            }
        } while ( ui.canRetry() );
        return null;
    }

    /**
     * Utility method that acquires authentication information
     * from a login URL given a login protocol, with credential input
     * from the user.
     *
     * <p>This method manages user interaction using
     * {@link #getSuccessResponse getSuccessResponse}
     * and either succeeds in returning the desired information
     * or returns null; in the latter case the user is messaged appropriately.
     *
     * @param  loginProto  login protocol
     * @param  authScheme  authentication scheme, used for messaging
     * @param  loginUrl  URL at which credentials can be swapped for a token
     * @param  ui   user interface for supplying credentials
     * @param  authReader  acquires auth info from a URL connection
     * @return  authentication information, or null if login failed
     */
    public static <A> A readAuth( LoginProtocol loginProto,
                                  AuthScheme authScheme, URL loginUrl,
                                  UserInterface ui, AuthReader<A> authReader ) {
        final HttpURLConnection conn;
        try {
            conn = getSuccessResponse( loginProto, authScheme, loginUrl, ui );
        }
        catch ( IOException e ) {
            ui.message( new String[] { "Login communications failed: " + e } );
            return null;
        }
        if ( conn == null ) {
            return null;
        }
        try {
            return authReader.readAuth( conn );
        }
        catch ( IOException e ) {
            ui.message( new String[] { "Login protocol failed: " + e } );
            return null;
        }
    }

    /**
     * Knows how to acquire specific authentication information
     * from an open URL connection.
     */
    @FunctionalInterface
    public interface AuthReader<T> {

        /**
         * Reads an item of information from an open URL connection.
         *
         * @param  conn  connection
         * @return   authentication information
         */
        T readAuth( HttpURLConnection conn ) throws IOException;
    }
}
