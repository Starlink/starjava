package uk.ac.starlink.task;

/**
 * Exception generated when the value of a parameter is invalid.
 * <p>
 * Note each of the constructors takes the parameter in question as
 * its first argument.
 */
public class ParameterValueException extends TaskException {
    public ParameterValueException( Parameter par ) {
        super( "Bad value for parameter " + par );
    }
    public ParameterValueException( Parameter par, String message ) {
        super( "Bad value for parameter " + par + ": " + message );
    }
    public ParameterValueException( Parameter par, String message, 
                                    Throwable cause ) {
        super( "Bad value for parameter " + par + ": " + message, cause );
    }
    public ParameterValueException( Parameter par, Throwable cause ) {
        super( "Bad value for parameter " + par, cause );
    }
}
