package uk.ac.starlink.plastic;

/**
 * Exception thrown to indicate failure of a hub test.
 *
 * @author   Mark Taylor
 * @since    7 Aug 2006
 * @see   HubTester
 */
public class HubTestException extends Exception {

    /**
     * Constructs an exception from a message string.
     *
     * @param  msg  message
     */
    public HubTestException( String msg ) {
        super( msg );
    }

    /**
     * Constructs an exception from a message string and a cause.
     *
     * @param  msg  message
     * @param  e   cause exception
     */
    public HubTestException( String msg, Throwable e ) {
        super( msg, e );
    }
}
