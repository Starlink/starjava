package uk.ac.starlink.topcat;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.ttools.plot2.BasicCaptioner;
import uk.ac.starlink.ttools.plot2.BasicTicker;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.Orientation;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Tick;
import uk.ac.starlink.ttools.plot2.config.SpecifierPanel;
import uk.ac.starlink.util.Bi;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Component that allows the user to adjust an integer value.
 * This implements the {@link uk.ac.starlink.ttools.plot2.config.Specifier}
 * interface since that's what's required here, though it's not
 * intended to be used like most other <code>Specifier</code> implementations
 * (for instance the component is much less compact).
 *
 * @author   Mark Taylor
 * @since    4 Jul 2025
 */
public class UserIntSpecifier extends SpecifierPanel<Integer> {

    private final JSlider slider_;
    private final JTextField valueField_;
    private final Action incAct_;
    private final Action decAct_;
    private final JTextField loField_;
    private final JTextField hiField_;
    private final JRadioButton sliderButton_;
    private final JRadioButton textButton_;
    private int ilo_;
    private int ihi_;
    private int ivalue_;
    private static final Captioner swingCaptioner_ = new BasicCaptioner();

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public UserIntSpecifier() {
        super( true );
        ilo_ = 0;
        ihi_ = 10;
        loField_ = new JTextField( 10 );
        hiField_ = new JTextField( 10 );
        slider_ = new JSlider( ilo_, ihi_, ilo_ );
        ChangeListener changeForwarder = getChangeForwarder();
        ActionListener actionForwarder = getActionForwarder();
        slider_.addChangeListener( evt -> {
            setValue( getSliderValue() );
            updateTextDisplay();
            changeForwarder.stateChanged( evt );
        } );
        slider_.setPaintLabels( true );
        valueField_ = new JTextField( 10 );
        valueField_.addActionListener( evt -> {
            setValue( getTextValue() );
            updateTextDisplay();
            updateSliderDisplay();
            actionForwarder.actionPerformed( evt );
        } );
        loField_.addActionListener( evt -> sliderLimitsUpdated() );
        hiField_.addActionListener( eve -> sliderLimitsUpdated() );
        slider_.addComponentListener( new ComponentAdapter() {
            @Override
            public void componentResized( ComponentEvent evt ) {
                updateTickLabels();
                getComponent().revalidate();
            }
        } );
        incAct_ = createAddAction( +1, "+1", "Add one to value" );
        decAct_ = createAddAction( -1, "-1", "Subtract one from value" );
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
        Box textBox = Box.createHorizontalBox();
        JButton decButton = new JButton( decAct_ );
        JButton incButton = new JButton( incAct_ );
        textBox.add( new JLabel( "Value: " ) );
        textBox.add( new ShrinkWrapper( valueField_ ) );
        textBox.add( Box.createHorizontalStrut( 5 ) );
        textBox.add( new JButton( decAct_ ) );
        textBox.add( Box.createHorizontalStrut( 5 ) );
        textBox.add( new JButton( incAct_ ) );
        textBox.add( Box.createHorizontalGlue() );
        Box sliderBox = Box.createVerticalBox();
        sliderBox.add( sliderStack );
        sliderBox.add( Box.createVerticalStrut( 5 ) );
        sliderBox.add( slider_ );
        textBox.setBorder( AuxWindow.makeTitledBorder( "Entry" ) );
        sliderBox.setBorder( AuxWindow.makeTitledBorder( "Slider" ) );
        setSliderRange( 0, 10 );
        setValue( ilo_ );
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

    public Integer getSpecifiedValue() {
        return Integer.valueOf( ivalue_ );
    }

    public void setSpecifiedValue( Integer value ) {
        int ival = value.intValue();
        if ( ( ival < ilo_ || ival > ihi_ ) && isSlider() ) {
            textButton_.setSelected( true );
            assert ! isSlider();
            entryMethodUpdated();
        }
        setValue( ival );
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
     * @param  ival new value
     */
    private void setValue( int ival ) {
        if ( ival != ivalue_ ) {
            ivalue_ = ival;
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
     * Updates GUI components following a change to the radio buttons
     * that indicate whether the slider or text entry should be active.
     */
    private void entryMethodUpdated() {
        boolean isSlider = isSlider();
        boolean isText = ! isSlider;
        valueField_.setEnabled( isText );
        incAct_.setEnabled( isText );
        decAct_.setEnabled( isText );
        loField_.setEnabled( isSlider );
        hiField_.setEnabled( isSlider );
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
        int ival0 = getSliderValue();
        int lo = readNumber( loField_, ilo_ );
        int hi = readNumber( hiField_, ihi_ );
        if ( lo < hi ) {
            ilo_ = lo;
            ihi_ = hi;
        }
        else {
            loField_.setText( Integer.toString( ilo_ ) );
            hiField_.setText( Integer.toString( ihi_ ) );
        }
        slider_.setMinimum( ilo_ );
        slider_.setMaximum( ihi_ );
        setValue( Math.max( ilo_, Math.min( ihi_, ivalue_ ) ) );
        updateTickLabels();
        updateSliderDisplay();
        updateTextDisplay();
    }

    /**
     * Ensures that the slider display is consistent with the
     * current value, if possible.
     */
    private void updateSliderDisplay() {
        if ( ivalue_ >= ilo_ && ivalue_ <= ihi_ ) {
            slider_.setValue( ivalue_ );
        }
    }

    /**
     * Ensures that the value entry field is consistent with the
     * current value.
     */
    private void updateTextDisplay() {
        String txt = valueField_.getText();
        try {
            if ( Integer.parseInt( txt ) == ivalue_ ) {
                return;
            }
        }
        catch ( NumberFormatException e ) {
        }
        valueField_.setText( Integer.toString( ivalue_ ) );
    }

    /**
     * Returns the numeric value currently entered into the text field.
     *
     * @return  data value
     */
    private int getTextValue() {
        return readNumber( valueField_, ivalue_ );
    }

    /**
     * Returns the numeric value currently showing in the slider.
     *
     * @return  data value
     */
    private int getSliderValue() {
        return slider_.getValue();
    }

    /**
     * Sets the values of the slider range limits and updates the
     * corresponding fields.
     * Does not ensure that the rest of the GUI is consistent.
     *
     * @param  ilo  new lower limit
     * @param  ihi  new upper limit
     */
    private void setSliderRange( int ilo, int ihi ) {
        ilo_ = ilo;
        ihi_ = ihi;
        loField_.setText( Integer.toString( ilo ) );
        hiField_.setText( Integer.toString( ihi ) );
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
        boolean withMinor = false;
        double crowding = 1.0;
        Tick[] ticks = BasicTicker.LINEAR
                      .getTicks( ilo_, ihi_, withMinor, swingCaptioner_,
                                 new Orientation[] { Orientation.X },
                                 getSliderPixels(), crowding )
                             .getTicks();
        Hashtable<Integer,JComponent> tickMap = new Hashtable<>();
        for ( Tick tick : ticks ) {
            double tval = tick.getValue();
            if ( tval == (int) tval ) {
                int ival = (int) tval;
                tickMap.put( Integer.valueOf( ival ),
                             new JLabel( Integer.toString( ival ) ) );
            }
        }
        slider_.setLabelTable( tickMap );
        slider_.repaint();
    }

    /**
     * Returns a new action that will add a fixed value to the
     * currently displayed value.
     *
     * @param  increment  amount to add
     * @param  name   action name
     * @param  description  action tooltip
     * @return  new value increment action
     */
    private Action createAddAction( int increment, String name,
                                    String description ) {
        return BasicAction.create( name, null, description, evt -> {
            setValue( getTextValue() + increment );
            updateTextDisplay();
            updateSliderDisplay();
        } );
    }

    /**
     * Reads a numeric value from a text field.
     *
     * @param  txtField  text field
     * @param  fallback   value to use if text field can't be parsed
     */
    private static int readNumber( JTextField txtField, int fallback ) {
        int value;
        try {
            return Integer.parseInt( txtField.getText() );
        }
        catch ( NumberFormatException e ) {
            txtField.setText( Integer.toString( fallback ) );
            return fallback;
        }
    }
}
