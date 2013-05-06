package uk.ac.starlink.ttools.plot2.config;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Config key for integer values.
 * Currently uses a spinner for the GUI.
 *
 * @author   Mark Taylor
 * @since    22 Feb 2013
 */
public class IntegerConfigKey extends ConfigKey<Integer> {

    private final int min_;
    private final int max_;
    private final int dflt_;

    /**
     * Constructor.
     *
     * <p>Note the min and max values configure only the spinner range,
     * they do not enforce a range when the value is set from a string value.
     *
     * @param  meta  metadata
     * @param  min   minimum
     * @param  max   maximum
     * @param  dflt  default value
     */
    public IntegerConfigKey( ConfigMeta meta, int min, int max, int dflt ) {
        super( meta, Integer.class, new Integer( dflt ) );
        min_ = min;
        max_ = max;
        dflt_ = dflt;
    }

    public Integer stringToValue( String txt ) throws ConfigException {
        try {
            return Integer.decode( txt );
        }
        catch ( NumberFormatException e ) {
            throw new ConfigException( this,
                                       "\"" + txt + "\" not an integer", e );
        }
    }

    public String valueToString( Integer value ) {
        return value.toString();
    }

    public Specifier<Integer> createSpecifier() {
        final JSpinner spinner =
            new JSpinner( new SpinnerNumberModel( dflt_, min_, max_, 1 ) );
        return new SpecifierPanel<Integer>( false ) {
            protected JComponent createComponent() {
                JComponent box = new Box( BoxLayout.X_AXIS ) {
                    @Override
                    public void setEnabled( boolean enabled ) {
                        super.setEnabled( enabled );
                        spinner.setEnabled( enabled );
                    }
                };
                box.add( new ShrinkWrapper( spinner ) );
                spinner.addChangeListener( getChangeForwarder() );
                return box;
            }
            public Integer getSpecifiedValue() {
                return new Integer( ((Number) spinner.getValue())
                          .intValue() );
            }
            public void setSpecifiedValue( Integer value ) {
                spinner.setValue( value );
                fireAction();
            }
        };
    }
}
