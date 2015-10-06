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

    /** The total area of histogram bars is normalised to unity. */
    AREA( "The total area of histogram bars is normalised to unity. "
        + "For cumulative plots, this behaves like <code>height</code>." ) {
        public double getScaleFactor( double sum, double max, double binWidth,
                                      boolean cumul ) {
            return 1.0 / ( cumul ? sum : ( sum * binWidth ) );
        }
    },

    /** Height of the tallest histogram bar is normalised to unity. */
    MAXIMUM( "The height of the tallest histogram bar is normalised to unity. "
           + "For cumulative plots, this behaves like <code>height</code>." ) {
        public double getScaleFactor( double sum, double max, double binWidth,
                                      boolean cumul ) {
            return 1.0 / ( cumul ? sum : max );
        }
    },

    /** The total height of histogram bars is normalised to unity. */
    HEIGHT( "The total height of histogram bars is normalised to unity." ) {
        public double getScaleFactor( double sum, double max, double binWidth,
                                      boolean cumul ) {
            return 1.0 / sum;
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
     * <p>The <code>binWidth</code> should at least make sense in terms
     * of screen area.  For linear X axis, it can be in data units,
     * but for logarithmic X axis it may have to be in log(data units).
     * The <code>binWidth</code> is only used by AREA mode.
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
