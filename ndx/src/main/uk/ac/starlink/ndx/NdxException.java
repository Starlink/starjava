package uk.ac.starlink.ndx;

/**
 * Exception indicating some failure in NDX access.
 */
public class NdxException extends Exception {
    public NdxException() {
        super();
    }

    public NdxException( String message ) {
        super( message );
    }

    public NdxException( String message, Throwable cause ) {
        super( message, cause );
    }

    public NdxException( Throwable cause ) {
        super( cause );
    }

}
