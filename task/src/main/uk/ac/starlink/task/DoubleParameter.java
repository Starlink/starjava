package uk.ac.starlink.task;

/**
 * Parameter representing a double precision number.
 */
public class DoubleParameter extends Parameter {

    private double doubleval;

    public DoubleParameter( String name ) {
        super( name );
    }

    public void setValueFromString( String stringval )
            throws ParameterValueException {
        try {
            doubleval = Double.parseDouble( stringval );
        }
        catch ( NumberFormatException e ) {
            throw new ParameterValueException( this, e.getMessage() );
        }
        super.setValueFromString( stringval );
    }

    /**
     * Returns the value of this parameter as a <tt>double</tt>.
     */
    public double doubleValue() throws ParameterValueException, AbortException {
        checkGotValue();
        return doubleval;
    }
}
