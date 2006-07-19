package uk.ac.starlink.plastic;

/**
 * Exception thrown when a value passed by a PLASTIC call does not match
 * the {@link ValueType} it is supposed to have.
 *
 * @author   Mark Taylor
 * @since    10 Jul 2006
 */
public class ValueTypeException extends Exception {

    /**
     * Constructs an exception with a given message.
     *
     * @param  msg  error message
     */
    public ValueTypeException( String msg ) {
        super( msg );
    }

    /**
     * Constructs an exception with a given message and cause.
     *
     * @param   msg  error message
     * @param   cause   underlying error
     */
    public ValueTypeException( String msg, Throwable cause ) {
        super( msg, cause );
    }
}
