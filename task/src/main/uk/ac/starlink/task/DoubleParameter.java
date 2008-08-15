package uk.ac.starlink.task;

/**
 * Parameter representing a double precision number.
 *
 * @author   Mark Taylor
 */
public class DoubleParameter extends Parameter {

    private double doubleval_;
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
        super( name );
        setUsage( "<float-value>" );
    }

    /**
     * Sets the minimum acceptable value for this parameter.
     *
     * @param   min   minimum value
     * @param   inclusive  true iff <code>min</code> itself is permitted
     */
    public void setMinimum( double min, boolean inclusive ) {
        min_ = Double.isNaN( min ) ? null : new Double( min );
        minInclusive_ = inclusive;
    }

    /**
     * Sets the maximum acceptable value for this parameter.
     *
     * @param   max  maximum value
     * @param   inclusive   true iff <code>max</code> itself is permitted
     */
    public void setMaximum( double max, boolean inclusive ) {
        max_ = Double.isNaN( max ) ? null : new Double( max );
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
