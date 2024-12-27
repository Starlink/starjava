package uk.ac.starlink.auth;

import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Manages authentication.  In general there would be one JVM-wide
 * instance of this class (obtained from {@link #getInstance()})
 * used for all access to URLs that might require or allow authentication;
 * it will intercept accesses as required and retain authentication
 * information as required for use in subsequent HTTP(S) requests
 * so that the user does not have to keep supplying credentials
 * where they are already known.
 *
 * <p>An application should typically call
 * {@link #setUserInterface setUserInterface}
 * on the default instance with an appropriate value near startup and
 * then use the same instance for all subsequent potentially authenticated
 * URL accesses.
 *
 * <p>To access (potentially) authenticated resources, client code
 * will usually just call {@link #openStream openStream(URL)} or
 * one of the various overloaded <code>connect</code> methods.
 * These are all convenience aliases for calls to the
 * {@link #makeConnection makeConnection} method that actually manages
 * authentication and redirection for connecting to a given URL.
 *
 * <p>Currently no attempt is made to handle proxy-authentication (407).
 *
 * @author   Mark Taylor
 * @since    16 Jun 2020
 */
public class AuthManager {

    private volatile UserInterface ui_;
    private final List<AuthScheme> schemes_;
    private final ContextList contexts_;
    private final Redirector dfltRedirector_;

    /** Default instance. */
    private static AuthManager instance_ =
        new AuthManager( (UserInterface) null, AuthUtil.getDefaultSchemes(),
                         Redirector.DEFAULT );

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.auth" );        

    /**
     * Constructor.  Note that in most cases best practice would be
     * to use the {@link #getInstance} method instead.
     *
     * @param  ui  user interface implementation;
     *             if null, no authentication management is attempted
     * @param  schemes   list of known authentication schemes,
     *                   ordered by preference
     * @param  dfltRedirector  handles default 3xx redirection behaviour
     */
    public AuthManager( UserInterface ui, AuthScheme[] schemes,
                        Redirector dfltRedirector ) {
        ui_ = ui;
        schemes_ =
            new CopyOnWriteArrayList<AuthScheme>( Arrays.asList( schemes ) );
        dfltRedirector_ = dfltRedirector;

        /* Set up a list to contain AuthContexts that we know about. */
        contexts_ = new ContextList();
    }

    /**
     * Sets the object that controls how the user is queried for credentials.
     * If the value is null, then this object will not attempt any
     * authentication management, and it will simply defer to normal
     * JVM behaviour.
     *
     * @param  ui  user interface to use
     *             if null, no authentication management is attempted
     */
    public void setUserInterface( UserInterface ui ) {
        ui_ = ui;
    }

    /**
     * Returns the object that controls how the user is queried for credentials.
     * If the value is null, then this object will not attempt any
     * authentication management, and it will simply defer to normal
     * JVM behaviour.
     *
     * @return  user interface in use
     *          if null, no authentication management is attempted
     */
    public UserInterface getUserInterface() {
        return ui_;
    }

    /**
     * Returns a mutable ordered list of authentication schemes known
     * by this manager.
     * Schemes earlier in this list are preferred over later ones.
     *
     * <p>As well as being mutable this list is thread-safe.
     *
     * @return  mutable list of schemes
     */
    public List<AuthScheme> getSchemes() {
        return schemes_;
    }

    /**
     * Clears all authentication state from this manager.
     * Any subsequent accesses to protected resources will require
     * new credential acquisition.
     */
    public void clear() {
        synchronized ( contexts_ ) {
            contexts_.clear();
        }
    }

    /**
     * Opens a URL connection to a given URL,
     * negotiating authentication and with default handling of 3xx redirection.
     * The return value is an open connection, that is one on which
     * <code>connect()</code> has been called.
     *
     * <p>An IOException is only thrown in unexpected circumstances;
     * connection failure is usually indicated by the status of the
     * returned connection object.
     *
     * @param  url  target URL
     * @return   open connection to URL, with authentication in place if
     *           applicable and possible
     */
    public URLConnection connect( URL url ) throws IOException {
        return connect( url, (UrlConnector) null );
    }

    /**
     * Opens a URL connection to a given URL with specified configuration,
     * negotiating authentication and with default handling of 3xx redirection.
     * The return value is an open connection, that is one on which
     * <code>connect()</code> has been called.
     *
     * <p>An IOException is only thrown in unexpected circumstances;
     * connection failure is usually indicated by the status of the
     * returned connection object.
     *
     * @param  url  target URL
     * @param  connector   obtains a connection from a URL;
     *                     may be null for default behaviour
     * @return   open connection to URL, with authentication in place if
     *           applicable and possible
     */
    public URLConnection connect( URL url, UrlConnector connector )
            throws IOException {
        return connect( url, connector, dfltRedirector_ );
    }

    /**
     * Opens a URL connection to a given URL with specified configuration,
     * negotiating authentication and with configurable handling
     * of 3xx redirection.
     * The return value is an open connection, that is one on which
     * <code>connect()</code> has been called.
     *
     * <p>An IOException is only thrown in unexpected circumstances;
     * connection failure is usually indicated by the status of the
     * returned connection object.
     *
     * @param  url  target URL
     * @param  connector   obtains a connection from a URL;
     *                     may be null for default behaviour
     * @param  redirector  controls handling of 3xx redirection
     * @return   open connection to URL, with authentication in place if
     *           applicable and possible
     */
    public URLConnection connect( URL url, UrlConnector connector,
                                  Redirector redirector )
            throws IOException {
        return makeConnection( url, connector, redirector ).getConnection();
    }

    /**
     * Opens a URL connection to a given URL with specified configuration,
     * negotiating authentication and with configurable handling
     * of 3xx redirection.
     * The return value is an AuthConnection which aggregates
     * an open connection, that is one on which
     * <code>connect()</code> has been called, and the AuthContext
     * which was used to open it.
     *
     * <p>An IOException is only thrown in unexpected circumstances;
     * connection failure is usually indicated by the status of the
     * returned connection object.
     *
     * @param  url  target URL
     * @param  connector   obtains a connection from a URL;
     *                     may be null for default behaviour
     * @param  redirector  controls handling of 3xx redirection
     * @return   object containing an open URLConnection with
     *           authentication in place if applicable and possible,
     *           and the AuthContext used to connect
     */
    public AuthConnection makeConnection( URL url, UrlConnector connector,
                                          Redirector redirector )
            throws IOException {

        /* Try to connect with an authentication context cached from
         * earlier behaviour that is manifestly applicable to this URL,
         * if there is one.  In general this should be reused rather than
         * asking the user for a new one, but that's not always the case
         * since it may have expired. */
        TestedContext tcontext0 = getUrlContext( url );
        AuthConnection aconn =
            connectWithContext( url, connector, tcontext0, redirector );
        if ( tcontext0 != null ) {
            assessAuthAttempt( tcontext0, aconn );
        }
        URLConnection conn = aconn.getConnection();
        if ( ui_ != null && conn instanceof HttpURLConnection ) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
            int code = hconn.getResponseCode();

            /* If the connection fails for reasons related to authentication,
             * try to authenticate by looking at the presented challenges. */
            if ( code == 401 || code == 403 ) {
                Challenge[] challenges = AuthUtil.getChallenges( hconn );
                if ( challenges.length == 0 && code == 401 ) {
                    // RFC7235 section 3.1
                    throw new IOException( "401 with no "
                                         + AuthUtil.CHALLENGE_HEADER
                                         + " challenges: " + url );
                }
                if ( challenges.length > 0 ) {

                    /* See if we have already confronted the user with
                     * one of these challenges.  If so, use the cached
                     * user response (which may or may not lead to
                     * successful authentication). */
                    TestedContext chContext =
                        getChallengeContext( challenges, url );
                    if ( chContext != null ) {
                        AuthConnection aconn2 =
                            connectWithContext( url, connector, chContext,
                                                redirector );
                        if ( assessAuthAttempt( chContext, aconn2 ) ) {
                            return aconn2;
                        }
                    }

                    /* Otherwise, pick a challenge and confront the user
                     * with it. */
                    ContextFactory cfact =
                        getPreferredContextFactory( challenges, url );
                    if ( cfact == null ) {
                        throw new IOException( "No supported auth-schemes in "
                                             + AuthUtil.CHALLENGE_HEADER );
                    }
                    return connectWithChallenge( url, connector, cfact,
                                                 redirector );
                }
            }
        }
        return aconn;
    }

    /**
     * Returns the content stream acquired by opening a URL.
     * This convenience method is shorthand for
     * <code>connect(url).getInputStream()</code>.
     *
     * @param   url  target URL
     * @return   an input stream for reading from the URL connection
     * @throws   IOException  in case of failure,
     *           including authentication failure
     */
    public InputStream openStream( URL url ) throws IOException {
        return connect( url ).getInputStream();
    }

    /**
     * Follows 3xx redirects, applying authentication as required.
     *
     * @param  conn  initial URL connection
     * @param  connector  obtains a connection from a URL;
     *                    may be null for default behaviour
     * @param  redirector   defines how redirection is done
     * @return  connected connection following redirects;
     *          in case of no redirection,
     *          this will be the same as the input connection
     */
    public URLConnection followRedirects( URLConnection conn,
                                          UrlConnector connector,
                                          Redirector redirector )
            throws IOException {
        URL url1 = redirector.getRedirectUrl( conn );
        if ( url1 != null ) {
            int hcode = conn instanceof HttpURLConnection
                      ? ((HttpURLConnection) conn).getResponseCode()
                      : -1;
            logger_.info( "HTTP " + hcode + " redirect to " + url1 );
            return connect( url1, connector, redirector );
        }
        else {
            return conn;
        }
    }

    /**
     * Attempts to establish authentication for an authcheck-type URL.
     * If it has not already been done for the domain in question,
     * this method may (depending on the <code>isForceLogin</code> parameter)
     * query the user for credentials as required,
     * and has the side-effect of setting up authentication to related
     * URLs for subsequent communication.
     *
     * <p>Setting the <code>isForceLogin</code> parameter affects whether
     * user interaction will take place.
     * If true, then any previous credentials for challenges received
     * are disregarded, and either a 200 or a 401/403 response will
     * trigger user interaction (request for credentials),
     * as long as challenges are present.
     * If false, any existing credentials are used where applicable,
     * and user interaction only takes place on a 401/403 response;
     * so a request for credentials only takes place if it can't
     * be avoided.
     *
     * <p>The expected behaviour of this authcheck endpoint is as follows,
     * in accordance with the (draft at time of writing) "SSO_next" proposal.
     * Briefly, it behaves as other endpoints in the service
     * <em>except</em> that a service allowing both authenticated and
     * anonymous access should accompany 200 responses to anonymous
     * access with an RFC7235 challenge.
     * In more detail: if authenticated or unauthenticated access is attempted,
     * it should provoke a 200/401/403 response, following normal
     * HTTP rules, matching the behaviour that a similarly authenticated
     * request would see when using the associated VO service.
     * However, in the case of a service that permits both authenticated
     * and anonymous access on the same endpoint, an anonymous request
     * should provoke a 200 response with an accompanying WWW-Authenticate
     * header (an authentication Challenge as defined in RFC7235 sec 4.1).
     * Thus clients attempting authenticated or unauthenticated access
     * must be prepared for
     * <ul>
     * <li>200 with or without a challenge</li>
     * <li>401 with a challenge</li> 
     * <li>403 with or without a challenge</li>
     * <li>404</li>
     * </ul>
     * Additionally, the response SHOULD contain an
     * <code>X-VO-Authenticated</code> header giving the authenticated
     * user ID if authentication has been established.
     * The response body is not defined by this proposal.
     *
     * @param   authcheckUrl  URL at which an authcheck endpoint may be present
     * @param   isHead   if true use a HEAD request, if false use GET
     * @param   isForceLogin  whether to force a new login where it could
     *                        be avoided
     * @return   characterisation of authentication status that has been
     *           established for the given URL, and will be used for
     *           compatible URLs in future if suitable
     */
    public AuthStatus authcheck( URL authcheckUrl, final boolean isHead,
                                 final boolean isForceLogin )
            throws IOException {

        /* Prepare behaviour. */
        Redirector redir = Redirector.NO_REDIRECT;
        UrlConnector connector = new UrlConnector() {
            public void connect( HttpURLConnection conn ) throws IOException {
                conn.setInstanceFollowRedirects( false );
                if ( isHead ) {
                    conn.setRequestMethod( "HEAD" );
                }
                conn.connect();
            }
        };

        /* Make a connection with no attempt at authentication. */
        URLConnection conn1 = authcheckUrl.openConnection();
        if ( ! ( conn1 instanceof HttpURLConnection ) ) {
            return AuthStatus.NO_AUTH;
        }
        HttpURLConnection hconn1 = (HttpURLConnection) conn1;
        connector.connect( hconn1 );

        /* Acquire response code and challenges, and determine
         * authentication type. */
        int code1 = hconn1.getResponseCode();
        Challenge[] challenges = AuthUtil.getChallenges( hconn1 );
        final AuthType authType;
        if ( code1 == 401 || code1 == 403 ) {
            authType = AuthType.REQUIRED;
        }
        else if ( code1 >= 200 && code1 < 300 ) {
            authType = challenges.length > 0 ? AuthType.OPTIONAL
                                             : AuthType.NONE;
        }
        else {
            return new AuthStatus( AuthType.UNKNOWN );
        }
        logger_.info( "Initial Authcheck connection to " + authcheckUrl
                    + ": " + code1 );

        /* Determine whether we may need to interact with the user to
         * acquire credentials. */
        if ( challenges.length > 0 &&
             ( authType == AuthType.REQUIRED || isForceLogin ) ) {

            /* If forcing login, discard any previously acquired credentials. */
            if ( isForceLogin ) {
                synchronized ( contexts_ ) {
                    for ( Challenge ch : challenges ) {
                        contexts_
                       .removeIf( item -> item.context_
                                         .isChallengeDomain( ch,
                                                             authcheckUrl ) );
                    }
                }
            }

            /* If the user has already supplied a response to this challenge,
             * and we haven't just discarded it, try using that. */
            AuthConnection aconn = null;
            TestedContext chContext =
                getChallengeContext( challenges, authcheckUrl );
            if ( chContext != null ) {
                assert ! isForceLogin;
                AuthConnection aconn1 =
                    connectWithContext( authcheckUrl, connector,
                                        chContext, redir );
                if ( assessAuthAttempt( chContext, aconn1 ) ) {
                    aconn = aconn1;
                }
            }

            /* Otherwise, confront the user with the challenge. */
            if ( aconn == null ) {
                ContextFactory cfact =
                    getPreferredContextFactory( challenges, authcheckUrl );
                if ( cfact == null ) {
                    logger_.warning( "No supported auth-schemes in "
                                   + AuthUtil.CHALLENGE_HEADER );
                }
                else if ( ui_ != null ) {
                    aconn = connectWithChallenge( authcheckUrl, connector,
                                                  cfact, redir );
                }
            }

            /* Extract authentication information from the connection if
             * possible. */
            if ( aconn != null &&
                 aconn.getConnection() instanceof HttpURLConnection ) {
                HttpURLConnection hconn2 =
                    (HttpURLConnection) aconn.getConnection();
                int code2 = hconn2.getResponseCode();
                AuthContext context2 = aconn.getContext();
                boolean isAuthenticated = code2 == 200
                                       && context2 != null
                                       && context2.hasCredentials();
                String authId = AuthUtil.getAuthenticatedId( aconn );
                return new AuthStatus( authType, isAuthenticated, authId );
            }
        }

        /* If we didn't manage to get any more authentication information,
         * return what we have. */
        return new AuthStatus( authType );
    }

    /**
     * Determine whether a connection acquired from a given context is
     * suitable for use.  This method has side-effects, it may update the
     * suppplied context to note success or failure.
     *
     * @param  tcontext  context used to acquire connection, may be updated
     * @param  aconn     connection
     * @return  true if the connection is suitable for use,
     *          false if further attempts should be made;
     *          note a true result does not necessarily mean that the
     *          connection was successfully established
     */
    private boolean assessAuthAttempt( TestedContext tcontext,
                                       AuthConnection aconn ) {
        URLConnection conn = aconn.getConnection();
        int code;
        if ( conn instanceof HttpURLConnection ) {
            try {
                code = ((HttpURLConnection) conn).getResponseCode();
            }
            catch ( IOException e ) {
                code = -1;
            }
        }
        else {
            code = -1;
        }
        boolean isAnonymous = ! tcontext.context_.hasCredentials();
        boolean isAuthFailure = code == 401 || code == 403;
        boolean isSuccess = code >= 200 && code < 400;

        /* If it was successful, record that and return true. */
        if ( isSuccess ) {
            tcontext.hasSucceeded_ = true;
            return true;
        }

        /* If authentication failed, record that.
         * If it was an anonymous attempt return true, since the user has
         * declined to authenticate, but if it had credentials return
         * false to indicate that other contexts may be attempted. */
        else if ( isAuthFailure ) {
            tcontext.hasFailed_ = true;
            return isAnonymous;
        }

        /* Failure for some other reason; return true, since other
         * authentication attempts probably will not help. */
        else {
            return true;
        }
    }

    /**
     * Returns an existing authentication context known to apply to a
     * given URL.
     *
     * @param  url  target URL
     * @return  appropriate authentication context, or null
     */
    private TestedContext getUrlContext( URL url ) {
        synchronized ( contexts_ ) {
            return contexts_.stream()
                            .filter( tc -> tc.context_.isUrlDomain( url ) )
                            .findFirst()
                            .orElse( null );
        }
    }

    /**
     * Returns an existing authentication context known to apply to a
     * a response that issued given challenges.
     *
     * @param  challenges  authentication challenges issued when attempting
     *                     to access URL
     * @param  url   the URL being accessed
     * @return   appropriate authentication context, or null
     */
    private TestedContext getChallengeContext( Challenge[] challenges,
                                               URL url ) {
        synchronized ( contexts_ ) {
            return contexts_
                  .stream()
                  .filter( tcontext ->
                       Arrays.stream( challenges )
                      .anyMatch( ch -> tcontext.context_
                                               .isChallengeDomain( ch, url ) ) )
                  .findFirst()
                  .orElse( null );
        }
    }

    /**
     * Indicates which challenge from a group is preferred.
     *
     * @param  challenges   available challenges from a 401 response
     * @param  url   URL with which the challenges were associated
     * @return   preferred (AuthScheme,Challenge) combination
     */
    private ContextFactory getPreferredContextFactory( Challenge[] challenges,
                                                       URL url ) {

        /* RFC 7235 sec 2.1 says:
         *    "When creating their values, the user agent ought to do so by
         *     selecting the challenge with what it considers to be the most
         *     secure auth-scheme that it understands, obtaining credentials
         *     from the user as appropriate." */
        for ( AuthScheme scheme : schemes_ ) {
            for ( Challenge ch : challenges ) {
                ContextFactory cfact;
                try {
                    cfact = scheme.createContextFactory( ch, url );
                }
                catch ( BadChallengeException e ) {
                    logger_.warning( "Challenge error reported"
                                   + " by scheme " + scheme.getName()
                                   + ": " + e.getMessage()
                                   + " (" + ch + ")" );
                    cfact = null;
                }
                if ( cfact != null ) {
                    return cfact;
                }
            }
        }
        return null;
    }

    /**
     * Attempts to connect to a given URL in accordance with a particular
     * RFC7235 challenge.  The user is queried for credentials, perhaps
     * repeatedly.  If the user fails or declines to authenticate,
     * the returned connection may have a 4xx status.
     *
     * @param  url  target URL
     * @param  connector   provides custom connection to a resource;
     *                     may be null for default behaviour
     * @param  cfact   context factory representing a parsed challenge
     * @param  redirector  controls handling of 3xx redirection
     * @return  open connection to URL
     */
    private AuthConnection connectWithChallenge( URL url,
                                                 UrlConnector connector,
                                                 ContextFactory cfact,
                                                 Redirector redirector )
            throws IOException {
        while ( true ) {

            /* Attempt to acquire credentials from the user in a form
             * which we can apply to the request.  There is no guarantee
             * that these credentials will be sufficient for authorization. */
            logger_.info( "Acquire credentials for " + url );
            AuthContext context = cfact.createContext( ui_ );

            /* If the user declines to supply credentials, store an anonymous
             * context; that prevents us asking the same question again
             * later. */
            if ( context == null ) {
                TestedContext anonContext =
                    new TestedContext( cfact.createUnauthContext() );
                assert ! anonContext.context_.hasCredentials();
                logger_.info( "Configuring anonymous context for " + url );
                synchronized ( contexts_ ) {
                    contexts_.add( anonContext );
                }
                return connectWithContext( url, connector, anonContext,
                                           redirector );
            }
            assert context.hasCredentials();
            TestedContext tcontext = new TestedContext( context );

            /* Otherwise, open the connection using the supplied credentials. */
            AuthConnection aconn =
                connectWithContext( url, connector, tcontext, redirector );
            URLConnection conn = aconn.getConnection();
            if ( conn instanceof HttpURLConnection ) {
                HttpURLConnection hconn = (HttpURLConnection) conn;
                int code = hconn.getResponseCode();

                /* No guarantee that auth succeeded, but we have gathered
                 * credentials from the user, so provisionally save them
                 * for subsequent use in the same authentication context. */
                synchronized ( contexts_ ) {
                    contexts_.add( tcontext );
                }

                /* If auth failed, message the user and prepare to ask
                 * again for credentials, if possible. */
                if ( code == 401 || code == 403 ) {
                    tcontext.hasFailed_ = true;
                    if ( ui_.canRetry() ) {
                        ui_.message( new String[] {
                            "Authentication failed",
                            AuthUtil.authFailureMessage( hconn ),
                        } );
                    }
                    else {
                        logger_.info( "Unsuccessful authentication"
                                    + " (" + code + ") for " + url );
                        return aconn;
                    }
                }
                else {

                    /* If auth succeeded, remember the validated context
                     * for future use. */
                    if ( code >= 200 && code < 300 ) {
                        tcontext.hasSucceeded_ = true;
                        AuthScheme scheme = context.getScheme();
                        StringBuffer sbuf = new StringBuffer()
                           .append( "Configuring authenticated context " );
                        if ( context.hasCredentials() ) {
                            sbuf.append( "using scheme " )
                                .append( context.getScheme().getName() );
                        }
                        sbuf.append( " for " )
                            .append( url )
                            .append( " (" )
                            .append( code )
                            .append( ")" );
                        logger_.info( sbuf.toString() );
                    }

                    /* In any case we don't have an auth failure,
                     * so there's no point retrying; return the successful
                     * or failed connection now. */
                    return aconn;
                }
            }
            else {
                return aconn;
            }
        }
    }

    /**
     * Opens a connection for communication
     * with a given URL using a given authentication context
     * and configurable handling of 3xx redirects.
     * In case of redirection, different authentication contexts
     * may be set up and used as required.
     *
     * @param  url  target URL
     * @param  connector   performs default HTTP connection;
     *                     may be null for default behaviour
     * @param  context   auth context containing required credentials, or null
     * @param  redirector  controls handling of 3xx redirection
     * @return   URL connection that is appropriately configured
     *           on which connect() has been called
     */
    private AuthConnection connectWithContext( URL url, UrlConnector connector,
                                               TestedContext tcontext,
                                               Redirector redirector )
            throws IOException {
        AuthContext context = tcontext == null ? null : tcontext.context_;
        logger_.config( ( context == null
                              ? "Unauthenticated"
                              : context.hasCredentials() ? "Authenticated"
                                                         : "Anonymous" )
                      + " connection to " + url );
        AuthConnection aconn = simpleConnect( url, connector, tcontext );
        URLConnection conn = aconn.getConnection();
        URL url1 = redirector.getRedirectUrl( conn );
        if ( url1 == null ) {
            return aconn;
        }
        else {
            int hcode = conn instanceof HttpURLConnection
                      ? ((HttpURLConnection) conn).getResponseCode()
                      : -1;
            logger_.config( "HTTP " + hcode + " redirect to " + url1 );
            return makeConnection( url1, connector, redirector );
        }
    }

    /**
     * Opens a connection for communication
     * with a given URL using a given fixed authentication context.
     * 3xx redirections are not handled and no context manipulation
     * or negotiation will occur.
     *
     * @param  url  target URL
     * @param  connector   performs default HTTP connection;
     *                     may be null for default behaviour
     * @param  context   auth context containing required credentials, or null
     * @return   URL connection that is appropriately configured
     *           on which connect() has been called
     */
    private static AuthConnection simpleConnect( URL url,
                                                 UrlConnector connector,
                                                 TestedContext tcontext )
            throws IOException {
        AuthContext context = tcontext == null ? null : tcontext.context_;
        URLConnection conn = url.openConnection();
        HttpURLConnection hconn = conn instanceof HttpURLConnection
                                ? (HttpURLConnection) conn
                                : null;
        if ( hconn != null ) {
            hconn.setInstanceFollowRedirects( false );
        }
        if ( hconn != null && context != null ) {
            context.configureConnection( hconn );
        }
        if ( hconn != null && connector != null ) {
            connector.connect( hconn );
        }
        else {
            conn.connect();
        }
        return new AuthConnection( conn, context );
    }

    /**
     * Returns the default instance of this class.
     * Note that the initial state of this instance has no user interface,
     * meaning there is no change to normal JVM behaviour.
     * To get this instance to manage authentication, it is necessary
     * first to call {@link #setUserInterface}.
     *
     * @return   default instance
     */
    public static AuthManager getInstance() {
        return instance_;
    }

    /**
     * Resets the default AuthManager instance.
     *
     * @param  authManager  new default instance
     */
    public static void setDefaultInstance( AuthManager authManager ) {
        instance_ = authManager;
    }

    /**
     * Utility class that records alongside an AuthContext a note of
     * whether it has ever succeeded and/or failed to authenticate.
     */
    private static class TestedContext {

        final AuthContext context_;
        boolean hasSucceeded_;
        boolean hasFailed_;

        /**
         * Constructor.
         *
         * @param  context  context
         */
        TestedContext( AuthContext context ) {
            context_ = context;
        }
    }

    /**
     * Mutable list implementation used to contain
     * AuthContexts known to the AuthManager.
     * This is a fairly normal list, except that using an iterator sorts
     * the elements so that the items come out in a preferred-first sequence
     * (contexts which have succeeded before ones which have failed).
     * It also purges any expired elements.
     * Since it extends AbstractSequentialList, everything is based on
     * iteration.
     *
     * <p>The implementation is not thread-safe, so uses should be synchronized.
     */
    private static class ContextList
            extends AbstractSequentialList<TestedContext> {

        private final List<TestedContext> list_;

        /** Comparator sorts succeeding contexts first, failing ones last. */
        private static final Comparator<TestedContext> CONTEXT_COMPARATOR =
            ( c1, c2 ) -> {
                if ( c1.hasFailed_ != c2.hasFailed_ ) {
                    return c1.hasFailed_ ? +1 : -1;
                }
                else if ( c1.hasSucceeded_ != c2.hasSucceeded_ ) {
                    return c1.hasSucceeded_ ? -1 : +1;
                }
                else {
                    return Integer
                          .compare( System.identityHashCode( c1.context_ ),
                                    System.identityHashCode( c2.context_ ) );
                }
            };

        ContextList() {
            list_ = new ArrayList<>();
        }

        public int size() {
            return list_.size();
        }

        public ListIterator<TestedContext> listIterator( int index ) {
            list_.removeIf( item -> item.context_.isExpired() );
            list_.sort( CONTEXT_COMPARATOR );
            return list_.listIterator( index );
        }

        @Override
        public void clear() {
            list_.clear();
        }
    }
}
