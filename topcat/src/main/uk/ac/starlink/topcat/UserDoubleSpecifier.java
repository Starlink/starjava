package uk.ac.starlink.topcat;

import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.ttools.plot2.BasicCaptioner;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.Orientation;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Scale;
import uk.ac.starlink.ttools.plot2.Tick;
import uk.ac.starlink.ttools.plot2.Ticker;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.ScaleConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.SpecifierPanel;
import uk.ac.starlink.util.Bi;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Component that allows the user to adjust a floating point value.
 * This implements the {@link uk.ac.starlink.ttools.plot2.config.Specifier}
 * interface since that's what's required here, though it's not
 * intended to be used like most other <code>Specifier</code> implementations
 * (for instance the component is much less compact).
 *
 * @author   Mark Taylor
 * @since    3 Jul 2025
 */
public class UserDoubleSpecifier extends SpecifierPanel<Double> {

    private final JSlider slider_;
    private final JTextField valueField_;
    private final JTextField loField_;
    private final JTextField hiField_;
    private final Specifier<Scale> scaleSpecifier_;
    private final JRadioButton sliderButton_;
    private final JRadioButton textButton_;
    private double dlo_;
    private double dhi_;
    private Scale scale_;
    private double dvalue_;

    private static final int ILO = 0;
    private static final int IHI = 10_000;
    private static final Captioner swingCaptioner_ = new BasicCaptioner();
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public UserDoubleSpecifier() {
        super( true );
        loField_ = new JTextField( 10 );
        hiField_ = new JTextField( 10 );
        scaleSpecifier_ =
            new ScaleConfigKey( new ConfigMeta( "Scale", "scale" ) )
           .createSpecifier();
        scaleSpecifier_.setSpecifiedValue( Scale.LINEAR );
        slider_ = new JSlider( ILO, IHI, ILO );
        ChangeListener changeForwarder = getChangeForwarder();
        ActionListener actionForwarder = getActionForwarder();
        slider_.addChangeListener( evt -> {
            setValue( getSliderValue() );
            updateTextDisplay();
            changeForwarder.stateChanged( evt );
        } );
        slider_.setPaintLabels( true );
        valueField_ = new JTextField( 15 );
        valueField_.addActionListener( evt -> {
            setValue( getTextValue() );
            updateTextDisplay();
            updateSliderDisplay();
            actionForwarder.actionPerformed( evt );
        } );
        loField_.addActionListener( evt -> sliderLimitsUpdated() );
        hiField_.addActionListener( evt -> sliderLimitsUpdated() );
        scaleSpecifier_.addActionListener( evt -> sliderScaleUpdated() );
        slider_.addComponentListener( new ComponentAdapter() {
            @Override
            public void componentResized( ComponentEvent evt ) {
                updateTickLabels();
                getComponent().revalidate();
            }
        } );
        sliderButton_ = new JRadioButton();
        textButton_ = new JRadioButton();
        ButtonGroup inputGrp = new ButtonGroup();
        inputGrp.add( sliderButton_ );
        inputGrp.add( textButton_ );
        sliderButton_.addChangeListener( evt -> entryMethodUpdated() );
        sliderButton_.setSelected( true );
    }

    protected JComponent createComponent() {
        Box limitsLine = Box.createHorizontalBox();
        limitsLine.add( new ShrinkWrapper( loField_ ) );
        limitsLine.add( new JLabel( " \u2014 " ) );  // emdash
        limitsLine.add( new ShrinkWrapper( hiField_ ) );
        LabelledComponentStack sliderStack = new LabelledComponentStack();
        sliderStack.addLine( "Range", limitsLine );
        sliderStack.addLine( "Scale", scaleSpecifier_.getComponent() );
        Box textBox = Box.createVerticalBox();
        textBox.add( new LineBox( "Value", new ShrinkWrapper( valueField_ ) ) );
        Box sliderBox = Box.createVerticalBox();
        sliderBox.add( sliderStack );
        sliderBox.add( Box.createVerticalStrut( 5 ) );
        sliderBox.add( slider_ );
        textBox.setBorder( AuxWindow.makeTitledBorder( "Entry" ) );
        sliderBox.setBorder( AuxWindow.makeTitledBorder( "Slider" ) );
        setSliderRange( 0, 1 );
        setValue( dlo_ );
        updateSliderDisplay();
        updateTextDisplay();
        GridBagLayout layer = new GridBagLayout();
        JPanel panel = new JPanel( layer );
        List<Bi<JRadioButton,JComponent>> entries = Arrays.asList(
            new Bi<JRadioButton,JComponent>( textButton_, textBox ),
            new Bi<JRadioButton,JComponent>( sliderButton_, sliderBox )
        );
        for ( int i = 0; i < entries.size(); i++ ) {
            JRadioButton butt = entries.get( i ).getItem1();
            JComponent box = entries.get( i ).getItem2();
            GridBagConstraints cons = new GridBagConstraints();
            cons.anchor = GridBagConstraints.NORTH;
            cons.gridx = 0;
            cons.gridy = i;
            panel.add( butt, cons );
            cons.gridx = 1;
            cons.weightx = 1.0;
            cons.anchor = GridBagConstraints.WEST;
            cons.fill = GridBagConstraints.HORIZONTAL;
            cons.insets = new Insets( 2, 2, 2, 2 );
            panel.add( box, cons );
        }
        return panel;
    }

    public Double getSpecifiedValue() {
        return Double.valueOf( dvalue_ );
    }

    public void setSpecifiedValue( Double value ) {
        double dval = value == null ? Double.NaN : value.doubleValue();
        if ( ! ( dval >= dlo_ && dval <= dhi_ ) && isSlider() ) {
            textButton_.setSelected( true );
            assert ! isSlider();
            entryMethodUpdated();
        }
        setValue( dval );
        updateSliderDisplay();
        updateTextDisplay();
    }

    /**
     * No-op.
     */
    public void submitReport( ReportMap report ) {
    }

    /**
     * Sets the current value for this specifier.
     * Any change to the recorded value should be effected by calling
     * this method.  It informs listeners, but does not attempt to
     * make any updates to the GUI of this component.
     *
     * @param  dval  new value
     */
    private void setValue( double dval ) {
        if ( dval != dvalue_ ) {
            dvalue_ = dval;
            fireAction();
        }
    }

    /**
     * Indicates whether the slider component is currently active.
     *
     * @return   true for slider active, false for text entry active
     */
    private boolean isSlider() {
        return sliderButton_.isSelected();
    }

    /**
     * Returns the currently selected slider scale option.
     *
     * @return  slider scale
     */
    private Scale getScale() {
        return scaleSpecifier_.getSpecifiedValue();
    }

    /**
     * Updates GUI components following a change to the radio buttons
     * that indicate whether the slider or text entry should be active.
     */
    private void entryMethodUpdated() {
        boolean isSlider = isSlider();
        boolean isText = ! isSlider;
        valueField_.setEnabled( isText );
        loField_.setEnabled( isSlider );
        hiField_.setEnabled( isSlider );
        scaleSpecifier_.getComponent().setEnabled( isSlider );
        slider_.setEnabled( isSlider );
        setValue( isSlider() ? getSliderValue() : getTextValue() );
        updateSliderDisplay();
        updateTextDisplay();
    }

    /**
     * Updates GUI components following a change to the values
     * entered into the lower or upper slider limit fields.
     */
    private void sliderLimitsUpdated() {
        Scale scale = getScale();
        double dval0 = getSliderValue();
        double lo = readNumber( loField_, dlo_ );
        double hi = readNumber( hiField_, dhi_ );
        if ( lo < hi && ( lo > 0 || !scale.isPositiveDefinite() ) ) {
            dlo_ = lo;
            dhi_ = hi;
        }
        else {
            loField_.setText( Double.toString( dlo_ ) );
            hiField_.setText( Double.toString( dhi_ ) );
        }
        setValue( Math.max( dlo_, Math.min( dhi_, dvalue_ ) ) );
        updateTickLabels();
        updateSliderDisplay();
        updateTextDisplay();
    }

    /**
     * Updates GUI components following a change to the slider scale selection.
     */
    private void sliderScaleUpdated() {
        Scale scale = scaleSpecifier_.getSpecifiedValue();
        double dval0 = getSliderValue();
        if ( scale.isPositiveDefinite() && dlo_ <= 0 ) {
            if ( dhi_ <= 1 ) {
                setSliderRange( 1, 10 );
            }
            else {
                setSliderRange( 1, dhi_ );
            }
        }
        setValue( Math.max( dlo_, Math.min( dhi_, dvalue_ ) ) );
        updateTickLabels();
        updateSliderDisplay();
        updateTextDisplay();
    }

    /**
     * Ensures that the slider display is consistent with the
     * current value, if possible.
     */
    private void updateSliderDisplay() {
        if ( dvalue_ >= dlo_ && dvalue_ <= dhi_ ) {
            slider_.setValue( valueToSlider( dvalue_ ) );
        }
    }

    /**
     * Ensures that the value entry field is consistent with the
     * current value.
     */
    private void updateTextDisplay() {
        String txt = valueField_.getText();
        try {
            if ( Double.parseDouble( txt ) == dvalue_ ) {
                return;
            }
        }
        catch ( NumberFormatException e ) {
        }
        valueField_.setText( Double.toString( dvalue_ ) );
    }

    /**
     * Returns the numeric value currently entered into the text field.
     *
     * @return  data value
     */
    private double getTextValue() {
        return readNumber( valueField_, dvalue_ );
    }

    /**
     * Returns the numeric value currently showing in the slider.
     *
     * @return  data value
     */
    private double getSliderValue() {
        int i0 = slider_.getValue();
        double d0 = sliderToValue( i0 );

        /* For cosmetic reasons, try to round the value to a round number
         * corresponding to the nearest pixel so that reporting the
         * value as text does not include spurious (and ugly) precision.
         * We do it by formatting the value using a pixel-sized value delta,
         * and turning that formatted value back into a number. */
        int npix = getSliderPixels();
        if ( npix > 10 ) {
            int iPixStep = ( IHI - ILO ) / npix;
            double dPixStep = Math.abs( sliderToValue( i0 + iPixStep ) - d0 );
            if ( dPixStep > 0 ) {
                String numstr = PlotUtil.formatNumber( d0, dPixStep );
                try {
                    return Double.parseDouble( numstr );
                }
                catch ( NumberFormatException e ) {
                    logger_.info( "Parse of formatted number failed: "
                                + numstr );
                }
            }
        }

        /* If something went wrong, it's OK to use the exact value. */
        return d0;
    }

    /**
     * Sets the values of the slider range limits and updates the
     * corresponding fields.
     * Does not ensure that the rest of the GUI is consistent.
     *
     * @param  dlo  new lower limit
     * @param  dhi  new upper limit
     */
    private void setSliderRange( double dlo, double dhi ) {
        dlo_ = dlo;
        dhi_ = dhi;
        loField_.setText( Double.toString( dlo ) );
        hiField_.setText( Double.toString( dhi ) );
    }

    /**
     * Returns the extent in pixels of the slider, or at least a
     * reasonable guess.
     *
     * @return   reasonable value for slider extent in pixels
     */
    private int getSliderPixels() {
        int npix = slider_.getOrientation() == JSlider.HORIZONTAL
                 ? slider_.getWidth()
                 : slider_.getHeight();

        /* If the reported value is zero, it's probably because it hasn't
         * been posted yet.  In that case use a plausible default. */
        return npix == 0 ? 200 : npix;
    }

    /**
     * Ensures that the displayed tick labels on the slider are
     * consistent with the rest of the GUI.
     */
    private void updateTickLabels() {
        Ticker ticker = getScale().getTicker();
        boolean withMinor = false;
        double crowding = 1.0;
        Tick[] ticks = ticker.getTicks( dlo_, dhi_, withMinor, swingCaptioner_,
                                        new Orientation[] { Orientation.X },
                                        getSliderPixels(), crowding )
                             .getTicks();
        Hashtable<Integer,JComponent> tickMap = new Hashtable<>();
        for ( Tick tick : ticks ) {
            tickMap.put( Integer.valueOf( valueToSlider( tick.getValue() ) ),
                         new JLabel( tick.getLabel().toText() ) );
        }
        slider_.setLabelTable( tickMap );
        slider_.repaint();
    }

    /**
     * Turns a slider position into a data value.
     *
     * @param  ival  slider position
     * @return  specifier value
     */
    private double sliderToValue( int ival ) {
        Scale scale = getScale();
        double f = ( ival - ILO ) / (double) ( IHI - ILO );
        double slo = scale.dataToScale( dlo_ );
        double shi = scale.dataToScale( dhi_ );
        double s = slo + f * ( shi - slo );
        return scale.scaleToData( s );
    }

    /**
     * Turns a data value into a slider value.
     *
     * @param  dval  specifier value
     * @return  slider position
     */
    private int valueToSlider( double dval ) {
        Scale scale = getScale();
        double slo = scale.dataToScale( dlo_ );
        double shi = scale.dataToScale( dhi_ );
        double s = scale.dataToScale( dval );
        double f = ( s - slo ) / ( shi - slo );
        f = Math.max( 0, Math.min( 1, f ) );
        return (int) Math.round( f * ( IHI - ILO ) + ILO );
    }

    /**
     * Reads a numeric value from a text field.
     *
     * @param  txtField  text field
     * @param  fallback   value to use if text field can't be parsed
     */
    private static double readNumber( JTextField txtField, double fallback ) {
        double value;
        try {
            value = Double.parseDouble( txtField.getText() );
        }
        catch ( NumberFormatException e ) {
            value = Double.NaN;
        }
        if ( Double.isNaN( value ) || Double.isInfinite( value ) ) {
            txtField.setText( Double.toString( fallback ) );
            value = fallback;
        }
        return value;
    }
}
