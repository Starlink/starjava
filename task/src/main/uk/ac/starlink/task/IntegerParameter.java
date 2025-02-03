package uk.ac.starlink.task;

/**
 * Parameter representing an integer value.
 */
public class IntegerParameter extends Parameter<Integer> {

    private boolean even;
    private boolean odd;
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;

    @SuppressWarnings("this-escape")
    public IntegerParameter( String name ) {
        super( name, Integer.class, false );
        setUsage( "<int-value>" );
    }

    public Integer stringToObject( Environment env, String stringval )
            throws ParameterValueException {
        int intval;
        try {
            intval = Integer.parseInt( stringval.replaceAll( "_", "" ) );
        }
        catch ( NumberFormatException e ) {
            throw new ParameterValueException( this, "Not an integer" );
        }
        if ( odd && intval % 2 == 0 ) {
            throw new ParameterValueException( this,
                                               intval + " is not odd" );
        }
        if ( even && intval % 2 == 1 ) {
            throw new ParameterValueException( this,
                                               intval + " is not even" );
        }
        if ( intval < min ) {
            throw new ParameterValueException( this, intval 
                                             + " < minimum value " + min );
        }
        if ( intval > max ) {
            throw new ParameterValueException( this, intval
                                             + " > maximum value " + max );
        }
        return Integer.valueOf( intval );
    }

    /**
     * Returns the value of this parameter as an int primitive.
     *
     * @return   int value
     * @throws  NullPointerException  if parameter value is null
     *          (only possible if isNullPermitted true)
     */
    public int intValue( Environment env ) throws TaskException {
        return objectValue( env ).intValue();
    }

    /**
     * Sets the default value as an integer.
     *
     * @param  dflt  new default value
     */
    public void setIntDefault( int dflt ) {
        setStringDefault( Integer.toString( dflt ) );
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
}
