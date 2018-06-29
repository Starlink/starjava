package uk.ac.starlink.ttools.plot2.config;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportMap;

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
    private final boolean flip_;
    private final double resetVal_;
    private final TextOption txtOpt_;
    private final boolean resetOpt_;
    private final JSlider slider_;
    private final JButton resetButton_;
    private final JTextField txtField_;
    private final JRadioButton sliderButton_;
    private final JRadioButton txtButton_;
    private static final int MIN = 0;
    private static final int MAX = 10000;
    private static final boolean DISPLAY_TEXT = false;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.config" );

    /**
     * Constructs a specifier with minimal options.
     *
     * @param   lo   slider lower bound
     * @param   hi   slider upper bound
     * @param  log  true for logarithmic slider scale, false for linear
     * @param  reset  value reset button resets to, or NaN for no reset
     */
    public SliderSpecifier( double lo, double hi, boolean log, double reset ) {
        this( lo, hi, log, reset, false, TextOption.NONE );
    }

    /**
     * Constructs a specifier with all options.
     *
     * @param   lo   slider lower bound
     * @param   hi   slider upper bound
     * @param  log  true for logarithmic slider scale, false for linear
     * @param  reset  value reset button resets to, or NaN for no reset
     * @param  flip  true to make slider values increase right to left
     * @param  txtOpt  configures whether a text field should appear;
     *                 null means NONE
     */
    public SliderSpecifier( double lo, double hi, boolean log,
                            final double reset, boolean flip,
                            TextOption txtOpt ) {
        super( true );
        lo_ = lo;
        hi_ = hi;
        log_ = log;
        flip_ = flip;
        resetVal_ = reset;
        txtOpt_ = txtOpt == null ? TextOption.NONE : txtOpt;
        slider_ = DISPLAY_TEXT
                ? new TextDisplaySlider( MIN, MAX ) {
                      @Override
                      public String getDisplayValue() {
                          return Double.toString( getSliderValue() );
                      }
                      @Override
                      public Dimension getMinimumSize() {
                          return minWidth( super.getMinimumSize(), 100 );
                      }
                  }
                : new JSlider( MIN, MAX ) {
                      @Override
                      public Dimension getMinimumSize() {
                          return minWidth( super.getMinimumSize(), 100 );
                      }
                  };
        resetOpt_ = reset >= lo && reset <= hi;
        Action resetAct = new AbstractAction( null, ResourceIcon.ZERO ) {
            public void actionPerformed( ActionEvent evt ) {
                slider_.setValue( unscale( reset ) );
            }
        };
        resetAct.putValue( Action.SHORT_DESCRIPTION,
                           "Reset slider to default (" + resetVal_ + ")" );
        resetButton_ = new JButton( resetAct );
        resetButton_.setMargin( new Insets( 0, 0, 0, 0 ) );
        txtField_ = new JTextField( 8 ) {
            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
            @Override
            public Dimension getMinimumSize() {
                return minWidth( super.getMinimumSize(), 60 );
            }
        };
        sliderButton_ = new JRadioButton();
        txtButton_ = new JRadioButton();
        ButtonGroup bgrp = new ButtonGroup();
        bgrp.add( sliderButton_ );
        bgrp.add( txtButton_ );
        sliderButton_.setSelected( true );
    }

    protected JComponent createComponent() {
        JComponent line = Box.createHorizontalBox();
        if ( txtOpt_.hasTextField_ ) {
            line.add( sliderButton_ );
        }
        line.add( slider_ );
        if ( resetOpt_ ) {
            line.add( Box.createHorizontalStrut( 5 ) );
            line.add( resetButton_ );
        }
        if ( txtOpt_.hasTextField_ ) {
            line.add( Box.createHorizontalStrut( 10 ) );
            line.add( txtButton_ );
            line.add( txtField_ );
        }
        final ActionListener actionForwarder = getActionForwarder();
        ActionListener radioListener = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                updateInputState();
                actionForwarder.actionPerformed( evt );
            }
        };
        txtButton_.addActionListener( radioListener );
        sliderButton_.addActionListener( radioListener );
        final ChangeListener changeForwarder = getChangeForwarder();
        slider_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                updateInputState();
                changeForwarder.stateChanged( evt );
            }
        } );
        txtField_.addActionListener( actionForwarder );
        updateInputState();
        return line;
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
                txtField_.setCaretPosition( 0 );
            }
        }
    }

    public void submitReport( ReportMap report ) {
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
        int i0 = slider_.getValue();
        double d0 = scale( i0 );

        /* For cosmetic reasons, try to round the value to a round number
         * corresponding to the nearest pixel so that reporting the
         * value as text does not include spurious (and ugly) precision.
         * We do it by formatting the value using a pixel-sized value delta,
         * and turning that formatted value back into a number. */
        int npix = getSliderPixels();
        if ( npix > 10 ) {
            int iPixStep = ( slider_.getMaximum() - slider_.getMinimum() )
                         / npix;
            double dPixStep = Math.abs( scale( i0 + iPixStep ) - d0 );
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
     * Returns the slider component used by this specifier.
     *
     * @return  slider
     */
    public JSlider getSlider() {
        return slider_;
    }

    /**
     * Returns the text entry component used by this specifier.
     *
     * @return  text field
     */
    public JTextField getTextField() {
        return txtField_;
    }

    /**
     * Formats a value provided by this specifier for display.
     * The default implementation does something obvious,
     * but may be overridden by subclasses.
     *
     * @param   value  double value as provided by this specifier
     * @return   string representation for preseentation to the user
     */
    public String valueToString( double value ) {
        if ( Double.isNaN( value ) ) {
            return "";
        }
        else if ( value == (int) value ) {
            return Integer.toString( (int) value );
        }
        else {
            return Double.toString( value );
        }
    }

    /**
     * Called to ensure that the enabledness of the input components matches
     * the currently selected input component.
     */
    private void updateInputState() {
        boolean sliderActive = isSliderActive();
        slider_.setEnabled( sliderActive );
        resetButton_.setEnabled( sliderActive );
        txtField_.setEnabled( ! sliderActive );
        if ( txtOpt_.isEchoValue_ && sliderActive ) {
            txtField_.setText( valueToString( getSpecifiedValue() ) );
            txtField_.setCaretPosition( 0 );
        }
    }

    /**
     * Turns a slider value into a specified value.
     *
     * @param  ival  slider position
     * @return  specifier value
     */
    private double scale( int ival ) {
        double f = ( ival - MIN ) / (double) ( MAX - MIN );
        if ( flip_ ) {
            f = 1 - f;
        }
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
        if ( flip_ ) {
            s = 1 - s;
        }
        return (int) Math.round( s * ( MAX - MIN ) + MIN );
    }

    /**
     * Adjusts a dimension to ensure that its width is no smaller than
     * a given value.
     *
     * @param  size  input dimension
     * @param  minWidth   minimum acceptable size of width
     * @return  output dimension, width may have been adjusted
     */
    private static Dimension minWidth( Dimension size, int minWidth ) {
        return new Dimension( Math.max( minWidth, size.width ), size.height );
    }

    /**
     * Specifies whether and how a text display field should appear alongside
     * the slider for user entry.
     */
    public enum TextOption {

        /**
         * No text display field.
         * Only the slider is shown.
         */
        NONE( false, false ),

        /**
         * Text display option provided without echo.
         * The user may choose to enter the value as text,
         * but the specifier will not update the content of the text field.
         */
        ENTER( true, false ),

        /**
         * Text display option provided with echo.
         * The user may choose to enter the value as text,
         * and if the slider is active, its value will be reflected in the
         * content of the text field.
         */
        ENTER_ECHO( true, true );

        private final boolean hasTextField_;
        private final boolean isEchoValue_;

        /**
         * Constructor.
         *
         * @param  hasTextField  whether text entry field is provided
         * @param  isEchoValue   whether slider state is displayed in text field
         */
        TextOption( boolean hasTextField, boolean isEchoValue ) {
            hasTextField_ = hasTextField;
            isEchoValue_ = isEchoValue;
            assert hasTextField_ || ! isEchoValue_;
        }
    }
}
