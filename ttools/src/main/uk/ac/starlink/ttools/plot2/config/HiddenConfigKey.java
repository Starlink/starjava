package uk.ac.starlink.ttools.plot2.config;

/**
 * ConfigKey implementation which is not available for manipulation in
 * the user interface.  UI-related methods throw an
 * UnsupportedOperationException.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2013
 */
public class HiddenConfigKey<T> extends ConfigKey<T> {

    /**
     * Constructor.
     *
     * @param  meta   metadata
     * @param  clazz   value class
     * @param  dflt  default value
     */
    public HiddenConfigKey( ConfigMeta meta, Class<T> clazz, T dflt ) {
        super( meta, clazz, dflt );
    }

    /**
     * Throws UnsupportedOperationException.
     */
    public Specifier<T> createSpecifier() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException.
     */
    public T stringToValue( String txt ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException.
     */
    public String valueToString( T value ) {
        return value.toString();
    }
}
