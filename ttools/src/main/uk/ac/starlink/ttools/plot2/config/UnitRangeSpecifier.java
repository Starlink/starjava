package uk.ac.starlink.ttools.plot2.config;

import com.jidesoft.swing.RangeSlider;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Subrange;

/**
 * Specifier for acquiring range for which both bounds fall between 0 and 1.
 * The range may be of zero extent, in which case the result can be
 * specified as a single value rather than a pair.
 *
 * @author   Mark Taylor
 * @since    15 Dec 2016
 */
public class UnitRangeSpecifier extends SpecifierPanel<Subrange> {

    private static final int NSTEP = 1000;
    private final RangeSlider slider_;
    private final JButton resetButton_;
    private final JTextField loField_;
    private final JTextField hiField_;
    private final JRadioButton sliderButton_;
    private final JRadioButton txtButton_;

    /**
     * Constructor.
     *
     * @param  reset  reset (default) value
     */
    public UnitRangeSpecifier( final Subrange reset ) {
        super( true );
        slider_ = new RangeSlider( 0, NSTEP ) {
            @Override
            public Dimension getMinimumSize() {
                Dimension s = super.getMinimumSize();
                return new Dimension( Math.max( s.width, 128 ), s.height );
            }
        };
        setSliderRange( reset );
        Action resetAct = new AbstractAction( null, ResourceIcon.ZERO ) {
            public void actionPerformed( ActionEvent evt ) {
                setSliderRange( reset );
            }
        };
        resetAct.putValue( Action.SHORT_DESCRIPTION,
                           "Reset slider to default (" + reset + ")" );
        resetButton_ = new JButton( resetAct );
        resetButton_.setMargin( new Insets( 0, 0, 0, 0 ) );
        JTextField[] fields = new JTextField[ 2 ];
        for ( int i = 0; i < 2; i++ ) {
            fields[ i ] = new JTextField( 4 ) {
                @Override
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
        }
        loField_ = fields[ 0 ];
        hiField_ = fields[ 1 ];
        sliderButton_ = new JRadioButton();
        txtButton_ = new JRadioButton();
        ButtonGroup bgrp = new ButtonGroup();
        bgrp.add( sliderButton_ );
        bgrp.add( txtButton_ );
        sliderButton_.setSelected( true );
    }

    public JComponent createComponent() {
        JComponent line = Box.createHorizontalBox();
        line.add( sliderButton_ );
        line.add( slider_ );
        line.add( Box.createHorizontalStrut( 5 ) );
        line.add( txtButton_ );
        line.add( loField_ );
        line.add( Box.createHorizontalStrut( 2 ) );
        line.add( hiField_ );
        line.add( Box.createHorizontalStrut( 5 ) );
        line.add( resetButton_ );
        final ChangeListener changeForwarder = getChangeForwarder();
        final ActionListener actionForwarder = getActionForwarder();
        slider_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                updateInputState();
                changeForwarder.stateChanged( evt );
            }
        } );
        loField_.addActionListener( actionForwarder );
        hiField_.addActionListener( actionForwarder );
        ActionListener radioListener = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                updateInputState();
                actionForwarder.actionPerformed( evt );
            }
        };
        sliderButton_.addActionListener( radioListener );
        txtButton_.addActionListener( radioListener );
        txtButton_.addActionListener( radioListener );
        updateInputState();
        return line;
    }

    public Subrange getSpecifiedValue() {
        if ( ! isSliderActive() ) {
            Subrange range = getTextValue();
            if ( range != null ) {
                return range;
            }
        }
        return getSliderValue();
    }

    public void setSpecifiedValue( Subrange range ) {
        if ( range != null ) {
            if ( isSliderActive() ) {
                setSliderRange( range );
            }
            else {
                setTextRange( range );
            }
        }
    }

    public void submitReport( ReportMap report ) {
    }

    /**
     * Returns true if the GUI is currently using the slider control,
     * false if it's using the text fields.
     *
     * @return   true iff slider is activated
     */
    public boolean isSliderActive() {
        return sliderButton_.isSelected();
    }

    /**
     * Sets the GUI to use the slider rather than the text fields
     * to acquire values.
     *
     * @param  isActive  true for slider, false for text
     */
    public void setSliderActive( boolean isActive ) {
        sliderButton_.setSelected( isActive );
    }

    /**
     * Called to ensure that the enabledness of the input components matches
     * the currently selected input component.
     */
    private void updateInputState() {
        boolean sliderActive = isSliderActive();
        slider_.setEnabled( sliderActive );
        resetButton_.setEnabled( sliderActive );
        loField_.setEnabled( ! sliderActive );
        hiField_.setEnabled( ! sliderActive );
        Subrange value = getSpecifiedValue();
        if ( sliderActive ) {
            setTextRange( value );
        }
        else {
            setSliderRange( value );
        }
    }

    /**
     * Returns the range value indicated by the curent state of the slider
     * control.
     *
     * @return   subrange according to slider
     */
    private Subrange getSliderValue() {
        return new Subrange( scale( slider_.getLowValue() ),
                             scale( slider_.getHighValue() ) );
    }

    /**
     * Returns the range value indicated by the current state of the text
     * entry fields.
     *
     * @return  subrange according to text fields
     */
    private Subrange getTextValue() {
        double lo = unformatFraction( loField_.getText() );
        double hi = unformatFraction( hiField_.getText() );
        if ( Double.isNaN( hi ) ) {
            hi = lo;
        }
        if ( ! ( lo >= 0 && lo <= 1 ) ) {
            lo = 0;
            loField_.setText( formatFraction( lo ) );
        }
        if ( ! ( hi >= lo && hi <= 1 ) ) {
            hi = 1;
            hiField_.setText( lo == hi ? null : formatFraction( hi ) );
        }
        return new Subrange( lo, hi );
    }

    /**
     * Sets the state of the slider control to a given subrange value.
     *
     * @param  range  range value
     */
    private void setSliderRange( Subrange range ) {
        slider_.setLowValue( unscale( range.getLow() ) );
        slider_.setHighValue( unscale( range.getHigh() ) );
    }

    /**
     * Sets the state of the text entry fields to a given subrange value.
     *
     * @param  range  range value
     */
    private void setTextRange( Subrange range ) {
        double lo = range.getLow();
        double hi = range.getHigh();
        loField_.setText( formatFraction( lo ) );
        hiField_.setText( lo == hi ? null : formatFraction( hi ) );
    }

    /**
     * Turns a slider value into a data fraction.
     *
     * @param  ivalue  slider position
     * @return   data value
     */
    private double scale( int ivalue ) {
        return ivalue * 1.0 / NSTEP;
    }

    /**
     * Returns a data fraction into a slider value.
     *
     * @param   dvalue  data value
     * @return   slider position
     */
    private int unscale( double dvalue ) {
        return (int) Math.round( dvalue * NSTEP );
    }

    /**
     * Formats a data fraction value for display in the text fields.
     *
     * @param   value  data value
     * @return   presentation string
     */
    private String formatFraction( double value ) {
        if ( value == 0 ) {
            return "0";
        }
        else if ( value == 1 ) {
            return "1";
        }
        else {
            return String.format( "%1$5.3f", value );
        }
    }

    /**
     * Parses a text string as a data fraction scalar.
     *
     * @param   txt  text
     * @return   data value or NaN
     */
    private double unformatFraction( String txt ) {
        try {
            return Double.parseDouble( txt );
        }
        catch ( RuntimeException e ) {
            return Double.NaN;
        }
    }
}
