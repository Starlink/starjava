package uk.ac.starlink.util;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.logging.Logger;

/**
 * Authenticator implementation which uses system properties to set
 * fixed username and passwords for HTTP Basic Authentication.
 * The username and password are acquired from the system properties
 * {@value #USER_PROP} and {@value #PASSWORD_PROP}.
 * A convenience method {@link #installInstance} is provided to set this
 * authenticator up for use in all HTTP connection attempts.
 *
 * <p>Since this uses the same username and password for all web sites,
 * it's obviously a bit of a blunt instrument.  It may be refined at
 * some point in the future.
 *
 * @author   Mark Taylor
 * @since    10 Aug 2011
 */
public class PropertyAuthenticator extends Authenticator {

    /** System property supplying basic authentication username ({@value}). */
    public static final String USER_PROP = "star.basicauth.user";

    /** System property supplying basic authentication password ({@value}). */
    public static final String PASSWORD_PROP = "star.basicauth.password";

    private final PasswordAuthentication authentication_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.util" );

    /**
     * Constructor.
     */
    public PropertyAuthenticator() {
        authentication_ = createAuthentication();
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        if ( authentication_ != null ) {
            logger_.info( "Supplying HTTP authentication for "
                        + getRequestingURL()
                        + ", user=" + authentication_.getUserName() );
        }
        return authentication_;
    }

    /**
     * Returns a PasswordAuthentication instance suitable for use with
     * PropertyAuthenticator.
     * It reads the current values of the properties and keeps them for later.
     *
     * @return  authentication
     */
    public static PasswordAuthentication createAuthentication() {
        try {
            String user = System.getProperty( USER_PROP, null );
            String password = System.getProperty( PASSWORD_PROP, "" );
            return user == null
                 ? null
                 : new PasswordAuthentication( user, password.toCharArray() );
        }
        catch ( SecurityException e ) {
            return null;
        }
    }

    /**
     * Installs an instance of PropertyAuthenticator so that it is used
     * automatically in response to all 401 Unauthorized HTTP responses.
     * The authenticator is only installed if the properties are present.
     * If the <code>offerAdvice</code> parameter is true, then if
     * the properties are not set up, an authenticator is installed which
     * issues a message advising how to use system properties to get
     * the authenticator working next time.
     *
     * @param  offerAdvice  if true, install an advising authenticator if the
     *                      property one isn't going to work
     * @return  true iff an authenticator was installed;
     *          if <code>offerAdvice</code> is true, will always return true
     *
     * @see java.net.Authenticator#setDefault
     */
    public static boolean installInstance( boolean offerAdvice ) {
        PropertyAuthenticator propAuth = new PropertyAuthenticator();
        if ( propAuth.authentication_ != null ) {
            logger_.info( "Installing authenticator user="
                        + propAuth.authentication_.getUserName() );
            Authenticator.setDefault( propAuth );
            return true;
        }
        else if ( offerAdvice ) {
            Authenticator.setDefault( new AdviceAuthenticator() );
            return true;
        }
        return false;
    }

    /**
     * Authenticator implementation which writes a message through the
     * logging system advising how to use PropertyAuthenticator if
     * authentication is requested.
     */
    private static class AdviceAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            String src = getRequestingHost();
            if ( src == null ) {
                src = String.valueOf( getRequestingURL() );
            }
            String msg = new StringBuffer()
                .append( "Authentication requested for " )
                .append( src )
                .append( "." )
                .append( "  No credentials available." )
                .append( "  Try system properties " )
                .append( USER_PROP )
                .append( "/" )
                .append( PASSWORD_PROP )
                .append( "." )
                .toString();
            logger_.warning( msg );
            return null;
        }
    }
}
