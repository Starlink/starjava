package uk.ac.starlink.auth;

import java.util.Arrays;

/**
 * Aggregates a username and password.
 * The password is supplied, and should ideally be always manipulated,
 * as an array of characters rather than a String for security reasons,
 * following practice elsewhere in the J2SE.
 * However the difference between using char[] and String is not all
 * that great.
 *
 * @author   Mark Taylor
 * @since    15 Jun 2020
 */
public class UserPass {

    private final String username_;
    private final char[] password_;

    /**
     * Constructor.
     *
     * @param   username   user name
     * @param   password   password
     */
    public UserPass( String username, char[] password ) {
        username_ = username == null ? "" : username;
        password_ = password == null ? new char[ 0 ] : password;
    }

    /**
     * Returns the username.
     *
     * @return  user name; may be zero length, but not null
     */
    public String getUsername() {
        return username_;
    }

    /**
     * Returns the password.
     *
     * @return  password; may be zero length, but not null
     */
    public char[] getPassword() {
        return password_;
    }

    @Override
    public int hashCode() {
        int code = 11296203;
        code = 23 * username_.hashCode();
        code = 23 * Arrays.hashCode( password_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof UserPass ) {
            UserPass other = (UserPass) o;
            return this.username_.equals( other.username_ )
                && Arrays.equals( this.password_, other.password_ );
        }
        else {
            return false;
        }
    }
}
