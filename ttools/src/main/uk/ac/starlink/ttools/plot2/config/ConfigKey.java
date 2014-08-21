package uk.ac.starlink.ttools.plot2.config;

/**
 * Typed key for use in a ConfigMap.
 * As well as serving as a key in {@link ConfigMap},
 * this class has methods to allow a command-line or graphical interface
 * to be constructed for the corresponding values automatically.
 *
 * <p>Note that this class does <em>not</em> sport the
 * {@link uk.ac.starlink.ttools.plot2.Equality}
 * annotation.  A ConfigKey is normally considered equal only to itself,
 * not to other similarly-named ConfigKeys.
 *
 * @author   Mark Taylor
 * @since    22 Feb 2013
 */
public abstract class ConfigKey<T> {

    private final ConfigMeta meta_;
    private final Class<T> clazz_;
    private final T dflt_;

    /**
     * Constructor.
     *
     * @param   meta  metadata describing this key
     * @param   clazz  value type for the values indexed by this key
     * @param   dflt  default value when key not present in map
     */
    public ConfigKey( ConfigMeta meta, Class<T> clazz, T dflt ) {
        meta_ = meta;
        clazz_ = clazz;
        dflt_ = dflt;
    }

    /**
     * Returns metadata about this key.
     *
     * @return  metadata
     */
    public ConfigMeta getMeta() {
        return meta_;
    }

    /**
     * Returns the type of value described by this key.
     *
     * @return  class
     */
    public Class<T> getValueClass() {
        return clazz_;
    }

    /**
     * Returns the default value associated with this key.
     * This value may be used when no value is explicitly supplied for this key.
     * 
     * @return   default value
     */
    public T getDefaultValue() {
        return dflt_;
    }

    /**
     * Converts an object to the value type of this key.
     *
     * @param  value untyped value
     * @return  typed value
     */
    public T cast( Object value ) {
        return clazz_.cast( value );
    }

    /**
     * Decodes a string value to the value type of this key.
     * An empty string should be interpreted as a null value,
     * but this may cause an exception if null is not a permissible
     * value for this key.
     *
     * @param   txt  string representation of value
     * @return   value
     */
    public abstract T stringToValue( String txt ) throws ConfigException;

    /**
     * Reports a value as a string.
     * If at all possible the roundtripping should be possible,
     * so <code>stringToValue(valueToString(v)).equals(v)</code>.
     * A null value, if permitted, should be represented as an empty string.
     *
     * @param   value   possible value associated with this key
     * @return  string representation
     */
    public abstract String valueToString( T value );

    /**
     * Constructs a graphical control with which the user can
     * specify a suitable value for association with this key.
     *
     * @return   new specifier
     */
    public abstract Specifier<T> createSpecifier();

    @Override
    public String toString() {
        return meta_.getShortName();
    }
}
