package uk.ac.starlink.datanode.nodes;

/**
 * Exception thrown when construction of a DataNode fails with some reasonable
 * cause.
 */
public class NoSuchDataException extends Exception {
    public NoSuchDataException( String message ) {
        super( message );
    }

    public NoSuchDataException( Throwable th ) {
        super( th ); 
    }

    public NoSuchDataException( String message, Throwable th ) {
        super( message, th );
    }
}
