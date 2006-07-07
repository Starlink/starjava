package uk.ac.starlink.task;

/**
 * Parameter representing an integer value.
 */
public class IntegerParameter extends Parameter {

    private boolean even;
    private boolean odd;
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;
    private int intval;

    public IntegerParameter( String name ) {
        super( name );
        setUsage( "<int-value>" );
    }

    /**
     * Mandates that any value of this parameter must be even.
     */
    public void setEven() {
        even = true;
    }

    /**
     * Mandates that any value of this parameter must be odd.
     */
    public void setOdd() {
        odd = true;
    }

    /**
     * Mandates a minimum value for this parameter.
     *
     * @param  min  the smallest value this parameter may take
     */
    public void setMinimum( int min ) {
        this.min = min;
    }

    /**
     * Mandates a maximum value for this parameter.
     *
     * @param   max  the largest value this parameter may take.
     */
    public void setMaximum( int max ) {
        this.max = max;
    }

    /**
     * Returns the value of this parameter as an <tt>int</tt>.
     *
     * @param  env  execution environment
     * @return   integer value
     */
    public int intValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return intval;
    }

    public void setValueFromString( Environment env, String stringval ) 
            throws TaskException {
        try {
            intval = Integer.parseInt( stringval );
        }
        catch ( NumberFormatException e ) {
            throw new ParameterValueException( this, e.getMessage() );
        }
        if ( odd && intval % 2 == 0 ) {
            throw new ParameterValueException( this, intval + " is not odd" );
        }
        if ( even && intval % 2 == 1 ) {
            throw new ParameterValueException( this, intval + " is not even" );
        }
        if ( intval < min ) {
            throw new ParameterValueException( this, intval 
                                             + " < minimum value " + min );
        }
        if ( intval > max ) {
            throw new ParameterValueException( this, intval
                                             + " < maximum value " + max );
        }
        super.setValueFromString( env, stringval );
    }
}
