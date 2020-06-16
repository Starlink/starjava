package uk.ac.starlink.auth;

/**
 * Characterises the authentication associated with a connection.
 *
 * @author   Mark Taylor
 * @since    7 Feb 2022
 */
public class AuthStatus {

    private final AuthType authType_;
    private final boolean isAuthenticated_;
    private final String authId_;

    /** Connection with no possible or actual authentication. */
    public static final AuthStatus NO_AUTH =
        new AuthStatus( AuthType.NONE, false, null );

    /**
     * Constructs an AuthStatus with a given type but no actual authentication.
     *
     * @param   authType  authentication type
     */
    public AuthStatus( AuthType authType ) {
        this( authType, false, null );
    }

    /**
     * Constructs an AuthStatus with given characteristics.
     *
     * @param  authType  authentication type
     * @param  isAuthenticated  true if authentication has been established
     * @param  authId   user identifier for authenticated identity;
     *                  should normally be non-null if isAuthenticated is true, 
     *                  but not guaranteed to be
     */
    public AuthStatus( AuthType authType, boolean isAuthenticated,
                       String authId ) {
        authType_ = authType;
        isAuthenticated_ = isAuthenticated;
        authId_ = authId;
    }

    /**
     * Returns the authentication type for this connection.
     *
     * @return  authentication type
     */
    public AuthType getAuthType() {
        return authType_;
    }

    /**
     * Indicates whether authentication has been established.
     *
     * @return  true for authenticated
     */
    public boolean isAuthenticated() {
        return isAuthenticated_;
    }

    /**
     * Returns the authenticated user ID for this connection.
     * Usually this will be non-null iff {@link #isAuthenticated} is true,
     * but that's not guaranteed.
     *
     * @return   user ID
     */
    public String getAuthenticatedId() {
        return authId_;
    }

    /**
     * Returns a non-null string that can be used to characterise the user
     * identity.
     *
     * @return   non-null identity description
     */
    public String getIdentityString() {
        if ( isAuthenticated_ ) {
            return authId_ == null ? "(authenticated-unknownId)"
                                   : authId_;
        }
        else {
            return "(anonymous)";
        }
    }

    @Override
    public String toString() {
        return authType_ + ": " + getIdentityString();
    }
}
