package uk.ac.starlink.ttools.plot2;

/**
 * Defines the mapping of an input value to the range 0..1.
 *
 * @author   Mark Taylor
 * @since    22 Jan 2015
 */
@Equality
public interface Scaler {

    /**
     * Returns the lower bound of the input range.
     * Any input values less than or equal to this value
     * will be mapped to zero.
     *
     * @return  lower input bound
     */
    double getLow();

    /**
     * Returns the upper bound of the input range.
     * Any input values greater than or equal to this value
     * will be mapped to 1.
     *
     * @return  upper input bound
     */
    double getHigh();

    /**
     * Indicates whether this scaler does log-like scaling.
     * This is used to determine whether an axis on which it is represented
     * should have logarithmic or linear annotation.
     *
     * @return  true for log-like, false for linear
     */
    boolean isLogLike();

    /**
     * Scales an input value to the interval 0..1.
     * NaN values stay NaN.
     *
     * @param  val  input data value
     * @return  value in range 0..1 (inclusive), or NaN for NaN input
     */
    double scaleValue( double val );
}
