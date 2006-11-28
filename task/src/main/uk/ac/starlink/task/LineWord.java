package uk.ac.starlink.task;

/**
 * Represents a single word on the command line used as by LineEnvironment.
 *
 * @author   Mark Taylor
 * @since    27 Nov 2006
 */
public class LineWord {

    private final String name_;
    private final String value_;
    private final String text_;

    /**
     * Constructor.  The supplied text should be either of the form
     * <code>name=value</code> or, for positionally-determined parameters,
     * just <code>value</code>.
     *
     * @param   text  command-line argument
     */
    public LineWord( String text ) {
        text_ = text;
        int epos = text.indexOf( '=' );
        String sval;
        if ( epos > 0 ) {
            name_ = text.substring( 0, epos );
            sval = text.substring( epos + 1 );
        }
        else {
            name_ = null;
            sval = text;
        }
        value_ = ( sval.length() == 0 || sval.equals( "null" ) )
               ? LineEnvironment.NULL_STRING
               : sval;
    }

    /**
     * Returns the parameter name represented by this word.
     * May be null if none was specified.
     *
     * @return  parameter name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the parameter value represented by this word.
     *
     * @return  parameter value
     */
    public String getValue() {
        return value_;
    }

    /**
     * Returns the full text of the original command-line argument.
     *
     * @return   command-line argument
     */
    public String getText() {
        return text_;
    }

    public String toString() {
        return text_;
    }
}
