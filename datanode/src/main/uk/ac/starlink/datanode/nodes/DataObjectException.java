package uk.ac.starlink.datanode.nodes;

/**
 * Exception thrown when a data object which was expected to be present
 * cannot be supplied.
 *
 * @author   Mark Taylor (Starlink)
 * @since    19 Jan 2005
 */
public class DataObjectException extends Exception {

    public DataObjectException( String msg ) {
        super( msg );
    }

    public DataObjectException( String msg, Throwable th ) {
        super( msg, th );
    }

    public DataObjectException( Throwable th ) {
        super( th.getMessage(), th );
    }
}
