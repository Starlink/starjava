package uk.ac.starlink.task;

/**
 * Exception generated when a task does not complete correctly for some
 * known reason.
 */
public class ExecutionException extends TaskException {
    private int errorCode = 1;
    public ExecutionException() {
        super();
    }
    public ExecutionException( String message ) {
        super( message );
    }
    public ExecutionException( String message, int errorCode ) {
        super( message );
        this.errorCode = errorCode;
    }
    public ExecutionException( String message, Throwable cause ) {
        super( message, cause );
    }
    public ExecutionException( Throwable cause ) {
        super( cause );
    }
    public void setErrorCode( int code ) {
        errorCode = code;
    }
    public int getErrorCode() {
        return errorCode;
    }
}
