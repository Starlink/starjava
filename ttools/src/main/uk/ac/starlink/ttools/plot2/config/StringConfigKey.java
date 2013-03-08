package uk.ac.starlink.ttools.plot2.config;

import javax.swing.JComponent;
import javax.swing.JTextField;

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
        final String dflt = getDefaultValue();
        return new SpecifierPanel<String>( true ) {
            private final JTextField txtField_;
            /** Constructor. */ {
                txtField_ = new JTextField();
                if ( dflt != null ) {
                    txtField_.setText( dflt );
                }
                txtField_.addActionListener( getActionForwarder() );
            }
            protected JComponent createComponent() {
                return txtField_;
            }
            public String getSpecifiedValue() {
                return txtField_.getText();
            }
            public void setSpecifiedValue( String value ) {
                txtField_.setText( value );
                fireAction();
            }
        };
    }
}
