package uk.ac.starlink.topcat.plot2;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.SpecifierPanel;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Utility class containing zone id specifier factory implementations.
 *
 * @author   Mark Taylor
 * @since    5 Feb 2016
 */
public class ZoneSpecifiers {

    private static final ZoneId FIXED = new ZoneId( "FIXED" );

    /**
     * Private constructor prevents instantiation.
     */
    private ZoneSpecifiers() {
    }

    /**
     * Returns a factory whose getItem method dispenses null values.
     * Using this effectively prevents display of zone selectors,
     * so it is suitable for a single-zone context.
     *
     * @return    factory dispensing null zone id specifiers
     */
    public static Factory<Specifier<ZoneId>> createEmptyZoneSpecifierFactory() {
        return new Factory<Specifier<ZoneId>>() {
            public Specifier<ZoneId> getItem() {
                return null;
            }
        };
    }

    /**
     * Returns a factory whose getItem method always dispenses the same value.
     * This can be used in a single-zone plotting context, but will still
     * display the zone selector.  It's not very useful.
     *
     * @return  factory that always dispenses the same zone id specifier
     */
    public static Factory<Specifier<ZoneId>> createFixedZoneSpecifierFactory() {
        return new Factory<Specifier<ZoneId>>() {
            public Specifier<ZoneId> getItem() {
                return new SpecifierPanel<ZoneId>( false ) {
                    protected JComponent createComponent() {
                        return new JLabel( "Fixed" );
                    }
                    public ZoneId getSpecifiedValue() {
                        return FIXED;
                    }
                    public void setSpecifiedValue( ZoneId zid ) {
                        throw new UnsupportedOperationException();
                    }
                    public void submitReport( ReportMap report ) {
                    }
                };
            }
        };
    }

    /**
     * Returns a factory whose getItem method dispenses integer zone ids.
     * They can also be selected in the GUI by the user.
     * Optionally, the default value increments for each subsequent
     * call of getItem.
     *
     * @param  autoIncrement  true to force increment of default zone id
     *                        for each specifier in sequence
     * @return   factory dispensing integer-based zone ids
     */
    public static Factory<Specifier<ZoneId>>
            createIntegerZoneSpecifierFactory( final boolean autoIncrement ) {
        return new Factory<Specifier<ZoneId>>() {
            private int index_;
            public Specifier<ZoneId> getItem() {
                return new SpecifierPanel<ZoneId>( false ) {
                    private final SpinnerNumberModel model_ =
                        new SpinnerNumberModel( 0, Integer.MIN_VALUE,
                                                Integer.MAX_VALUE, 1 );
                    protected JComponent createComponent() {
                        if ( autoIncrement ) {
                            index_++;
                        }
                        model_.setValue( new Integer( index_ ) );
                        JComponent box = Box.createHorizontalBox();
                        box.add( new ShrinkWrapper( new JSpinner( model_ ) ) );
                        model_.addChangeListener( getChangeForwarder() );
                        return box;
                    }
                    public ZoneId getSpecifiedValue() {
                        return new ZoneId( (Integer) model_.getNumber() );
                    }
                    public void setSpecifiedValue( ZoneId zid ) {
                        model_.setValue( (Integer) zid.getValue() );
                    }
                    public void submitReport( ReportMap report ) {
                    }
                };
            }
        };
    }
}
