package uk.ac.starlink.task;

/**
 * Superclass for exceptions in the task package.
 */
public class TaskException extends Exception {
    public TaskException() {
        super();
    }
    public TaskException( String msg ) {
        super( msg );
    }
    public TaskException( String msg, Throwable cause ) {
        super( msg, cause );
    }
    public TaskException( Throwable cause ) {
        super( cause );
    }
}
