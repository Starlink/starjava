package uk.ac.starlink.task;

/**
 * Parameter value representing a boolean value.
 * Permissible string values are true, false, yes and no (case insensitive).
 *
 * @author   Mark Taylor
 * @since    9 Aug 2005
 */
public class BooleanParameter extends Parameter {

    private boolean booleanVal_;

    /**
     * Constructs a new boolean parameter.
     *
     * @param  name  parameter name
     */
    public BooleanParameter( String name ) {
        super( name );
        setUsage( "true|false" );
    }

    /**
     * Returns the value of this parameter as a boolean.
     *
     * @param   env  execution environment
     * @return  boolean value
     */
    public boolean booleanValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return booleanVal_;
    }

    /**
     * Sets the default as a boolean value.
     *
     * @param   dflt  default value
     */
    public void setDefault( boolean dflt ) {
        setDefault( dflt ? "true" : "false" );
    }

    public void setValueFromString( Environment env, String stringval )
            throws TaskException {
        if ( "TRUE".equalsIgnoreCase( stringval ) ||
             "YES".equalsIgnoreCase( stringval ) ) {
            booleanVal_ = true;
        }
        else if ( "FALSE".equalsIgnoreCase( stringval ) ||
                  "NO".equalsIgnoreCase( stringval ) ) {
            booleanVal_ = false;
        }
        else {
            throw new ParameterValueException( this, stringval +
                                               " is not true/false/yes/no" );
        }
        super.setValueFromString( env, stringval );
    }
}
