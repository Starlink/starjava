package uk.ac.starlink.util;

/**
 * Exception thrown if an object creation fails for reasonable reasons.
 *
 * @author   Mark Taylor
 * @since    10 Aug 2005
 */
public class LoadException extends Exception {
    LoadException( String msg, Throwable e ) {
        super( msg, e );
    }
    LoadException( String msg ) {
        super( msg );
    }
}
