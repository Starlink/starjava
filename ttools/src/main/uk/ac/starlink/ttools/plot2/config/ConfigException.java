package uk.ac.starlink.ttools.plot2.config;

/**
 * Exception thrown when a configuration input value is not suitable.
 *
 * @author   Mark Taylor
 * @since    26 Feb 2013
 */
public class ConfigException extends Exception {

    private final ConfigKey<?> key_;

    /**
     * Constructor with chained exception.
     *
     * @param  key  key whose value was being solicited when the error occurred
     * @param  msg  error message, may reference value but should not name key
     * @param  cause   chained exception, if any
     */
    public ConfigException( ConfigKey<?> key, String msg, Throwable cause ) {
        super( msg, cause );
        key_ = key;
    }

    /**
     * Constructor.
     *
     * @param  key  key whose value was being solicited when the error occurred
     * @param  msg  error message, may reference value but should not name key
     */
    public ConfigException( ConfigKey key, String msg ) {
        this( key, msg, null );
    }

    /**
     * Returns the key whose value this exception applies to.
     *
     * @return  config key
     */
    public ConfigKey<?> getConfigKey() {
        return key_;
    }
}
