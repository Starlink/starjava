package uk.ac.starlink.ttools.plot2.config;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import uk.ac.starlink.ttools.plot2.ReportMap;

/**
 * SpecifierPanel subclass that uses a text field for input.
 *
 * @author   Mark Taylor
 * @since    17 Jul 2013
 */
public class TextFieldSpecifier<V> extends SpecifierPanel<V> {

    private final ConfigKey<V> key_;
    private final JTextField txtField_;
    private V badValue_;

    /**
     * Constructor.
     *
     * @param  key  config key for which this panel will gather values
     * @param  badValue  value to be returned if a ConfigException is
     *                   encountered when parsing the entered text
     */
    public TextFieldSpecifier( ConfigKey<V> key, V badValue ) {
        super( true );
        key_ = key;
        badValue_ = badValue;
        txtField_ = new JTextField();
        V dflt = key.getDefaultValue();
        if ( dflt != null ) {
            txtField_.setText( key.valueToString( dflt ) );
        }
    }

    protected JComponent createComponent() {
        txtField_.addActionListener( getActionForwarder() );
        return txtField_;
    }

    public V getSpecifiedValue() {
        try {
            return key_.stringToValue( txtField_.getText() );
        }
        catch ( ConfigException e ) {
            JOptionPane.showMessageDialog( txtField_, e.getMessage(),
                                           "Bad Value",
                                           JOptionPane.ERROR_MESSAGE );
            txtField_.setText( "" );
            return null;
        }
    }

    public void setSpecifiedValue( V value ) {
        txtField_.setText( key_.valueToString( value ) );
        fireAction();
    }

    public void submitReport( ReportMap report ) {
    }
}
