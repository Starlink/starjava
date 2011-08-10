package uk.ac.starlink.util;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

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

    /**
     * Constructor.
     */
    public PropertyAuthenticator() {
        authentication_ = createAuthentication();
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
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
     *
     * @see java.net.Authenticator#setDefault
     */
    public static void installInstance() {
        PropertyAuthenticator authenticator = new PropertyAuthenticator();
        if ( authenticator.getPasswordAuthentication() != null ) {
            Authenticator.setDefault( authenticator );
        }
    }
}
