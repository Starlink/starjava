package uk.ac.starlink.task;

/**
 * Parameter for holding string values.
 *
 * @author  Mark Taylor
 * @since   20 Aug 2014
 */
public class StringParameter extends Parameter<String> {

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    public StringParameter( String name ) {
        super( name, String.class, false );
    }

    public String stringToObject( Environment env, String sval )
            throws TaskException {
        return sval;
    }
}
