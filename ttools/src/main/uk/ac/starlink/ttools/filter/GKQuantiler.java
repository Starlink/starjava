package uk.ac.starlink.ttools.filter;

/**
 * Quantiler based on the method of Greenwald and Kanna,
 * via the implementation in
 * <a href="https://github.com/DataDog/sketches-java"
 *         >https://github.com/DataDog/sketches-java</a>.
 * It's not that fast and not that accurate, but it can cope with any
 * number of samples.
 *
 * @author   Mark Taylor
 * @since    3 Dec 2020
 * @see <a href="http://infolab.stanford.edu/~datar/courses/cs361a/papers/quantiles.pdf"
 *         >Greenwald and Kanna paper</a>
 */
public class GKQuantiler implements Quantiler {

    private final GKArray gkArray_;

    /** Default rank accuracy. */
    public static final double DFLT_RANK_ACCURACY = 0.0001;

    /**
     * Constructor with default accumulator.
     */
    public GKQuantiler() {
        this( new GKArray( DFLT_RANK_ACCURACY ) );
    }

    /**
     * Constructor with custom accumulator.
     */
    public GKQuantiler( GKArray gkArray ) {
        gkArray_ = gkArray;
    }

    public void acceptDatum( double value ) {
        gkArray_.accept( value );
    }

    public void addQuantiler( Quantiler other ) {
        gkArray_.mergeWith( ((GKQuantiler) other).gkArray_ );
    }

    public void ready() {
    }

    public double getValueAtQuantile( double quantile ) {
        return gkArray_.isEmpty() ? Double.NaN
                                  : gkArray_.getValueAtQuantile( quantile );
    }
}
