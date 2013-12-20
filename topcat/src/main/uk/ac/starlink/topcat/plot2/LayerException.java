package uk.ac.starlink.topcat.plot2;

/**
 * Exception thrown if a plot layer cannot be added as requested.
 *
 * @author   Mark Taylor
 * @since    20 Dec 2013
 */
public class LayerException extends Exception {

    /**
     * Constructor.
     *
     * @param   msg   exception message
     */
    public LayerException( String msg ) {
        super( msg );
    }

    /**
     * Constructor with cause.
     *
     * @param   msg   exception message
     * @param   cause  underlying exception
     */
    public LayerException( String msg, Throwable cause ) {
        super( msg, cause );
    }
}
