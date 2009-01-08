package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.MultiParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;

/**
 * Convenience parameter subclass which implements MultiParameter.
 *
 * @author   Mark Taylor
 * @since    13 Oct 2008
 */
public class DefaultMultiParameter extends Parameter implements MultiParameter {

    private final char valueSep_;

    /**
     * Constructor.
     *
     * @param  name  parameter name
     * @param  valueSep  value separator character
     */
    public DefaultMultiParameter( String name, char valueSep ) {
        super( name );
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
        String stringVal = stringValue( env );
        return stringVal == null
             ? new String[ 0 ]
             : stringVal.split( new String( new char[] { valueSep_ } ) );
    }
}
