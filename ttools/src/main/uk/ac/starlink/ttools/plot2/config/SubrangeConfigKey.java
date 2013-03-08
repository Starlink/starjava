package uk.ac.starlink.ttools.plot2.config;

import com.jidesoft.swing.RangeSlider;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JPanel;
import uk.ac.starlink.ttools.plot2.Subrange;

/**
 * Config key that specifies a Subrange.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2013
 */
public class SubrangeConfigKey extends ConfigKey<Subrange> {

    /**
     * Constructs a key with a given default.
     *
     * @param   meta  metadata
     * @param   dflt  default subrange
     */
    public SubrangeConfigKey( ConfigMeta meta, Subrange dflt ) {
        super( meta, Subrange.class, dflt );
    }

    /**
     * Constructs a key with the usual default.
     * The default subrange covers the whole range 0..1.
     * 
     * @param  meta  metadata
     */
    public SubrangeConfigKey( ConfigMeta meta ) {
        this( meta, new Subrange() );
    }

    public String valueToString( Subrange value ) {
        return format( value.getLow(), 3 ) + "," + format( value.getHigh(), 3 );
    }

    public Subrange stringToValue( String txt ) {
        String[] limits = txt.split( "," );
        if ( limits.length == 2 ) {
            String slo = limits[ 0 ].trim();
            String shi = limits[ 1 ].trim();
            try {
                double lo = slo.length() > 0 ? Double.parseDouble( slo ) : 0;
                double hi = shi.length() > 0 ? Double.parseDouble( shi ) : 1;
                if ( lo >= 0 && lo <= hi && hi <= 1 ) {
                    return new Subrange( lo, hi );
                }
                else {
                    throw new ConfigException( this,
                                               "0 <= lo <= hi <= 1 violated" );
                }
            }
            catch ( NumberFormatException e ) {
                throw new ConfigException( this,
                                           "Bad number(s): \"" + slo + "\","
                                                       + " \"" + shi + "\"" );
            }
        }
        else {
            throw new ConfigException( this,
                                       "Should be two numbers "
                                     + "separated by a comma" );
        }
    }

    public Specifier<Subrange> createSpecifier() {
        return new SubrangeSpecifier();
    }

    /**
     * Formats a subrange limit number for display.
     *
     * @param  dval  value
     * @param  nf  number of significant figures
     * @return  formatted value
     */
    private static String format( double dval, int nf ) {
        assert dval >= 0 && dval <= 1;
        int m10 = (int) Math.round( Math.pow( 10, nf ) );
        int mf = (int) Math.round( dval * m10 );
        if ( mf <= 0 ) {
            return "0";
        }
        else if ( mf >= m10 ) {
            return "1";
        }
        else {
            return "0." + String.format( "%0" + nf + "d", mf );
        }
    }

    /**
     * Specifier that uses a double slider component.
     */
    private static class SubrangeSpecifier extends SpecifierPanel<Subrange> {
        private final RangeSlider slider_;
        private static final int MIN = 0;
        private static final int MAX = 10000;

        /**
         * Constructor.
         */
        SubrangeSpecifier() {
            super( true );
            slider_ = new RangeSlider( MIN, MAX );
            slider_.addChangeListener( getChangeForwarder() );
        }

        protected JComponent createComponent() {
            return slider_;
        }

        public Subrange getSpecifiedValue() {
            return new Subrange( scale( slider_.getLowValue() ),
                                 scale( slider_.getHighValue() ) );
        }

        public void setSpecifiedValue( Subrange subrange ) {
            slider_.setLowValue( unscale( subrange.getLow() ) );
            slider_.setHighValue( unscale( subrange.getHigh() ) );
        }

        private static double scale( int ival ) {
            return ( ival - MIN ) / (double) ( MAX - MIN );
        }

        private static int unscale( double dval ) {
            return (int) Math.round( dval * ( MAX - MIN ) ) + MIN;
        }
    }
}
