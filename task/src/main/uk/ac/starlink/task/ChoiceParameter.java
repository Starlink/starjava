package uk.ac.starlink.task;

/**
 * Parameter whose legal value must be one of a disjunction of given values.
 * Matching is case-insensitive against the stringified value of the option.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class ChoiceParameter extends Parameter {

    private final Object[] options_;
    private Object objectValue_;

    /**
     * Constructor.
     *
     * @param   name  parameter name
     * @param   options  legal values of this parameter
     */
    public ChoiceParameter( String name, Object[] options ) {
        super( name );
        options_ = (Object[]) options.clone();
    }

    public String getUsage() {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < options_.length; i++ ) {
            if ( i > 0 ) {
                sbuf.append( '|' );
            }
            sbuf.append( options_[ i ].toString() );
        }
        return sbuf.toString();
    }

    public void setValueFromString( Environment env, String value )
            throws TaskException {
        if ( value == null ) {
            super.setValueFromString( env, value );
            return;
        }
        for ( int i = 0; i < options_.length; i++ ) {
            if ( value.equalsIgnoreCase( options_[ i ].toString() ) ) {
                objectValue_ = options_[ i ];
                super.setValueFromString( env, value );
                return;
            }
        }
        StringBuffer sbuf = new StringBuffer()
            .append( "Unknown value " )
            .append( value )
            .append( " - must be one of " );
        for ( int i = 0; i < options_.length; i++ ) {
            if ( i > 0 ) {
                sbuf.append( ", " );
            }
            sbuf.append( options_[ i ] );
        }
        throw new ParameterValueException( this, sbuf.toString() );
    }

    /**
     * Returns the value as an object.  It will be identical to one of
     * the options of this parameter.
     *
     * @param  env  execution environment
     * @return  selected object
     */
    public Object objectValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return objectValue_;
    }

    /**
     * Returns a copy of the array of options which is accepted by this
     * parameter.
     *
     * @return  permitted options
     */
    public Object[] getOptions() {
        return (Object[]) options_.clone();
    }
}
