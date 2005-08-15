package uk.ac.starlink.task;

/**
 * An Exception thrown when a task is invoked with the wrong usage.
 */
public class UsageException extends TaskException {
    private String usage;

    public UsageException() {
        super();
    }
    public UsageException( String msg ) {
        super( msg );
    }
    public UsageException( String msg, Throwable cause ) {
        super( msg, cause );
    }
    public UsageException( Throwable cause ) {
        super( cause );
    }
}
