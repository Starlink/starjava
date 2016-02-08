package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.SpecifierPanel;

/**
 * Decorates a specifier with an Auto button.
 * This is a checkbox which if checked overrides the state of the
 * base specifier and returns an alternative externally supplied
 * value instead.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class AutoSpecifier<T> extends SpecifierPanel<T> {

    private final Specifier<T> base_;
    private final ToggleButtonModel autoModel_;
    private T autoValue_;

    /**
     * Constructor.
     *
     * @param  base  base specifier
     */
    public AutoSpecifier( Specifier<T> base ) {
        super( true );
        base_ = base;
        autoModel_ =
            new ToggleButtonModel( "Auto", null,
                                   "Use automatically generated value" );
    }

    protected JComponent createComponent() {
        final JComponent baseComponent = base_.getComponent();
        autoModel_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                boolean isAuto = autoModel_.isSelected();
                baseComponent.setEnabled( ! isAuto );
                if ( isAuto ) {
                    base_.setSpecifiedValue( autoValue_ );
                }
            }
        } );
        autoModel_.setSelected( true );
        ActionListener forwarder = getActionForwarder();
        autoModel_.addActionListener( forwarder );
        base_.addActionListener( forwarder );
        JComponent line = Box.createHorizontalBox();
        line.add( baseComponent );
        line.add( Box.createHorizontalStrut( 10 ) );
        line.add( autoModel_.createCheckBox() );
        return line;
    }

    /**
     * Sets the value specified when the auto button is on.
     *
     * @param  autoValue  new auto value
     */
    public void setAutoValue( T autoValue ) {
        autoValue_ = autoValue;
        if ( autoModel_.isSelected() ) {
            base_.setSpecifiedValue( autoValue );
        }
    }

    /**
     * Returns the value that will be returend when the auto button is on.
     *
     * @return  auto value
     */
    public T getAutoValue() {
        return autoValue_;
    }

    /**
     * Sets whether the auto button is on or off.
     *
     * @param  isAuto  true for automatic values
     */
    public void setAuto( boolean isAuto ) {
        autoModel_.setSelected( isAuto );
    }

    /**
     * Indicates whether the auto button is on or off.
     *
     * @return  true iff the auto value will be returned
     */
    public boolean isAuto() {
        return autoModel_.isSelected();
    }

    public T getSpecifiedValue() {
        return base_.getSpecifiedValue();
    }

    public void setSpecifiedValue( T value ) {
        base_.setSpecifiedValue( value );
        fireAction();
    }

    public void submitReport( ReportMap report ) {
        base_.submitReport( report );
    }
}
