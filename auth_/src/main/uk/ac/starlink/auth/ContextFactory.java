package uk.ac.starlink.auth;

/**
 * Contains configuration for creating AuthContext objects from
 * user credentials.
 *
 * @author   Mark Taylor
 * @since    15 Jun 2020
 * @see   AuthScheme#createContextFactory
 */
public interface ContextFactory {

    /**
     * Attempts to create an authentication context, by enquiring for
     * input from the UI as appropriate.
     * Should return an apparently viable, though not guaranteed valid, context
     * ({@link AuthContext#hasCredentials} will return true),
     * or null if the user declined to authenticate.
     *
     * <p>If appropriate, the implementation of this method should offer
     * the user retry attempts following failed or inadequate credential
     * entry in accordance with the supplied UserInterface.
     * Note however that retries must not be attempted
     * if {@link UserInterface#canRetry} returns false or
     * following a null return from {@link UserInterface#readUserPassword}.
     *
     * @param   ui     user interface that can be used to query the user
     *                 for credentials
     * @return   authentication context, or null if the user declined to
     *           authenticate
     */
    AuthContext createContext( UserInterface ui );

    /**
     * Creates a context representing unauthenticated (anonymous) access.
     * The resulting context, for which {@link AuthContext#hasCredentials}
     * will return false, may or may not be capable of connecting,
     * but it can represent the choice of a user not to authenticate for
     * a given challenge.
     *
     * @return  anonymous authentication context
     */
    AuthContext createUnauthContext();
}
