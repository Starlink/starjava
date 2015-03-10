package uk.ac.starlink.ttools.plot2.config;

import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Specifier that decorates another one with a toggle button for
 * selection of a fixed value.
 *
 * @author   Mark Taylor
 * @since    5 Mar 2013
 */
public class ToggleSpecifier<T> extends SpecifierPanel<T> {

    private final Specifier<T> base_;
    private final T toggleValue_;
    private final JCheckBox checkBox_;

    /**
     * Constructor.
     *
     * @param   base   base specifier
     * @param   toggleValue  value to yield if the toggle is set
     * @param   label   text to label the toggle button with
     */
    public ToggleSpecifier( Specifier<T> base, T toggleValue, String label ) {
        super( true );
        base_ = base;
        toggleValue_ = toggleValue;
        checkBox_ = new JCheckBox( label );
    }

    protected JComponent createComponent() {
        final JComponent baseComponent = base_.getComponent();
        checkBox_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                baseComponent.setEnabled( ! checkBox_.isSelected() );
            }
        } );
        ActionListener forwarder = getActionForwarder();
        checkBox_.addActionListener( forwarder );
        base_.addActionListener( forwarder );
        JComponent line = Box.createHorizontalBox();
        line.add( baseComponent );
        line.add( Box.createHorizontalStrut( 10 ) );
        line.add( checkBox_ );
        return line;
    }

    public T getSpecifiedValue() {
        return checkBox_.isSelected() ? toggleValue_
                                      : base_.getSpecifiedValue();
    }

    public void setSpecifiedValue( T value ) {
        boolean isTog = PlotUtil.equals( value, toggleValue_ );
        checkBox_.setSelected( isTog );
        base_.getComponent().setEnabled( ! isTog );
        if ( ! isTog ) {
            base_.setSpecifiedValue( value );
        }
        fireAction();
    }

    public void submitReport( ReportMap report ) {
        base_.submitReport( report );
    }
}
