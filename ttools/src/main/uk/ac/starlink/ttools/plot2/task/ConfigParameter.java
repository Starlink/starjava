package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;

/**
 * Typed parameter subclass intended to get the value for a ConfigKey.
 *
 * @author   Mark Taylor
 * @since    1 Mark 2013
 */
public class ConfigParameter<T> extends Parameter {

    private final ConfigKey<T> key_;
    private T tval_;

    /**
     * Constructor.
     *
     * @param  name   parameter name
     * @param  key  config key
     */
    public ConfigParameter( String name, ConfigKey<T> key ) {
        super( name );
        key_ = key;
        setDefault( key.valueToString( key.getDefaultValue() ) );
    }

    @Override
    public void setValueFromString( Environment env, String stringval )
            throws TaskException {
        try {
            tval_ = key_.stringToValue( stringval );
        }
        catch ( ConfigException e ) {
            throw new ParameterValueException( this, e );
        }
        super.setValueFromString( env, stringval );
    }

    /**
     * Returns the value of this parameter in the type appropriate for
     * its config key.
     *
     * @param  env   execution environment
     * @return   typed parameter value
     */
    public T configValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return tval_;
    }
}
