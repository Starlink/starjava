package uk.ac.starlink.task;

/**
 * Exception generated when the value of a parameter is invalid.
 * <p>
 * Note each of the constructors takes the parameter in question as
 * its first argument.
 */
public class ParameterValueException extends UsageException {

    private final Parameter param_;

    public ParameterValueException( Parameter par ) {
        super( "Bad value for parameter " + par );
        param_ = par;
    }
    public ParameterValueException( Parameter par, String message ) {
        super( "Bad value for parameter " + par + ": " + message );
        param_ = par;
    }
    public ParameterValueException( Parameter par, String message, 
                                    Throwable cause ) {
        super( "Bad value for parameter " + par + ": " + message, cause );
        param_ = par;
    }
    public ParameterValueException( Parameter par, Throwable cause ) {
        super( "Bad value for parameter " + par, cause );
        param_ = par;
    }

    /**
     * Returns the parameter with which this exception is associated.
     *
     * @return  parameter
     */
    public Parameter getParameter() {
        return param_;
    }
}
