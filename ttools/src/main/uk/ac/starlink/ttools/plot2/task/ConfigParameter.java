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
 * @since    1 Mar 2013
 */
public class ConfigParameter<T> extends Parameter<T> {

    private final ConfigKey<T> key_;

    /**
     * Constructor.
     *
     * @param  key  config key
     * @param  baseName   parameter name excluding suffix
     * @param  suffix    parameter suffix, may be empty
     * @param  suffixType  word indicating what suffix identifies,
     *                     ignored if <code>suffix</code> is empty
     * @param  hasSuffixDetail  if true, adds additional description about how
     *                          suffixes are used
     * @param  exampleSuffix  suffix string to use in example documentation
     */
    @SuppressWarnings("this-escape")
    private ConfigParameter( ConfigKey<T> key, String baseName, String suffix,
                             String suffixType, boolean hasSuffixDetail,
                             String exampleSuffix ) {
        super( baseName + suffix, key.getValueClass(), true );
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
        if ( hasSuffixDetail ) {
            if ( suffix != null && suffix.length() > 0 ) {
                if ( prompt != null && prompt.length() > 0 ) {
                    prompt += " for " + suffixType + " " + suffix;
                }
            }
            final String extraDescrip;
            if ( suffix != null && suffix.length() > 0 ) {
                extraDescrip = new StringBuffer()
                   .append( "<p>This parameter affects " )
                   .append( suffixType )
                   .append( " <code>" )
                   .append( suffix )
                   .append( "</code>; if the <code>" )
                   .append( suffix )
                   .append( "</code> suffix is ommitted, it affects all " )
                   .append( suffixType )
                   .append( "s.</p>\n" )
                   .toString();
            }
            else {
                extraDescrip = new StringBuffer()
                   .append( "<p>If a " )
                   .append( suffixType )
                   .append( " suffix is appended to the parameter name,\n" )
                   .append( "only that " )
                   .append( suffixType )
                   .append( " is affected,\n" )
                   .append( "e.g. <code>" )
                   .append( baseName )
                   .append( exampleSuffix )
                   .append( "</code> affects only " )
                   .append( suffixType )
                   .append( " <code>" )
                   .append( exampleSuffix )
                   .append( "</code>.</p>\n" )
                   .toString();
            }
            if ( descrip != null && descrip.length() > 0 ) {
                descrip = new StringBuffer()
                         .append( descrip )
                         .append( extraDescrip )
                         .toString();
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
        this( key, key.getMeta().getShortName(), "", null, false, "???" );
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
     * Sets the typed default value for this parameter.
     *
     * @param  dflt  typed default value
     */
    public void setDefaultOption( T dflt ) {
        setStringDefault( dflt == null ? null
                                       : key_.valueToString( dflt ) );
    }

    /**
     * Returns a ConfigParameter based on the given key.
     * I think this factory method is required to invoke the constructor
     * in a typesafe way.
     *
     * @param  key  config key
     * @return  new parameter
     */
    public static <T> ConfigParameter<T>
            createConfigParameter( ConfigKey<T> key ) {
        return new ConfigParameter<T>( key );
    }

    /**
     * Returns a layer-indexed config parameter with a given layer suffix.
     * The name is constructed from the key name followed by the suffix.
     *
     * @param  key  config key
     * @param  layerSuffix   suffix part of name
     * @param  hasSuffixDetail  if true, adds additional description about
     *                          layer suffix usage
     * @return   new parameter
     */
    public static <T> ConfigParameter<T>
            createLayerSuffixedParameter( ConfigKey<T> key, String layerSuffix,
                                          boolean hasSuffixDetail ) {
        return new ConfigParameter<T>( key, key.getMeta().getShortName(),
                                       layerSuffix == null ? "" : layerSuffix,
                                       "layer", hasSuffixDetail,
                                       AbstractPlot2Task.EXAMPLE_LAYER_SUFFIX );
    }

    /**
     * Returns a zone-indexed config parameter with a given zone suffix.
     * The name is constructed from the key name followed by the suffix.
     *
     * @param  key  config key
     * @param  zoneSuffix   suffix part of name
     * @param  hasSuffixDetail  if true, adds additional description about
     *                          zone suffix usage
     * @return   new parameter
     */
    public static <T> ConfigParameter<T>
            createZoneSuffixedParameter( ConfigKey<T> key, String zoneSuffix,
                                         boolean hasSuffixDetail ) {
        return new ConfigParameter<T>( key, key.getMeta().getShortName(),
                                       zoneSuffix == null ? "" : zoneSuffix,
                                       "zone", hasSuffixDetail,
                                       AbstractPlot2Task.EXAMPLE_ZONE_SUFFIX );
    }
}
