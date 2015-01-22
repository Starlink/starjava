package uk.ac.starlink.ttools.plot2.config;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
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
    }
}
