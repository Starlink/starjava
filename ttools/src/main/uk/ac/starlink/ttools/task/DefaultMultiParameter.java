package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.MultiParameter;
import uk.ac.starlink.task.Parameter;

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
}
