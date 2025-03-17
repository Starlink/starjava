package uk.ac.starlink.ttools.plot2;

import uk.ac.starlink.ttools.plot.Rounder;

/**
 * Represents a mapping of data values to some linear scale.
 * The function is assumed to be strictly monotonic increasing and continuous.
 *
 * <p>All finite values are permitted for both the data and the scale,
 * except as documented by the {@link #isPositiveDefinite} method.
 *
 * <p>The {@link #LINEAR} and {@link #LOG} instances are provided
 * as static members of this class; to produce other instances,
 * see the {@link ScaleType} class.
 *
 * @author   Mark Taylor
 * @since    12 Mar 2025
 */
@Equality
public interface Scale {

    public static final Scale LINEAR =
        ScaleType.LINEAR.createScale( new double[ 0 ] );
    public static final Scale LOG =
        ScaleType.LOG.createScale( new double[ 0 ] );
    public static final Scale TIME =
        ScaleType.TIME.createScale( new double[ 0 ] );

    /**
     * Returns the type of this scale.
     *
     * @return  scale type
     */
    ScaleType getScaleType();

    /**
     * Returns the array of parameter values that combined with the scale type
     * fully specify this scale.
     *
     * @return  scale parameter values, same length as
                <code>getScaleType().getParams()</code>
     */
    double[] getParamValues();

    /**
     * Forward mapping.
     *
     * @param  d  data value
     * @return  scale value
     */
    double dataToScale( double d );

    /**
     * Inverse mapping.
     *
     * @param  s  scale value
     * @return  data value
     */
    double scaleToData( double s );

    /**
     * If this method returns true, then only data values that are
     * strictly greater than zero can be mapped to the scale,
     * and a data value of zero is mapped to a scale value of
     * negative infinity.
     * Otherwise, any finite data value and any finite scale value
     * can be mapped to a finite value in the other domain.
     *
     * @return  true iff negative data values are excluded
     */
    boolean isPositiveDefinite();

    /**
     * Indicates whether the scaling relation is linear.
     *
     * @return  true for uniformly linear scaling, false otherwise
     */
    boolean isLinear();

    /**
     * Returns an object that can generate axis ticks for this scale.
     *
     * @return  ticker
     */
    Ticker getTicker();

    /**
     * Returns an object which can round scale values to a value that
     * counts as a round number.
     *
     * @return  rounder for the scale quantity
     */
    Rounder getScaleRounder();

    /**
     * Returns an algebraic expression representing the mapping of
     * a variable from data space to scale space.
     *
     * @param  var   input variable representation
     */
    String dataToScaleExpression( String var );
}
