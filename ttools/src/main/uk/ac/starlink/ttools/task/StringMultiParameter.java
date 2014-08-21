package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.MultiParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;

/**
 * Convenience parameter subclass for getting a sequence of string values.
 *
 * @author   Mark Taylor
 * @since    13 Oct 2008
 */
public class StringMultiParameter extends Parameter<String[]>
                                          implements MultiParameter {

    private final char valueSep_;

    /**
     * Constructor.
     *
     * @param  name  parameter name
     * @param  valueSep  value separator character
     */
    public StringMultiParameter( String name, char valueSep ) {
        super( name, String[].class, false );
        valueSep_ = valueSep;
    }

    public char getValueSeparator() {
        return valueSep_;
    }

    /**
     * Returns the values of this parameter as an array.
     * If the value is null, an empty array is returned.
     *
     * @param  env  execution environment
     * @return   array of individual values
     */
    public String[] stringsValue( Environment env ) throws TaskException {
        String[] objval = objectValue( env );
        return objval == null ? new String[ 0 ] : objval;
    }

    public String[] stringToObject( Environment env, String stringVal ) {
        return stringVal.split( new String( new char[] { valueSep_ } ) );
    }
}
