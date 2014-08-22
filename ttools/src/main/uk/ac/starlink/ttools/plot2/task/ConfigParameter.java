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
public class ConfigParameter<T> extends Parameter<T> {

    private final ConfigKey<T> key_;

    /**
     * Constructor.
     *
     * @param  name   parameter name
     * @param  key  config key
     */
    public ConfigParameter( String name, ConfigKey<T> key ) {
        super( name, key.getValueClass(), true );
        key_ = key;
        setStringDefault( key.valueToString( key.getDefaultValue() ) );
        boolean nullPermitted;
        try {
            key.stringToValue( null );
            nullPermitted = true;
        }
        catch ( Exception e ) {
            nullPermitted = false;
        }
        setNullPermitted( nullPermitted );
    }

    public T stringToObject( Environment env, String stringval )
            throws TaskException {
        try {
            return key_.stringToValue( stringval );
        }
        catch ( ConfigException e ) {
            throw new ParameterValueException( this, e );
        }
    }
}
