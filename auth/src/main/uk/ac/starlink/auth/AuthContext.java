package uk.ac.starlink.auth;

import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;

/**
 * An object which is capable of manipulating HTTP connections to
 * add required authentication information.
 * 
 * @author   Mark Taylor
 * @since    15 Jun 2020
 */
public interface AuthContext {

    /**
     * Returns the scheme that yielded this context.
     *
     * @return  scheme implementation
     */
    AuthScheme getScheme();

    /**
     * Indicates whether this context is actually believed capable of
     * authenticating.  If this method returns false, it effectively
     * represents an anonymous connection; in that case the
     * {@link #configureConnection} method will normally do nothing.
     *
     * @return   true if this context can authenticate,
     *           false if it represents anonymous access
     */
    boolean hasCredentials();

    /**
     * Indicates whether this context is expected to be good for
     * authenticating a given URL.  Some authentication schemes
     * document scope or protection space rules that allow one
     * to determine whether a context representing a challenge
     * to one URL will be applicable to another.
     *
     * <p>If this method unconditionally returns false,
     * and a service has authenticated and anonymous access
     * at the same endpoints, it's generally hard for client code
     * ever to use the authenticated access.
     *
     * @param   url   URL for which authentication is required
     * @return  true iff this context ought to be able to authenticate
     *          to the given URL
     */
    boolean isUrlDomain( URL url );

    /**
     * Indicates whether this context is expected to be good for
     * authenticating against a given challenge.
     *
     * @param  challenge   challenge
     * @param  url  URL from which the challenge was received
     * @return  true iff this context ought to be able to answer the
     *          given challenge
     */
    boolean isChallengeDomain( Challenge challenge, URL url );

    /**
     * Indicates whether this authentication period's validity is known
     * to have expired.
     *
     * @return   true iff this context is known to be no longer useful
     */
    boolean isExpired();

    /**
     * Configures an HTTP connection with required authentication.
     * The supplied connection object is unopened
     * (no call to <code>connect()</code> has been made)
     * on entry and on exit.
     *
     * @param  connection  unopened HTTP connection object
     * @throws   IOException  if authentication could not be configured
     */
    void configureConnection( HttpURLConnection connection ) throws IOException;

    /**
     * Returns an array of command-line arguments that could be passed
     * to curl(1) corresponding to the authentication arrangements
     * implemented by this context.
     * This is informational, and done on a best-efforts basis.
     * An empty array may be returned if either no special arrangements
     * in curl are required, or if no curl arguments are known that
     * produce the desired effect.
     *
     * @param   url  URL to which curl would be connecting
     * @param   showSecret   if true, sensitive information such as passwords
     *                       may be included;
     *                       if false, such items must be omitted
     * @return   array of curl command-line argument strings;
     *           no attempt at shell quoting should be applied
     * @see  <a href="https://curl.haxx.se/docs/manpage.html">curl(1)</a>
     */
    String[] getCurlArgs( URL url, boolean showSecret );
}
