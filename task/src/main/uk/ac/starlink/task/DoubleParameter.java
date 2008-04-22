package uk.ac.starlink.task;

/**
 * Parameter representing a double precision number.
 */
public class DoubleParameter extends Parameter {

    private double doubleval_;

    public DoubleParameter( String name ) {
        super( name );
        setUsage( "<float-value>" );
    }

    public void setValueFromString( Environment env, String stringval )
            throws TaskException {
        double dval;
        if ( isNullPermitted() &&
             ( stringval == null || stringval.trim().length() == 0 ) ) {
            dval = Double.NaN;
        }
        else {
            try {
                dval = Double.parseDouble( stringval );
            }
            catch ( NumberFormatException e ) {
                throw new ParameterValueException( this, e.getMessage() );
            }
        }
        doubleval_ = dval;
        super.setValueFromString( env, stringval );
    }

    /**
     * Returns the value of this parameter as a <tt>double</tt>.
     * A null string value, if permitted, will give a NaN result.
     *
     * @param   env  execution environment
     * @return   double value
     */
    public double doubleValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return doubleval_;
    }
}
