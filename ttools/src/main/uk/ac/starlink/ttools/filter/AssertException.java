package uk.ac.starlink.ttools.filter;

import java.io.IOException;

/**
 * IOException which results from the failure of a user-level assertion.
 *
 * @see      AssertFilter
 * @author   Mark Taylor
 * @since    2 May 2006
 */
public class AssertException extends IOException {

    /**
     * Constructs an AssertException with a message.
     *
     * @param   msg  message
     */
    public AssertException( String msg ) {
        super( msg );
    }

    /**
     * Constructs an AssertException with a message and a cause.
     *
     * @param   msg  message
     * @param   e    cause
     */
    @SuppressWarnings("this-escape")
    public AssertException( String msg, Throwable e ) {
        super( msg );
        initCause( e );
    }
}
