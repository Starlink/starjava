package uk.ac.starlink.ecsv;

/**
 * Exception thrown if an attempt to read an ECSV file fails because
 * the input does not appear to conform to the ECSV format.
 *
 * @author   Mark Taylor
 * @since    27 Apr 2020
 */
public class EcsvFormatException extends Exception {
    
    /**
     * Constructs an exception with a given message and cause.
     *
     * @param   msg   message
     * @param   cause   underlying exception
     */
    public EcsvFormatException( String msg, Throwable cause ) {
        super( msg, cause );
    }

    /**
     * Constructs an exception with a given message.
     *
     * @param   msg   message
     */
    public EcsvFormatException( String msg ) {
        this( msg, null );
    }
}
