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
public abstract class Normalisation {

    private final String name_;
    private final String description_;

    /** No normalisation is performed. */
    public static final Normalisation NONE;

    /** The total area of histogram bars is normalised to unity. */
    public static final Normalisation AREA;

    /** Bars scaled by inverse bin width. */
    public static final Normalisation UNIT;

    /** Height of the tallest histogram bar is normalised to unity. */
    public static final Normalisation MAXIMUM;

    /** The total height of histogram bars is normalised to unity. */
    public static final Normalisation HEIGHT;

    /** Pre-defined instances. */
    private static final Normalisation[] KNOWN_VALUES = {
        NONE = new Normalisation( "None", "No normalisation is performed." ) {
            public double getScaleFactor( double sum, double max,
                                          double binWidth, boolean cumul ) {
                return 1.0;
            }
        },
        AREA = new Normalisation( "Area",
                                  "The total area of histogram bars "
                                + "is normalised to unity. " 
                                + "For cumulative plots, this behaves like "
                                + "<code>height</code>." ) {
            public double getScaleFactor( double sum, double max,
                                          double binWidth, boolean cumul ) {
                return 1.0 / ( cumul ? sum : ( sum * binWidth ) );
            }
        },
        UNIT = new Normalisation( "Unit",
                                  "Histogram bars are scaled by the inverse "
                                + "of the bin width in data units. "
                                + "For cumulative plots, this behaves like "
                                + "<code>none</code>." ) {
            public double getScaleFactor( double sum, double max,
                                          double binWidth, boolean cumul ) {
                return cumul ? 1.0 : 1.0 / binWidth;
            }
        },
        MAXIMUM = new Normalisation( "Maximum",
                                     "The height of the tallest histogram bar "
                                   + "is normalised to unity. "
                                   + "For cumulative plots, this behaves like "
                                   + "<code>height</code>." ) {
            public double getScaleFactor( double sum, double max,
                                          double binWidth, boolean cumul ) {
                return 1.0 / ( cumul ? sum : max );
            }
        },
        HEIGHT = new Normalisation( "Height",
                                    "The total height of histogram bars "
                                  + "is normalised to unity." ) {
            public double getScaleFactor( double sum, double max,
                                          double binWidth, boolean cumul ) {
                return 1.0 / sum;
            }
        },
    };

    /**
     * Constructor.
     *
     * @param   name   mode name
     * @param   description  short description
     */
    protected Normalisation( String name, String description ) {
        name_ = name;
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

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns the Normalisation instances defined by this class.
     *
     * @return  list of normalisation instances
     */
    public static Normalisation[] getKnownValues() {
        return KNOWN_VALUES.clone();
    }
}
