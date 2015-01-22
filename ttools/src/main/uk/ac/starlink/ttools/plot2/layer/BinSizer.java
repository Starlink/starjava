package uk.ac.starlink.ttools.plot2.layer;

import java.util.logging.Logger;
import javax.swing.JComponent;
import uk.ac.starlink.ttools.plot.Rounder;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.SliderSpecifier;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.SpecifierPanel;

/**
 * Determines 1-d histogram bin widths from data bounds.
 *
 * @author   Mark Taylor
 * @since    8 Jan 2014
 */
@Equality
public abstract class BinSizer {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.layer" );
    public static final ConfigKey<BinSizer> BINSIZER_KEY =
        new BinSizerConfigKey(
            new ConfigMeta( "binsize", "Bin Size" )
           .setStringUsage( "+<width>|-<count>" )
           .setShortDescription( "Bin size specification" )
           .setXmlDescription( new String[] {
                "<p>Configures the width of histogram bins.",
                "If the supplied string is a positive number,",
                "it is interpreted as a fixed width in the data coordinates",
                "of the X axis",
                "(if the X axis is logarithmic, the value is a fixed factor).",
                "If it is a negative number, then it will be interpreted",
                "as the approximate number of bins to display across",
                "the width of the plot",
                "(though an attempt is made to use only round numbers",
                "for bin widths).",
                "</p>",
                "<p>When setting this value graphically,",
                "you can use either the slider to adjust the bin count",
                "or the numeric entry field to fix the bin width.",
                "</p>",
            } )
        , 20 );

    /**
     * Provides a bin width value for a given axis data range.
     *
     * @param  xlog  false for linear scaling, true for logarithmic
     * @param  xlo   axis lower bound
     * @param  xhi   axis upper bound
     * @return   additive/multiplicative bin width appropriate for the
     *           given range
     */
    public abstract double getWidth( boolean xlog, double xlo, double xhi );

    /**
     * Returns a bin sizer instance which always returns the same fixed
     * value.
     *
     * @param  binWidth  fixed bin width
     * @return  bin sizer
     */
    public static BinSizer createFixedBinSizer( double binWidth ) {
        return new FixedBinSizer( binWidth );
    }

    /**
     * Returns a bin sizer instance which divides the axis range up into
     * a fixed number of equal intervals.  If the rounding flag is true,
     * the number is approximate, and bin widths returned are round numbers.
     *
     * @param   nbin   number of intervals to divide the axis into
     * @param   rounding   if true, only round numbers are returned
     * @return  bin sizer instance
     */
    public static BinSizer createCountBinSizer( double nbin,
                                                boolean rounding ) {
        return new CountBinSizer( nbin, rounding );
    }

    /**
     * BinSizer implementation that always returns a fixed value.
     */
    private static class FixedBinSizer extends BinSizer {
        private final double binWidth_;

        /**
         * Constructor.
         *
         * @param  binWidth  fixed bin width
         */
        FixedBinSizer( double binWidth ) {
            binWidth_ = binWidth;
        }

        public double getWidth( boolean xlog, double xlo, double xhi ) {
            return binWidth_;
        }

        @Override
        public int hashCode() {
            return Float.floatToIntBits( (float) binWidth_ );
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof FixedBinSizer ) {
                FixedBinSizer other = (FixedBinSizer) o;
                return this.binWidth_ == other.binWidth_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * BinSizer implementation that chops the data range
     * up into a fixed number of equal intervals, with optional rounding
     * of the resulting width.
     */
    private static class CountBinSizer extends BinSizer {
        private final double nbin_;
        private final boolean rounding_;

        /**
         * Constructor.
         *
         * @param  nbin  number of intervals
         * @param  rounding  true to round the output bin widths to sensible
         *                   values
         */
        CountBinSizer( double nbin, boolean rounding ) {
            nbin_ = nbin;
            rounding_ = rounding;
        }

        public double getWidth( boolean xlog, double xlo, double xhi ) {
            double width0 = xlog ? Math.exp( Math.log( xhi / xlo ) / nbin_ )
                                 : ( xhi - xlo ) / nbin_;
            if ( rounding_ ) {
                Rounder rounder = xlog ? Rounder.LOG : Rounder.LINEAR;
                return rounder.round( width0 );
            }
            else {
                return width0;
            }
        }

        @Override
        public int hashCode() {
            int code = 44301;
            code = 23 * code + Float.floatToIntBits( (float) nbin_ );
            code = 23 * code + ( rounding_ ? 5 : 7 );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof CountBinSizer ) {
                CountBinSizer other = (CountBinSizer) o;
                return this.nbin_ == other.nbin_
                    && this.rounding_ == other.rounding_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Config key implementation for a bin sizer object.
     * The string representation may be either a positive floating point value
     * giving bin width, or a negative value giving the approximate
     * number of bins visible.
     * For the graphical part, a slider is used to allow selection
     * of the approximate number of bins visible.
     */
    private static class BinSizerConfigKey extends ConfigKey<BinSizer> {

        private final int dfltNbin_;

        /**
         * Constructor.
         *
         * @param   meta  key metadata
         * @parma   dlftNbin  default bin count
         */
        BinSizerConfigKey( ConfigMeta meta, int dfltNbin ) {
            super( meta, BinSizer.class, new CountBinSizer( dfltNbin, true ) );
            dfltNbin_ = dfltNbin;
        }

        public BinSizer stringToValue( String txt ) {
            double dval;
            try {
                dval = Double.valueOf( txt.trim() );
            }
            catch ( NumberFormatException e ) {
                throw new ConfigException( this,
                                           "\"" + txt + "\" not numeric", e );
            }
            if ( dval > 0 ) {
                return new FixedBinSizer( dval );
            }
            else if ( dval <= -1 ) {
                return new CountBinSizer( -dval, true );
            }
            else {
                String msg = "Bad sizer value " + dval
                           + " - should be >0 (fixed) or <1 (-bin_count)";
                throw new ConfigException( this, msg );
            }
        }

        public String valueToString( BinSizer sizer ) {
            if ( sizer instanceof FixedBinSizer ) {
                double width = ((FixedBinSizer) sizer).binWidth_;
                return Double.toString( width );
            }
            else if ( sizer instanceof CountBinSizer ) {
                double nbin = ((CountBinSizer) sizer).nbin_;
                return Integer.toString( - (int) nbin );
            }
            else {
                return "??";
            }
        }

        public Specifier<BinSizer> createSpecifier() {
            final SliderSpecifier sliderSpecifier =
                    new SliderSpecifier( 2, 400, true, dfltNbin_, true, true );
            return new SpecifierPanel<BinSizer>( true ) {
                protected JComponent createComponent() {
                    sliderSpecifier.addActionListener( getActionForwarder() );
                    return sliderSpecifier.getComponent();
                }
                public BinSizer getSpecifiedValue() {
                    if ( ! sliderSpecifier.isSliderActive() ) {
                        double txtval = sliderSpecifier.getTextValue();
                        if ( txtval > 0 ) {
                            return new FixedBinSizer( txtval );
                        }
                        else if ( txtval < 0 ) {
                            return new CountBinSizer( -txtval, true );
                        }
                    }
                    return new CountBinSizer( sliderSpecifier.getSliderValue(),
                                              true );
                }
                public void setSpecifiedValue( BinSizer sizer ) {
                    if ( sizer instanceof CountBinSizer ) {
                        double nbin = ((CountBinSizer) sizer).nbin_;
                        sliderSpecifier.setSliderActive( true );
                        sliderSpecifier.setSpecifiedValue( nbin );
                    }
                    else if ( sizer instanceof FixedBinSizer ) {
                        double bw = ((FixedBinSizer) sizer).binWidth_;
                        sliderSpecifier.setSliderActive( false );
                        sliderSpecifier.setSpecifiedValue( bw );
                    }
                    else {
                        logger_.warning( "Can't reset to unknown sizer type" );
                    }
                }
            };
        }
    }
}
