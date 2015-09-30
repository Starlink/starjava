package uk.ac.starlink.ttools.plot2.config;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Config key for integer values.
 *
 * @author   Mark Taylor
 * @since    22 Feb 2013
 */
public abstract class IntegerConfigKey extends ConfigKey<Integer> {

    private final int dflt_;

    /**
     * Constructor.
     *
     * @param  meta  metadata
     * @param  dflt  default value
     */
    protected IntegerConfigKey( ConfigMeta meta, int dflt ) {
        super( meta, Integer.class, new Integer( dflt ) );
        if ( meta.getStringUsage() == null ) {
            meta.setStringUsage( "<int-value>" );
        }
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

    /**
     * Returns a config key that uses a JSpinner for the specifier.
     *
     * @param  meta  metadata
     * @param  dflt  default value
     * @param  lo    minimum value offered by spinner
     * @param  hi    maximum value offered by spinner
     */
    public static IntegerConfigKey createSpinnerKey( ConfigMeta meta, int dflt,
                                                     final int lo,
                                                     final int hi ) {
        return new IntegerConfigKey( meta, dflt ) {
            public Specifier<Integer> createSpecifier() {
                return new SpinnerSpecifier( lo, hi, 1 );
            }
        };
    }

    /**
     * Returns a config key that uses two JSpinners to specify either
     * a positive or a negative value.  This is a bit specialised
     * (currently used for SkyDensityPlotter), but might possibly be
     * useful in other contexts.
     *
     * @param  meta  metadata
     * @param  dflt  default value
     * @param  posLimit  maximum value (positive)
     * @param  negLimit  minimum value (negative)
     * @param  posLabel  label for positive value spinner
     * @param  negLabel  label for negative value spinner
     * @param  reportKey  key to report actual value used; may be null
     */
    public static IntegerConfigKey
            createSpinnerPairKey( ConfigMeta meta, int dflt,
                                  final int posLimit, final int negLimit,
                                  final String posLabel, final String negLabel,
                                  final ReportKey<Integer> reportKey ) {
        return new IntegerConfigKey( meta, dflt ) {
            public Specifier<Integer> createSpecifier() {
                return new SpinnerPairSpecifier( posLimit, negLimit,
                                                 posLabel, negLabel,
                                                 reportKey );
            }
        };
    }
 

    /**
     * Returns a config key that uses a SliderSpecifier.
     * Note that in case of log=true, you must not supply 0 for the lower value.
     *
     * @param  meta  metadata
     * @param  dflt  default value
     * @param  lo    minimum of slider range
     * @param  hi    maximum of slider range
     * @param  log   true for logarithmic scale, false for linear
     */
    public static IntegerConfigKey createSliderKey( ConfigMeta meta,
                                                    final int dflt,
                                                    final double lo,
                                                    final double hi,
                                                    final boolean log ) {
        return new IntegerConfigKey( meta, dflt ) {
            public Specifier<Integer> createSpecifier() {
                final Specifier<Double> slidey =
                    new SliderSpecifier( lo, hi, log, dflt );
                return new ConversionSpecifier<Double,Integer>( slidey ) {
                    protected Integer inToOut( Double dVal ) {
                        if ( dVal == null ) {
                            return null;
                        }
                        double dval = dVal.doubleValue();
                        return Double.isNaN( dval )
                             ? null
                             : new Integer( (int) Math.round( dval ) );
                    }
                    protected Double outToIn( Integer iVal ) {
                        return iVal == null ? null : iVal.doubleValue();
                    }
                };
            }
        };
    }

    /**
     * Specifier implementation that uses a JSpinner.
     */
    private static class SpinnerSpecifier extends SpecifierPanel<Integer> {

        private final JSpinner spinner_;

        /**
         * Constructor.
         *
         * @param   lo   minimum value offered by spinner
         * @param   hi   maximum value offered by spinner
         * @param   step  spinner step
         */
        SpinnerSpecifier( int lo, int hi, int step ) {
            super( false );
            spinner_ =
                new JSpinner( new SpinnerNumberModel( lo, lo, hi, step ) );
        }

        protected JComponent createComponent() {
            JComponent box = new Box( BoxLayout.X_AXIS ) {
                @Override
                public void setEnabled( boolean enabled ) {
                    super.setEnabled( enabled );
                    spinner_.setEnabled( enabled );
                }
            };
            box.add( new ShrinkWrapper( spinner_ ) );
            spinner_.addChangeListener( getChangeForwarder() );
            return box;
        }

        public Integer getSpecifiedValue() {
            return new Integer( ((Number) spinner_.getValue()).intValue() );
        }

        public void setSpecifiedValue( Integer value ) {
            spinner_.setValue( value );
            fireAction();
        }

        public void submitReport( ReportMap report ) {
        }
    }

    /**
     * Specifier that uses two JSpinners, one for positive values and one
     * for negative.  Radio buttons indicate which is active.
     */
    private static class SpinnerPairSpecifier extends SpecifierPanel<Integer> {

        private final JSpinner posSpinner_;
        private final JSpinner negSpinner_;
        private final JRadioButton posButt_;
        private final JRadioButton negButt_;
        private final ReportKey<Integer> reportKey_;
        private final JLabel reportLabel_;

        /**
         * Constructor.
         *
         * @param  posLimit  maximum value (positive)
         * @param  negLimit  minimum value (negative)
         * @param  posLabel  label for positive value spinner
         * @param  negLabel  label for negative value spinner
         * @param  reportKey  key to report actual value used; may be null
         */
        public SpinnerPairSpecifier( int posLimit, int negLimit,
                                     String posLabel, String negLabel,
                                     ReportKey<Integer> reportKey ) {
            super( false );
            posSpinner_ =
                new JSpinner( new SpinnerNumberModel( 0, 0, posLimit, 1 ) );
            negSpinner_ =
                new JSpinner( new SpinnerNumberModel( -1, negLimit, -1, 1 ) );
            posButt_ = new JRadioButton( posLabel );
            negButt_ = new JRadioButton( negLabel );
            reportKey_ = reportKey;
            reportLabel_ = new JLabel();
            reportLabel_.setFont( posSpinner_.getFont() );
            ButtonGroup bgrp = new ButtonGroup();
            bgrp.add( posButt_ );
            bgrp.add( negButt_ );
            posButt_.setSelected( true );
        }

        protected JComponent createComponent() {
            posSpinner_.addChangeListener( getChangeForwarder() );
            negSpinner_.addChangeListener( getChangeForwarder() );
            ActionListener radioListener = new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    updateState();
                    getActionForwarder().actionPerformed( evt );
                }
            };
            posButt_.addActionListener( radioListener );
            negButt_.addActionListener( radioListener );
            JComponent line = Box.createHorizontalBox();
            line.add( posButt_ );
            line.add( new ShrinkWrapper( posSpinner_ ) );
            line.add( Box.createHorizontalStrut( 5 ) );
            line.add( negButt_ );
            line.add( new ShrinkWrapper( negSpinner_ ) );
            if ( reportKey_ != null ) {
                line.add( Box.createHorizontalStrut( 10 ) );
                line.add( reportLabel_ );
            }
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    updateState();
                }
            } );
            return line;
        }

        public Integer getSpecifiedValue() {
            int value = posButt_.isSelected()
                      ? ((Number) posSpinner_.getValue()).intValue()
                      : ((Number) negSpinner_.getValue()).intValue();
            return new Integer( value );
        }

        public void setSpecifiedValue( Integer value ) {
            if ( value >= 0 ) {
                posButt_.setSelected( true );
                posSpinner_.setValue( new Integer( value ) );
            }
            else {
                negButt_.setSelected( true );
                negSpinner_.setValue( new Integer( value ) );
            }
            fireAction();
        }

        public void submitReport( ReportMap report ) {
            if ( reportKey_ != null && report != null ) {
                Integer value = report.get( reportKey_ );
                if ( value != null ) {
                    reportLabel_.setText( value.toString() );
                }
            }
        }

        /**
         * Updates enabledness of components according to current state.
         */
        private void updateState() {
            JComponent comp = getComponent();
            boolean isEnabled = comp == null || comp.isEnabled();
            posSpinner_.setEnabled( isEnabled && posButt_.isSelected() );
            negSpinner_.setEnabled( isEnabled && negButt_.isSelected() );
        }
    }
}
