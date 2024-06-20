package uk.ac.starlink.task;

/**
 * Parameter representing a long integer value.
 */
public class LongParameter extends Parameter<Long> {

    private long min_;
    private long max_;

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    public LongParameter( String name ) {
        super( name, Long.class, false );
        min_ = Long.MIN_VALUE;
        max_ = Long.MAX_VALUE;
        setUsage( "<longint-value>" );
    }

    public Long stringToObject( Environment env, String stringval )
            throws ParameterValueException {
        long longval;
        try {
            longval = Long.parseLong( stringval );
        }
        catch ( NumberFormatException e ) {
            throw new ParameterValueException( this, e.getMessage() );
        }
        if ( longval < min_ ) {
            throw new ParameterValueException( this, longval 
                                             + " < minimum value " + min_ );
        }
        if ( longval > max_ ) {
            throw new ParameterValueException( this, longval
                                             + " < maximum value " + max_ );
        }
        return Long.valueOf( longval );
    }

    /**
     * Returns the value of this parameter as long primitive.
     *
     * @return   long value
     * @throws  NullPointerException  if parameter value is null
     *          (only possible if isNullPermitted true)
     */
    public long longValue( Environment env ) throws TaskException {
        return objectValue( env ).longValue();
    }

    /**
     * Mandates a minimum value for this parameter.
     *
     * @param  min  the smallest value this parameter may take
     */
    public void setMinimum( long min ) {
        min_ = min;
    }

    /**
     * Mandates a maximum value for this parameter.
     *
     * @param   max  the largest value this parameter may take.
     */
    public void setMaximum( long max ) {
        max_ = max;
    }
}
