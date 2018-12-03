package uk.ac.starlink.ttools.plot2.layer;

import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTextField;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
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

    /**
     * Provides a bin width value for a given axis data range.
     *
     * @param  xlog  false for linear scaling, true for logarithmic
     * @param  xlo   axis lower bound
     * @param  xhi   axis upper bound
     * @param  rounding  rounding policy hint (may be ignored)
     *                   or null for no rounding
     * @return   additive/multiplicative bin width appropriate for the
     *           given range
     */
    public abstract double getWidth( boolean xlog, double xlo, double xhi,
                                     Rounding rounding );

    /**
     * Returns a bin sizer instance which always returns the same fixed
     * value.  No rounding is performed.
     *
     * @param  binWidth  fixed bin width
     * @return  bin sizer
     */
    public static BinSizer createFixedBinSizer( double binWidth ) {
        return new FixedBinSizer( binWidth );
    }

    /**
     * Returns a bin sizer instance which divides the axis range up into
     * an approximately fixed number of equal intervals.
     * Rounding is performed on request, so that bin sizes are sensible
     * values that give a bin count near the requested value.
     *
     * @param   nbin   number of intervals to divide the axis into
     * @return  bin sizer instance
     */
    public static BinSizer createCountBinSizer( double nbin ) {
        return new CountBinSizer( nbin );
    }

    /**
     * Constructs a config key for acquiring BinSizers.
     *
     * @param   meta  key metadata
     * @param   widthReportKey  report key giving bin width in data coordinates
     * @param   dfltNbin  default bin count
     * @param  allowZero  true iff zero is an allowed width
     * @return  new config key
     */
    public static ConfigKey<BinSizer>
            createSizerConfigKey( ConfigMeta meta,
                                  ReportKey<Double> widthReportKey,
                                  int dfltNbin, boolean allowZero ) {
        return new BinSizerConfigKey( meta, widthReportKey, dfltNbin,
                                      allowZero );
    }

    /**
     * Returns an XML string describing in general terms how to operate
     * the BinSizer config key.
     *
     * @return  XML &lt;p&gt; element(s)
     */
    public static String getConfigKeyDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>If the supplied value is a positive number",
            "it is interpreted as a fixed width in the data coordinates",
            "of the X axis",
            "(if the X axis is logarithmic, the value is a fixed factor).",
            "If it is a negative number, then it will be interpreted",
            "as the approximate number of smooothing widths that fit",
            "in the width of the visible plot",
            "(i.e. plot width / smoothing width).",
            "If the value is zero, no smoothing is applied.",
            "</p>",
            "<p>When setting this value graphically,",
            "you can use either the slider to adjust the bin count",
            "or the numeric entry field to fix the bin width.",
            "</p>",
        } );
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

        public double getWidth( boolean xlog, double xlo, double xhi,
                                Rounding rounding ) {
            return xlog ? Math.max( 1, binWidth_ ) : binWidth_;
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
     * up into a (roughly) fixed number of equal intervals.
     * It might not be exact according to whether rounding is requested.
     */
    private static class CountBinSizer extends BinSizer {
        private final double nbin_;

        /**
         * Constructor.
         *
         * @param  nbin  number of intervals
         */
        CountBinSizer( double nbin ) {
            nbin_ = nbin;
        }

        public double getWidth( boolean xlog, double xlo, double xhi,
                                Rounding rounding ) {
            double width0 = xlog
                ? Math.exp( ( Math.log( xhi ) - Math.log( xlo ) ) / nbin_ )
                : ( xhi - xlo ) / nbin_;
            return rounding == null
                 ? width0
                 : rounding.getRounder( xlog ).round( width0 );
        }

        @Override
        public int hashCode() {
            int code = 44301;
            code = 23 * code + Float.floatToIntBits( (float) nbin_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof CountBinSizer ) {
                CountBinSizer other = (CountBinSizer) o;
                return this.nbin_ == other.nbin_;
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

        private final ReportKey<Double> widthReportKey_;
        private final int dfltNbin_;
        private final boolean allowZero_;

        /**
         * Constructor.
         *
         * @param   meta  key metadata
         * @param   widthReportKey  report key giving bin width in data coords
         * @param   dlftNbin  default bin count
         * @param  allowZero  true iff zero is an allowed width
         */
        BinSizerConfigKey( ConfigMeta meta, ReportKey<Double> widthReportKey,
                           int dfltNbin, boolean allowZero ) {
            super( meta, BinSizer.class, new CountBinSizer( dfltNbin ) );
            widthReportKey_ = widthReportKey;
            dfltNbin_ = dfltNbin;
            allowZero_ = allowZero;
        }

        public BinSizer stringToValue( String txt ) throws ConfigException {
            double dval;
            try {
                dval = Double.valueOf( txt.trim() );
            }
            catch ( NumberFormatException e ) {
                throw new ConfigException( this,
                                           "\"" + txt + "\" not numeric", e );
            }
            if ( dval > 0 ||
                 dval == 0 && allowZero_ ) {
                return new FixedBinSizer( dval );
            }
            else if ( dval <= -1 ) {
                return new CountBinSizer( -dval );
            }
            else {
                String msg =
                    "Bad sizer value " + dval + " - should be " +
                    ( allowZero_ ? ">=" : ">" ) + "0 (fixed) " +
                    "or <=-1 (-bin_count)";
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
            return new BinSizerSpecifier( widthReportKey_, dfltNbin_,
                                          allowZero_, 1000 );
        }
    }

    /**
     * Specifier for BinSizer values.
     */
    public static class BinSizerSpecifier extends SpecifierPanel<BinSizer> {

        private final ReportKey<Double> widthReportKey_;
        private final boolean allowZero_;
        private final SliderSpecifier sliderSpecifier_;
        private final int maxCount_;

        /**
         * Constructor.
         *
         * @param   widthReportKey  report key giving bin width in data coords
         * @param   dlftNbin  default bin count
         * @param  allowZero  true iff zero is an allowed width
         * @param   maxCount   maximum  count value
         */
        BinSizerSpecifier( ReportKey<Double> widthReportKey, int dfltNbin,
                           boolean allowZero, int maxCount ) {
            super( true );
            widthReportKey_ = widthReportKey;
            allowZero_ = allowZero;
            maxCount_ = maxCount;
            double reset = dfltNbin == 0 ? maxCount : dfltNbin;
            sliderSpecifier_ =
                new SliderSpecifier( 2, maxCount, true, reset, true,
                                     SliderSpecifier.TextOption.ENTER );
        }

        protected JComponent createComponent() {
            sliderSpecifier_.addActionListener( getActionForwarder() );
            return sliderSpecifier_.getComponent();
        }

        public BinSizer getSpecifiedValue() {
            double dval = Double.NaN;
            if ( ! sliderSpecifier_.isSliderActive() ) {
                dval = sliderSpecifier_.getTextValue();
            }
            if ( Double.isNaN( dval ) ||
                 dval == 0 && ! allowZero_ ) {
                dval = - sliderSpecifier_.getSliderValue();
                if ( allowZero_ && dval == - maxCount_ ) {
                    dval = 0;
                }
            }
            return dval >= 0 ? new FixedBinSizer( dval )
                             : new CountBinSizer( - dval );
        }

        public void setSpecifiedValue( BinSizer sizer ) {
            if ( sizer instanceof CountBinSizer ) {
                double nbin = ((CountBinSizer) sizer).nbin_;
                sliderSpecifier_.setSliderActive( true );
                sliderSpecifier_.setSpecifiedValue( nbin );
            }
            else if ( sizer instanceof FixedBinSizer ) {
                double bw = ((FixedBinSizer) sizer).binWidth_;
                sliderSpecifier_.setSliderActive( false );
                sliderSpecifier_.setSpecifiedValue( bw );
            }
            else {
                logger_.warning( "Can't reset to unknown sizer type" );
            }
        }

        public void submitReport( ReportMap report ) {
            Double objval = report == null ? null
                                           : report.get( widthReportKey_ );
            double dval = objval == null ? Double.NaN : objval.doubleValue();
            displayBinWidth( dval );
        }

        /**
         * May display the current width in data coordinates in the
         * text field of this specifier's GUI.
         * The displayed value will only be affectedif the slider,
         * rather than the text field, is currently active.
         * This method can be used to reflect the actual width in data
         * coordinates that corresponds to the slider's current value,
         * if known, since the specifier itself is not able to determine that.
         *
         * @param  fixVal  the fixed positive bin width currently selected
         */
        private void displayBinWidth( double fixVal ) {
            if ( sliderSpecifier_.isSliderActive() ) {
                String txt = Double.isNaN( fixVal )
                           ? ""
                           : Float.toString( (float) fixVal );
                JTextField txtField = sliderSpecifier_.getTextField();
                txtField.setText( txt );
                txtField.setCaretPosition( 0 );
            }
        }
    }
}
