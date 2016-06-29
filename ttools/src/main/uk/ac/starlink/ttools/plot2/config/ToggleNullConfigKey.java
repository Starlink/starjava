package uk.ac.starlink.ttools.plot2.config;

/**
 * Wraps a supplied ConfigKey to provide one that will also allow
 * selection of the null value.
 * In the GUI, this is represented with a toggle button
 * that when selected indicates null, and when not defers to the
 * usual specifier GUI.
 *
 * <p>This is intended for use with config keys that do not normally accept
 * null values.  If used with config keys that do permit null values, 
 * confusion may result.
 *
 * @author   Mark Taylor
 * @since    29 Jun 2016
 */
public class ToggleNullConfigKey<T> extends ConfigKey<T> {

    private final ConfigKey<T> baseKey_;
    private final String toggleLabel_;

    /**
     * Constructor.
     *
     * @param  baseKey  config key providing non-null-valued behaviour
     * @param  toggleLabel   GUI label for the toggle button selecting null
     * @param  toggleDflt   true if the default is null,
     *                      false if the default is that of the base key
     */
    public ToggleNullConfigKey( ConfigKey<T> baseKey, String toggleLabel,
                                boolean toggleDflt ) {
        super( baseKey.getMeta(), baseKey.getValueClass(),
               toggleDflt ? null : baseKey.getDefaultValue() );
        baseKey_ = baseKey;
        toggleLabel_ = toggleLabel;
    }

    @Override
    public String valueToString( T value ) {
        return value == null ? "" : baseKey_.valueToString( value );
    }

    @Override
    public T stringToValue( String txt ) throws ConfigException {
        return txt == null || txt.trim().length() == 0
             ? null
             : baseKey_.stringToValue( txt );
    }

    @Override
    public Specifier<T> createSpecifier() {
        return new ToggleSpecifier<T>( baseKey_.createSpecifier(), null,
                                       toggleLabel_ );
    }
}
