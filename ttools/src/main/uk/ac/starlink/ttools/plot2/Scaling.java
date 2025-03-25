package uk.ac.starlink.ttools.plot2;

/**
 * Defines a policy for scaling values to a fixed interval.
 * The job of a Scaling is to create a {@link Scaler},
 * usually from information that can be gained from a {@link Span} instance.
 * Scaling implementation classes should generally implement one of the
 * sub-interfaces here to indicate how this can be done.
 *
 * @author   Mark Taylor
 * @since    22 Jan 2015
 * @see      Scalings
 */
@Equality
public interface Scaling {

    /** Linear scaling. */
    public static final Scaling.RangeScaling LINEAR =
        Scalings.createLinearScaling( "Linear" );

    /** Logarithmic scaling. */
    public static final Scaling.RangeScaling LOG =
        Scalings.createLogScaling( "Log" );

    /** Asinh scaling. */
    public static final Scaling.RangeScaling ASINH =
        Scalings.createAsinhScaling( "Asinh" );

    /** Square root scaling. */
    public static final Scaling.RangeScaling SQRT =
        Scalings.createSqrtScaling( "Sqrt" );

    /** Square scaling. */
    public static final Scaling.RangeScaling SQUARE =
        Scalings.createSquareScaling( "Square" );

    /** Arccos scaling; sigmoid vertical at each end. */
    public static final Scaling.RangeScaling ACOS =
        Scalings.createAcosScaling( "Acos" );

    /** Cos scaling; sigmoid horizontal at each end. */
    public static final Scaling.RangeScaling COS =
        Scalings.createCosScaling( "Cos" );

    /** Asinh-based scaling with default parameters. */
    public static final Scaling.RangeScaling AUTO =
        Scalings.createAutoScaling( "Auto" );

    /** Histogram scaling on a linear scale. */
    public static final Scaling.HistogramScaling HISTO =
        Scalings.createHistogramScaling( "Histogram", false );

    /** Histogram scaling on a logarithmic scale. */
    public static final Scaling.HistogramScaling HISTOLOG =
        Scalings.createHistogramScaling( "HistoLog", true );

    /** List of standard options for colour map stretch. */
    public static final Scaling[] STRETCHES = new Scaling[] {
        LOG, LINEAR, HISTO, HISTOLOG, ASINH, SQRT, SQUARE, ACOS, COS,
    };

    /**
     * Returns the name of this scaling.
     *
     * @return  name
     */
    String getName();

    /**
     * Returns a short description of this scaling.
     *
     * @return  short text description
     */
    String getDescription();

    /**
     * Indicates whether this scaling is logarithmic.
     * If so, it should be displayed on logarithmic axis,
     * and can't cope with negative values.
     *
     * @return   true for basically logarithmic,
     *           false for (perhaps distorted) linear
     */
    boolean isLogLike();

    /**
     * Interface for Scaling instances that can create Scalers from
     * a lower and upper bound.
     */
    interface RangeScaling extends Scaling {

        /**
         * Returns a scaler instance that can scale input values in a given
         * range.
         * The given bounds define the range of input values that will be
         * mapped to the fixed (0..1) output range.  Input values outside
         * that range will in general result in clipping, so for the
         * returned scaler <code>s</code>:
         * <pre>
         *    s.scaleValue(x) == s.scaleValue(lo) for x&lt;lo
         *    s.scaleValue(x) == s.scaleValue(hi) for x&gt;hi
         * </pre>
         *
         * @param  lo  lower bound of unclipped input data value
         * @param  hi  upper bound of unclipped input data value
         * @return  instance
         */
        Scaler createScaler( double lo, double hi );
    }

    /**
     * Marker interface for Scaling instances that create Scalers based
     * on a histogram assembled from data.
     */
    interface HistogramScaling extends Scaling {
    }
}
