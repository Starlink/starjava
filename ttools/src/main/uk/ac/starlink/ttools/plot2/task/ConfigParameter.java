package uk.ac.starlink.ttools.plot2.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;

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
     * @param  key  config key
     * @param  baseName   parameter name excluding suffix
     * @param  layerSuffix    layer suffix, may be empty
     * @param  fullDetail  if true, adds additional description
     */
    private ConfigParameter( ConfigKey<T> key,
                             String baseName, String layerSuffix,
                             boolean fullDetail ) {
        super( baseName + layerSuffix, key.getValueClass(), true );
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

        ConfigMeta meta = key.getMeta();
        String usage = meta.getStringUsage();
        String prompt = meta.getShortDescription();
        String descrip = meta.getXmlDescription();
        if ( fullDetail ) {
            if ( layerSuffix != null && layerSuffix.length() > 0 ) {
                if ( prompt != null && prompt.length() > 0 ) {
                    prompt += " for layer " + layerSuffix;
                }
                if ( descrip != null && descrip.length() > 0 ) {
                    descrip = new StringBuffer()
                        .append( descrip )
                        .append( "<p>This parameter affects layer " )
                        .append( layerSuffix )
                        .append( "." )
                        .append( "</p>" )
                        .append( "\n" )
                        .toString();
                }
            }
        }
        if ( usage != null ) {
            setUsage( usage );
        }
        setPrompt( prompt );
        setDescription( descrip );
    }

    /**
     * Constructs an un-suffixed config parameter.
     *
     * @param  key  config key
     */
    public ConfigParameter( ConfigKey<T> key ) {
        this( key, key.getMeta().getShortName(), "", true );
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

    @Override
    public String objectToString( Environment env, T objval ) {
        return key_.valueToString( objval );
    }

    /**
     * Returns a config parameter with a given suffix.
     * The name is construted from the key name followed by the suffix.
     *
     * @param  key  config key
     * @param  layerSuffix   suffix part of name
     * @param  fullDetail  if true, adds additional description
     * @return   new parameter
     */
    public static <T> ConfigParameter<T>
            createSuffixedParameter( ConfigKey<T> key, String layerSuffix,
                                     boolean fullDetail ) {
        return new ConfigParameter<T>( key, key.getMeta().getShortName(),
                                       layerSuffix, fullDetail );
    }
}
