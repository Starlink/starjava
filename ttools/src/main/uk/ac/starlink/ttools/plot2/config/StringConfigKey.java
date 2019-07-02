package uk.ac.starlink.ttools.plot2.config;

/**
 * Config key for use with String values.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2013
 */
public class StringConfigKey extends ConfigKey<String> {

    /**
     * Constructor.
     *
     * @param  meta  metadata
     * @param  dflt  default value
     */
    public StringConfigKey( ConfigMeta meta, String dflt ) {
        super( meta, String.class, dflt );
    }

    public String stringToValue( String txt ) {
        return txt;
    }

    public String valueToString( String value ) {
        return value;
    }

    public Specifier<String> createSpecifier() {
        return new TextFieldSpecifier<String>( this, null );
    }
}
