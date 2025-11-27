package uk.ac.starlink.ttools.taplint;

/**
 * Exception indicating that resource access has failed for reasons
 * related to authentication or authorization.
 *
 * @author   Mark Taylor
 * @since    27 Nov 2025
 */
public class AccessException extends Exception {

    /**
     * Constructor.
     *
     * @param   msg  message
     */
    public AccessException( String msg ) {
        super( msg );
    }

    /**
     * Constructor.
     *
     * @param   msg  message
     * @param   e   cause
     */
    public AccessException( String msg, Throwable e ) {
        super( msg, e );
    }
}
