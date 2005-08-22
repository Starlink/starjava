package uk.ac.starlink.ttools.filter;

/**
 * Checked exception thrown when arguments encountered on the command
 * line are illegal.
 *
 * @author   Mark Taylor (Starlink)
 * @since    26 Apr 2005
 */
public class ArgException extends Exception {

    /**
     * Constructor.
     *
     * @param message   basic message
     */
    public ArgException( String message ) {
        super( message );
    }
}
