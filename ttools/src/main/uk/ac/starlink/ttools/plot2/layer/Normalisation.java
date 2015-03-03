package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Defines normalisation modes for histogram-like plots.
 *
 * @author   Mark Taylor
 * @since    19 Feb 2015
 */
@Equality
public enum Normalisation {

    /** No normalisation is performed. */
    NONE( "No normalisation is performed." ) {
        public double getScaleFactor( double sum, double max, double binWidth,
                                      boolean cumul ) {
            return 1.0;
        }
    },

    /** The total height of histogram bars is normalised to unity. */
    HEIGHT( "The total height of histogram bars is normalised to unity." ) {
        public double getScaleFactor( double sum, double max, double binWidth,
                                      boolean cumul ) {
            return 1.0 / sum;
        }
    },

    /** The total area of histogram bars is normalised to unity. */
    AREA( "The total area of histogram bars is normalised to unity. "
        + "For logarithmic X axis or cumulative plots, this behaves like "
        + "<code>" + HEIGHT.toString().toLowerCase() + "</code>." ) {
        public double getScaleFactor( double sum, double max, double binWidth,
                                      boolean cumul ) {
            double effectiveBinWidth = PlotUtil.isFinite( binWidth )
                                     ? binWidth
                                     : 1;
            return 1.0 / ( cumul ? sum : ( sum * effectiveBinWidth ) );
        }
    },

    /** Height of the tallest histogram bar is normalised to unity. */
    MAXIMUM( "The height of the tallest histogram bar is normalised to unity. "
           + "For cumulative plots, this behaves like "
           + "<code>" + HEIGHT.toString().toLowerCase() + "</code>." ) {
        public double getScaleFactor( double sum, double max, double binWidth,
                                      boolean cumul ) {
            return 1.0 / ( cumul ? sum : max );
        }
    };

    private final String description_;

    /**
     * Constructor.
     *
     * @param   description  short description
     */
    Normalisation( String description ) {
        description_ = description;
    }

    /**
     * Returns a short description of this mode.
     *
     * @return   description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns the value by which all bins should be scaled to achieve
     * normalisation for a given data set.
     *
     * <p>If it is not possible to supply a single <code>binWidth</code> value
     * (for instance in the case of a logarithmic axis), NaN may be given.
     * The <code>binWidth</code> is only used by AREA mode, and in case of
     * an indefinite bin width that mode will behave like HEIGHT.
     *
     * <p>For cumulative plots, all the modes except NONE behave the same,
     * normalising the total value to unity.
     *
     * @param  sum  total height of all histogram bars
     * @param  max  height of tallest histogram bar
     * @param  binWidth  constant linear width of histogram bars, or NaN
     * @param  isCumulative  true iff the plot is cumulative
     */
    public abstract double getScaleFactor( double sum, double max,
                                           double binWidth,
                                           boolean isCumulative );
}
