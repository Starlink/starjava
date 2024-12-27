package uk.ac.starlink.auth;

/**
 * Exception indicating that an authentication challenge is badly formed,
 * for instance missing parameters required by the scheme to which it
 * is supposed to conform.
 * 
 * @author   Mark Taylor
 * @since    3 Jul 2020
 */
public class BadChallengeException extends Exception {

    /**
     * Constructor.
     *
     * @param   msg  informative message
     */
    public BadChallengeException( String msg ) {
        super( msg );
    }

    /**
     * Constructor.
     *
     * @param   msg  informative message
     * @param   cause   chained exception underlying this one
     */
    public BadChallengeException( String msg, Throwable cause ) {
        super( msg, cause );
    }
}
