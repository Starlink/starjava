package uk.ac.starlink.task;

/**
 * Exception generated when the user signals an intention to abort a task.
 */
public class AbortException extends TaskException {
    public AbortException() {
        super();
    }
    public AbortException( String msg ) {
        super( msg );
    }
    public AbortException( String msg, Throwable cause ) {
        super( msg, cause );
    }
    public AbortException( Throwable cause ) {
        super( cause );
    }
}
