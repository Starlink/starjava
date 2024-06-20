package uk.ac.starlink.task;

/**
 * Parameter representing a double precision number.
 *
 * @author   Mark Taylor
 */
public class DoubleParameter extends Parameter<Double> {

    private Double min_;
    private Double max_;
    private boolean minInclusive_;
    private boolean maxInclusive_;

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    public DoubleParameter( String name ) {
        super( name, Double.class, false );
        setUsage( "<float-value>" );
    }

    /**
     * Returns the value of this parameter as a <code>double</code>.
     * A null string value, if permitted, will give a NaN result.
     *
     * @param   env  execution environment
     * @return   double value
     */
    public double doubleValue( Environment env ) throws TaskException {
        Double objVal = objectValue( env );
        return objVal == null ? Double.NaN : objVal.doubleValue();
    }

    /**
     * Sets the default value as a floating point value.
     *
     * @param  dflt  new default value
     */
    public void setDoubleDefault( double dflt ) {
        setStringDefault( Double.isNaN( dflt ) ? "" : Double.toString( dflt ) );
    }

    /**
     * Sets the minimum acceptable value for this parameter.
     *
     * @param   min   minimum value
     * @param   inclusive  true iff <code>min</code> itself is permitted
     */
    public void setMinimum( double min, boolean inclusive ) {
        min_ = Double.isNaN( min ) ? null : Double.valueOf( min );
        minInclusive_ = inclusive;
    }

    /**
     * Sets the maximum acceptable value for this parameter.
     *
     * @param   max  maximum value
     * @param   inclusive   true iff <code>max</code> itself is permitted
     */
    public void setMaximum( double max, boolean inclusive ) {
        max_ = Double.isNaN( max ) ? null : Double.valueOf( max );
    }

    public Double stringToObject( Environment env, String stringval )
            throws TaskException {
        double dval;
        try {
            dval = Double.parseDouble( stringval );
        }
        catch ( NumberFormatException e ) {
            throw new ParameterValueException( this, e.getMessage() );
        }
        if ( min_ != null ) {
            double dmin = min_.doubleValue();
            if ( minInclusive_ ) {
                if ( dval < dmin ) {
                    throw new ParameterValueException(
                            this, dval + " < minimum value " + dmin );
                }
            }
            else {
                if ( dval <= dmin ) {
                    throw new ParameterValueException(
                            this, dval + " <= minimum value " + dmin );
                }
            }
        }
        if ( max_ != null ) {
            double dmax = max_.doubleValue();
            if ( maxInclusive_ ) {
                if ( dval > dmax ) {
                    throw new ParameterValueException( 
                            this, dval + " > maximum value " + dmax );
                }
            }
            else {
                if ( dval >= dmax ) {
                    throw new ParameterValueException(
                            this, dval + " >= maximum value " + dmax );
                }
            }
        }
        return dval;
    }

    /**
     * As a special case, setting the value of this parameter with a
     * null or empty string will result in a NaN value.
     */
    @Override
    public void setValueFromString( Environment env, String stringval )
            throws TaskException {
        if ( ( stringval == null || stringval.trim().length() == 0 ) &&
             isNullPermitted() ) {
            setValue( stringval, Double.NaN );
        }
        else {
            super.setValueFromString( env, stringval );
        }
    }
}
