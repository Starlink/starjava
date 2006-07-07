package uk.ac.starlink.task;

/**
 * Parameter representing a double precision number.
 */
public class DoubleParameter extends Parameter {

    private double doubleval;

    public DoubleParameter( String name ) {
        super( name );
        setUsage( "<float-value>" );
    }

    public void setValueFromString( Environment env, String stringval )
            throws TaskException {
        try {
            doubleval = Double.parseDouble( stringval );
        }
        catch ( NumberFormatException e ) {
            throw new ParameterValueException( this, e.getMessage() );
        }
        super.setValueFromString( env, stringval );
    }

    /**
     * Returns the value of this parameter as a <tt>double</tt>.
     *
     * @param   env  execution environment
     * @return   double value
     */
    public double doubleValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return doubleval;
    }
}
