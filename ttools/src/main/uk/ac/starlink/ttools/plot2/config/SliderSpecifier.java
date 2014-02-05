package uk.ac.starlink.ttools.plot2.config;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;

/**
 * Double value specifier that uses a slider to choose a value in the
 * range betwen two given values.  Linear and logarithmic scaling are
 * available.  The slider can optionally be accompanied by a text entry field.
 *
 * @author   Mark Taylor
 * @since    13 Jan 2014
 */
public class SliderSpecifier extends SpecifierPanel<Double> {

    private final double lo_;
    private final double hi_;
    private final boolean log_;
    private final boolean txtOpt_;
    private final JSlider slider_;
    private final JTextField txtField_;
    private final JRadioButton sliderButton_;
    private final JRadioButton txtButton_;
    private static final int MIN = 0;
    private static final int MAX = 10000;

    /**
     * Constructs a specifier with just a slider.
     *
     * @param   lo   slider lower bound
     * @param   hi   slider upper bound
     * @param  log  true for logarithmic slider scale, false for linear
     */
    public SliderSpecifier( double lo, double hi, boolean log ) {
        this( lo, hi, log, false );
    }

    /**
     * Constructs a specifier with a slider and optionally a text entry field.
     *
     * @param   lo   slider lower bound
     * @param   hi   slider upper bound
     * @param  log  true for logarithmic slider scale, false for linear
     * @param  txtOpt  true to include a text entry option
     */
    public SliderSpecifier( double lo, double hi, boolean log,
                            boolean txtOpt ) {
        super( true );
        lo_ = lo;
        hi_ = hi;
        log_ = log;
        txtOpt_ = txtOpt;
        slider_ = new JSlider( MIN, MAX );
        txtField_ = new JTextField( 8 );
        sliderButton_ = new JRadioButton();
        txtButton_ = new JRadioButton();
        ButtonGroup bgrp = new ButtonGroup();
        bgrp.add( sliderButton_ );
        bgrp.add( txtButton_ );
        sliderButton_.setSelected( true );
    }

    protected JComponent createComponent() {
        JComponent line = Box.createHorizontalBox();
        line.add( sliderButton_ );
        line.add( slider_ );
        line.add( Box.createHorizontalStrut( 10 ) );
        line.add( txtButton_ );
        line.add( txtField_ );
        final ActionListener actionForwarder = getActionForwarder();
        ActionListener radioListener = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                updateInputState();
                actionForwarder.actionPerformed( evt );
            }
        };
        txtButton_.addActionListener( radioListener );
        sliderButton_.addActionListener( radioListener );
        updateInputState();
        slider_.addChangeListener( getChangeForwarder() );
        txtField_.addActionListener( actionForwarder );
        return txtOpt_ ? line : slider_;
    }

    public Double getSpecifiedValue() {
        if ( ! isSliderActive() ) {
            double dval = getTextValue();
            if ( ! Double.isNaN( dval ) ) {
                return dval;
            }
        }
        return getSliderValue();
    }

    public void setSpecifiedValue( Double dValue ) {
        if ( dValue != null ) {
            double dval = dValue.doubleValue();
            if ( isSliderActive() ) {
                slider_.setValue( unscale( dval ) );
            }
            else {
                txtField_.setText( Float.toString( (float) dval ) );
            }
        }
    }

    /**
     * Indicates whether the slider or the text field is the currently
     * selected input component.
     *
     * @return   true for slider, false for text field
     */
    public boolean isSliderActive() {
        return sliderButton_.isSelected();
    }

    /**
     * Configures programmatically whether the slider or the text field is
     * the currently selected input component.
     *
     * @param  isActive  true for slider, false for text field
     */
    public void setSliderActive( boolean isActive ) {
        sliderButton_.setSelected( isActive );
    }

    /**
     * Returns the value currently entered in the text field, regardless
     * of whether the text field or slider is currently active.
     *
     * @return  text field value as a double, may be NaN
     */
    public double getTextValue() {
        String txt = txtField_.getText();
        if ( txt != null && txt.trim().length() > 0 ) {
            try {
                return Double.parseDouble( txt.trim() );
            }
            catch ( NumberFormatException e ) {
                txtField_.setText( null );
            }
        }
        return Double.NaN;
    }

    /**
     * Returns the value currently represented by the slider,
     * regardless of whether the slider or text field is currently active.
     *
     * @return  slider value
     */
    public double getSliderValue() {
        return scale( slider_.getValue() );
    }

    /**
     * Called to ensure that the enabledness of the input components matches
     * the currently selected input component.
     */
    private void updateInputState() {
        boolean sliderActive = isSliderActive();
        slider_.setEnabled( sliderActive );
        txtField_.setEnabled( ! sliderActive );
    }

    /**
     * Turns a slider value into a specified value.
     *
     * @param  ival  slider position
     * @return  specifier value
     */
    private double scale( int ival ) {
        double f = ( ival - MIN ) / (double) ( MAX - MIN );
        return log_ ? lo_ * Math.pow( hi_ / lo_, f )
                    : lo_ + ( hi_ - lo_ ) * f;
    }

    /**
     * Turns a specified value into a slider value.
     *
     * @param  dval  specifier value
     * @return  slider position
     */
    private int unscale( double dval ) {
        double s = log_ ? Math.log( dval / lo_ ) / Math.log( hi_ / lo_ )
                        : ( dval - lo_ ) / ( hi_ - lo_ );
        return (int) Math.round( s * ( MAX - MIN ) ) + MIN;
    }
}
