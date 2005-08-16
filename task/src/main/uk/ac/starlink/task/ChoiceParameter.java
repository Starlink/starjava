package uk.ac.starlink.task;

/**
 * Parameter whose legal value must be one of a disjunction of given 
 * string values.  Matching is case-insensitive.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class ChoiceParameter extends Parameter {

    private final String[] options_;

    /**
     * Constructor.
     *
     * @param   name  parameter name
     * @param   options  legal values of this parameter
     */
    public ChoiceParameter( String name, String[] options ) {
        super( name );
        options_ = (String[]) options.clone();
    }

    public String getUsage() {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < options_.length; i++ ) {
            if ( i > 0 ) {
                sbuf.append( '|' );
            }
            sbuf.append( options_[ i ] );
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
            if ( value.equalsIgnoreCase( options_[ i ] ) ) {
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
}
